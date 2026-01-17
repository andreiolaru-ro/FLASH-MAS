/*******************************************************************************
 * Copyright (C) 2021 Andrei Olaru.
 * * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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

	// The name of the JAR file to be executed/updated
	private static final String NODE_JAR_NAME = "flash-node.jar";

	// The folder name where the bundled JRE is expected
	private static final String JRE_FOLDER_NAME = "jre";

	private final int port;
	private final boolean redirectOutput;

	/**
	 * Constructor for the Daemon.
	 * @param port the port to listen on.
	 * @param redirectOutput true to write child process output to .log files, false to inherit IO (console).
	 */
	public FlashMasDaemon(int port, boolean redirectOutput) {
		this.port = port;
		this.redirectOutput = redirectOutput;
	}

	/**
	 * Starts the main listening loop.
	 */
	public void start() {
		System.out.println("==================================================");
		System.out.println("   Flash-MAS Daemon (Listener Mode)");
		System.out.println("   Listening on PORT: " + port);
		System.out.println("   Output Redirection: " + (redirectOutput ? "ENABLED (to files)" : "DISABLED (to console)"));
		System.out.println("   Local JRE path check: ./" + JRE_FOLDER_NAME);
		System.out.println("==================================================");

		try (ServerSocket serverSocket = new ServerSocket(port)) {
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
			System.err.println("[CRITICAL] Could not start server on port " + port);
			e.printStackTrace();
		}
	}

	/**
	 * Handles the specific conversation with the Controller.
	 */
	private void handleControllerConnection(Socket socket) {
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
	private void receiveJarFile(DataInputStream in) throws IOException {
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
	private void startNodeProcess(String argsString, DataOutputStream out) throws IOException {
		// 1. Detect Java Executable
		String javaBin = getJavaExecutablePath();
		if (javaBin == null) {
			out.writeUTF("ERROR: Java not found (local or system).");
			return;
		}

		// 2. Build the command list
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

		if (redirectOutput) {
			// Redirect output to log files (non-blocking for the daemon)
			pb.redirectOutput(new File("node-stdout.log"));
			pb.redirectError(new File("node-stderr.log"));
		} else {
			// Pipe output to the Daemon's console
			pb.inheritIO();
		}

		try {
			Process process = pb.start();
			long pid = process.pid();
			System.out.println("[EXEC] Started successfully. PID: " + pid);

			String logMsg = redirectOutput ? " Output redirected to logs." : " Output visible in console.";
			out.writeUTF("OK: Node started. PID: " + pid + logMsg);

		} catch (IOException e) {
			System.err.println("[EXEC] Failed to start: " + e.getMessage());
			out.writeUTF("ERROR: Start failed: " + e.getMessage());
		}
	}

	/**
	 * Logic to find the Java executable.
	 */
	private String getJavaExecutablePath() {
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