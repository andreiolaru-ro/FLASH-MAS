/*******************************************************************************
 * Copyright (C) 2021 Andrei Olaru.
 *
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 *
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.daemon;

import net.xqhs.util.logging.Unit;

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
 */
public class FlashMasDaemon extends Unit {

	/**
	 * Internal Enum for Protocol Commands.
	 */
	public enum DaemonCommand {
		UPLOAD_JAR,
		START_NODE,
		CHECK_STATUS,
		KILL_NODE,
		KILL_DAEMON,
		UNKNOWN;

		public static DaemonCommand fromString(String commandStr) {
			try {
				return valueOf(commandStr);
			} catch (IllegalArgumentException | NullPointerException e) {
				return UNKNOWN;
			}
		}
	}

	private static final String NODE_JAR_NAME = "flash-node.jar";
	private static final String JRE_FOLDER_NAME = "jre";
	private Process activeNodeProcess;

	private final int port;
	private final boolean redirectOutput;

	public static final int DEFAULT_PORT = 35274;

	public FlashMasDaemon(int port, boolean redirectOutput) {
		this.port = port;
		this.redirectOutput = redirectOutput;
	}

	/**
	 * Starts the main listening loop.
	 */
	public void start() {
		li("==================================================");
		li("   Flash-MAS Daemon (Listener Mode)");
		li("   Listening on PORT: {}", port);
		li("   Output Redirection: {}", (redirectOutput ? "ENABLED (to files)" : "DISABLED (to console)"));
		li("   Local JRE path check: ./" + JRE_FOLDER_NAME);
		li("==================================================");

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			while (true) {
				try {
					li("[INFO] Waiting for Controller connection...");
					Socket clientSocket = serverSocket.accept();
					li("[INFO] Accepted connection from: {}", clientSocket.getInetAddress());

					new Thread(() -> handleControllerConnection(clientSocket)).start();

				} catch (IOException e) {
					le("[ERROR] Accept failed", e);
				}
			}
		} catch (IOException e) {
			le("[CRITICAL] Could not start server on port {}", port, e);
		}
	}

	/**
	 * Handles the specific conversation with the Controller.
	 */
	private void handleControllerConnection(Socket socket) {
		try (DataInputStream in = new DataInputStream(socket.getInputStream());
			 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

			String commandStr = in.readUTF();
			DaemonCommand command = DaemonCommand.fromString(commandStr);

			li("[CMD] Received command: {}", command);

			switch (command) {
				case UPLOAD_JAR:
					receiveJarFile(in);
					out.writeUTF("OK: JAR file updated successfully.");
					break;
				case START_NODE:
					String arguments = in.readUTF();
					startNodeProcess(arguments, out);
					break;
				case CHECK_STATUS:
					out.writeUTF("OK: Daemon is running.");
					break;
				case KILL_NODE:
					if (activeNodeProcess != null && activeNodeProcess.isAlive()) {
						activeNodeProcess.destroyForcibly();
						activeNodeProcess = null;
						li("[INFO] Node killed by controller.");
						out.writeUTF("OK: Node killed.");
					} else {
						out.writeUTF("ERROR: No active node.");
					}
					break;
				case KILL_DAEMON:
					out.writeUTF("OK: Daemon shutting down.");
					out.flush();
					li("[WARN] Exiting...");
					if (activeNodeProcess != null && activeNodeProcess.isAlive()) {
						activeNodeProcess.destroyForcibly();
					}
					new Thread(() -> {
						try { Thread.sleep(500); } catch (InterruptedException ignored) {}
						System.exit(0);
					}).start();
					break;
				case UNKNOWN:
				default:
					lw("[WARN] Unknown command string received: {}", commandStr);
					out.writeUTF("ERROR: Unknown command.");
					break;
			}

		} catch (IOException e) {
			le("[ERROR] Connection error", e);
		}
	}

	/**
	 * Downloads the JAR file sent by the Controller.
	 */
	private void receiveJarFile(DataInputStream in) throws IOException {
		long fileSize = in.readLong();
		li("[UPLOAD] Receiving JAR ({} bytes)...", fileSize);

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
		li("[UPLOAD] File saved as '{}'.", NODE_JAR_NAME);
	}

	/**
	 * Starts the node process using ProcessBuilder and the bundled JRE.
	 */
	private void startNodeProcess(String argsString, DataOutputStream out) throws IOException {
		String javaBin = getJavaExecutablePath();
		if (javaBin == null) {
			out.writeUTF("ERROR: Java not found (local or system).");
			return;
		}

		List<String> command = new ArrayList<>();
		command.add(javaBin);
		command.add("-jar");
		command.add(NODE_JAR_NAME);

		String[] args = argsString.split(" ");
		for (String arg : args) {
			if (!arg.trim().isEmpty()) command.add(arg);
		}

		li("[EXEC] Executing: {}", command);

		ProcessBuilder pb = new ProcessBuilder(command);

		if (redirectOutput) {
			pb.redirectOutput(new File("node-stdout.log"));
			pb.redirectError(new File("node-stderr.log"));
		} else {
			pb.inheritIO();
		}

		try {
			activeNodeProcess = pb.start();
			long pid = activeNodeProcess.pid();
			li("[EXEC] Started successfully. PID: {}", pid);

			String logMsg = redirectOutput ? " Output redirected to logs." : " Output visible in console.";
			out.writeUTF("OK: Node started. PID: " + pid + logMsg);

		} catch (IOException e) {
			le("[EXEC] Failed to start", e);
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

		Path localJrePath = Paths.get(".", JRE_FOLDER_NAME, "bin", javaExecName);
		if (Files.exists(localJrePath)) {
			li("[JAVA] Found Bundled JRE: {}", localJrePath.toAbsolutePath());
			return localJrePath.toAbsolutePath().toString();
		}

		li("[JAVA] Bundled JRE not found. Using system 'java'.");
		return "java";
	}
}