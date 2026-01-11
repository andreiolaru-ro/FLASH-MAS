package net.xqhs.flash.daemon;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Flash-MAS Daemon (Server Mode).
 * Runs on remote machines. Opens a server socket and waits for the Controller
 * to connect and send commands (Upload JAR / Start Node).
 */
public class FlashMasDaemon {

	// Default listening port
	private static int PORT = 35274;

	// The name of the JAR file to be executed/updated
	private static final String NODE_JAR_NAME = "flash-node.jar";

	// The folder name where the bundled JRE is expected (e.g., ./jre/bin/java)
	private static final String JRE_FOLDER_NAME = "jre";

	public static void main(String[] args) {
		// Allow port override via command line arguments
		if (args.length > 0) {
			try {
				PORT = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.err.println("Invalid port argument. Using default: " + PORT);
			}
		}

		System.out.println("==================================================");
		System.out.println("   Flash-MAS Daemon (Listener Mode)");
		System.out.println("   Listening on PORT: " + PORT);
		System.out.println("   Local JRE path check: ./" + JRE_FOLDER_NAME);
		System.out.println("==================================================");

		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			while (true) {
				try {
					System.out.println("[INFO] Waiting for Controller connection...");
					Socket clientSocket = serverSocket.accept();
					System.out.println("[INFO] Accepted connection from: " + clientSocket.getInetAddress());

					// Handle the connection in a separate thread
					new Thread(() -> handleControllerConnection(clientSocket)).start();

				} catch (IOException e) {
					System.err.println("[ERROR] Accept failed: " + e.getMessage());
				}
			}
		} catch (IOException e) {
			System.err.println("[CRITICAL] Could not start server on port " + PORT);
			e.printStackTrace();
		}
	}

	/**
	 * Handles the specific conversation with the Controller.
	 */
	private static void handleControllerConnection(Socket socket) {
		try (DataInputStream in = new DataInputStream(socket.getInputStream());
			 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

			// Protocol: 1. Read Command
			String command = in.readUTF();
			System.out.println("[CMD] Received command: " + command);

			switch (command) {
				case "UPLOAD_JAR":
					receiveJarFile(in);
					out.writeUTF("OK: JAR file updated successfully.");
					break;

				case "START_NODE":
					String arguments = in.readUTF(); // Read arguments for the node
					startNodeProcess(arguments, out);
					break;

				case "CHECK_STATUS":
					out.writeUTF("OK: Daemon is running.");
					break;

				default:
					System.err.println("[WARN] Unknown command received.");
					out.writeUTF("ERROR: Unknown command.");
			}

		} catch (IOException e) {
			System.err.println("[ERROR] Connection error: " + e.getMessage());
		}
	}

	/**
	 * Downloads the JAR file sent by the Controller.
	 */
	private static void receiveJarFile(DataInputStream in) throws IOException {
		long fileSize = in.readLong();
		System.out.println("[UPLOAD] Receiving JAR (" + fileSize + " bytes)...");

		byte[] buffer = new byte[4096];
		try (FileOutputStream fos = new FileOutputStream(NODE_JAR_NAME)) {
			int bytesRead;
			long totalRead = 0;
			while (totalRead < fileSize &&
					(bytesRead = in.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalRead))) != -1) {
				fos.write(buffer, 0, bytesRead);
				totalRead += bytesRead;
			}
		}
		System.out.println("[UPLOAD] File saved as '" + NODE_JAR_NAME + "'.");
	}

	/**
	 * Starts the node process using ProcessBuilder and the bundled JRE.
	 */
	private static void startNodeProcess(String argsString, DataOutputStream out) throws IOException {
		// 1. Detect Java Executable
		String javaBin = getJavaExecutablePath();
		if (javaBin == null) {
			out.writeUTF("ERROR: Java not found (local or system).");
			return;
		}

		// 2. Build the command list
		// Command: <java_path> -jar flash-node.jar <arguments>
		List<String> command = new ArrayList<>();
		command.add(javaBin);
		command.add("-jar");
		command.add(NODE_JAR_NAME);

		// Add user arguments
		String[] args = argsString.split(" ");
		for (String arg : args) {
			if (!arg.trim().isEmpty()) command.add(arg);
		}

		System.out.println("[EXEC] Executing: " + command);

		// 3. Start the process
		ProcessBuilder pb = new ProcessBuilder(command);

		// Redirect output to log files (non-blocking for the daemon)
		pb.redirectOutput(new File("node-stdout.log"));
		pb.redirectError(new File("node-stderr.log"));

		try {
			Process process = pb.start();
			long pid = process.pid();
			System.out.println("[EXEC] Started successfully. PID: " + pid);
			out.writeUTF("OK: Node started. PID: " + pid);
		} catch (IOException e) {
			System.err.println("[EXEC] Failed to start: " + e.getMessage());
			out.writeUTF("ERROR: Start failed: " + e.getMessage());
		}
	}

	/**
	 * Logic to find the Java executable.
	 * Priority: 1. Bundled 'jre' folder. 2. System PATH.
	 */
	private static String getJavaExecutablePath() {
		String os = System.getProperty("os.name").toLowerCase();
		boolean isWindows = os.contains("win");
		String javaExecName = isWindows ? "java.exe" : "java";

		// Check local bundled JRE
		Path localJrePath = Paths.get(".", JRE_FOLDER_NAME, "bin", javaExecName);
		if (Files.exists(localJrePath)) {
			System.out.println("[JAVA] Found Bundled JRE: " + localJrePath.toAbsolutePath());
			return localJrePath.toAbsolutePath().toString();
		}

		// Fallback to system java
		System.out.println("[JAVA] Bundled JRE not found. Using system 'java'.");
		return "java";
	}
}