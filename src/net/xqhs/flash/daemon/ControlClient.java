package net.xqhs.flash.daemon;

import java.io.*;
import java.net.Socket;

/**
 * Controller Client.
 * Used by the Monitoring & Control Entity to send commands TO remote Daemons.
 */
public class ControlClient {

	/**
	 * Full deployment sequence: Upload JAR -> Start Node.
	 * * @param host          IP of the Daemon
	 * @param port          Port of the Daemon (usually 35274)
	 * @param localJarPath  Path to the JAR file on THIS computer
	 * @param nodeArguments Arguments for the agent node (e.g., "-node nodeB ...")
	 */
	public static void deployAndStart(String host, int port, String localJarPath, String nodeArguments) {
		System.out.println(">>> Starting Deployment to " + host + ":" + port);

		boolean uploadSuccess = sendCommand(host, port, "UPLOAD_JAR", localJarPath, null);

		if (uploadSuccess) {
			sendCommand(host, port, "START_NODE", null, nodeArguments);
		} else {
			System.err.println(">>> Deployment aborted due to upload failure.");
		}
	}

	/**
	 * Helper method to send a specific command.
	 */
	private static boolean sendCommand(String host, int port, String command, String filePath, String textPayload) {
		try (Socket socket = new Socket(host, port);
			 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			 DataInputStream in = new DataInputStream(socket.getInputStream())) {

			// Send Command Header
			System.out.println("[CLIENT] Sending command: " + command);
			out.writeUTF(command);

			// Handle data based on command type
			if ("UPLOAD_JAR".equals(command) && filePath != null) {
				File file = new File(filePath);
				if (!file.exists()) {
					System.err.println("[CLIENT] Error: File not found: " + filePath);
					return false;
				}

				// Send file size
				out.writeLong(file.length());

				// Send file content
				try (FileInputStream fis = new FileInputStream(file)) {
					byte[] buffer = new byte[4096];
					int read;
					while ((read = fis.read(buffer)) != -1) {
						out.write(buffer, 0, read);
					}
				}
				System.out.println("[CLIENT] File upload complete.");

			} else if ("START_NODE".equals(command) && textPayload != null) {
				// Send arguments
				out.writeUTF(textPayload);
				System.out.println("[CLIENT] Arguments sent.");
			}

			// Wait for Server Response
			String response = in.readUTF();
			System.out.println("[DAEMON RESPONSE] " + response);

			return response.startsWith("OK");

		} catch (IOException e) {
			System.err.println("[CLIENT] Communication error with " + host + ": " + e.getMessage());
			return false;
		}
	}
}