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
import java.net.Socket;

/**
 * Controller Client.
 * Used by the Monitoring & Control Entity to send commands TO remote Daemons.
 */
public class ControlClient extends Unit {

	/**
	 * Singleton Instance for static access.
	 */
	private static final ControlClient instance = new ControlClient();

	/**
	 * Status of a remote access.
	 */
	public enum RemoteStatus {
		ONLINE,
		UNREACHABLE,
		ERROR
	}

	/**
	 * Checks the status of a remote daemon (Ping).
	 *
	 * @param host the hostname or IP address of the remote daemon.
	 * @param port the port number of the remote daemon.
	 * @return the {@link RemoteStatus} indicating the state of the connection.
	 */
	public static RemoteStatus checkRemoteStatus(String host, int port) {
		try (Socket socket = new Socket(host, port);
			 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			 DataInputStream in = new DataInputStream(socket.getInputStream())) {

			out.writeUTF(FlashMasDaemon.DaemonCommand.CHECK_STATUS.name());
			String response = in.readUTF();

			if (response != null && response.startsWith("OK")) {
				return RemoteStatus.ONLINE;
			} else {
				return RemoteStatus.ERROR;
			}
		} catch (IOException e) {
			return RemoteStatus.UNREACHABLE;
		}
	}

	/**
	 * Uploads the JAR file to the remote daemon.
	 *
	 * @param host the hostname or IP address of the remote daemon.
	 * @param port the port number of the remote daemon.
	 * @param localJarPath the file path to the local JAR file to upload.
	 * @return true if the upload was successful, false otherwise.
	 */
	public static boolean uploadJar(String host, int port, String localJarPath) {
		instance.li(">>> Uploading JAR to {}:{} from {}", host, port, localJarPath);
		return sendCommand(host, port, FlashMasDaemon.DaemonCommand.UPLOAD_JAR, localJarPath, null);
	}

	/**
	 * Starts the node process on the remote daemon.
	 *
	 * @param host the hostname or IP address of the remote daemon.
	 * @param port the port number of the remote daemon.
	 * @param nodeArguments the command-line arguments to pass to the new node process.
	 * @return true if the start command was acknowledged by the daemon, false otherwise.
	 */
	public static boolean startNode(String host, int port, String nodeArguments) {
		instance.li(">>> Starting Node on {}:{} with args: {}", host, port, nodeArguments);
		return sendCommand(host, port, FlashMasDaemon.DaemonCommand.START_NODE, null, nodeArguments);
	}

	/**
	 * Sends a command to kill the node process (JVM) on the remote machine.
	 *
	 * @param host the hostname or IP address of the remote daemon.
	 * @param port the port number of the remote daemon.
	 * @return true if the command was successfully sent and acknowledged, false otherwise.
	 */
	public static boolean killRemoteNode(String host, int port) {
		return sendCommand(host, port, FlashMasDaemon.DaemonCommand.KILL_NODE, null, null);
	}

	/**
	 * Sends a command to kill the Daemon process itself on the remote machine.
	 * <p>Note: This will result in the loss of connection to the remote machine.</p>
	 *
	 * @param host the hostname or IP address of the remote daemon.
	 * @param port the port number of the remote daemon.
	 * @return true if the command was successfully sent and acknowledged, false otherwise.
	 */
	public static boolean killRemoteDaemon(String host, int port) {
		return sendCommand(host, port, FlashMasDaemon.DaemonCommand.KILL_DAEMON, null, null);
	}

	/**
	 * Helper method to handle socket communication for sending commands.
	 *
	 * @param host the hostname or IP address of the remote daemon.
	 * @param port the port number of the remote daemon.
	 * @param command the {@link FlashMasDaemon.DaemonCommand} to send.
	 * @param filePath optional file path if the command involves file transfer.
	 * @param textPayload optional text payload if the command requires arguments.
	 * @return true if the daemon responds with "OK", false otherwise.
	 */
	private static boolean sendCommand(String host, int port, FlashMasDaemon.DaemonCommand command, String filePath, String textPayload) {
		try (Socket socket = new Socket(host, port);
			 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			 DataInputStream in = new DataInputStream(socket.getInputStream())) {

			instance.li("[CLIENT] Sending command: {}", command);
			out.writeUTF(command.name());

			switch (command) {
				case UPLOAD_JAR:
					if (filePath != null) {
						File file = new File(filePath);
						if (!file.exists()) {
							instance.le("[CLIENT] Error: File not found: {}", filePath);
							return false;
						}
						out.writeLong(file.length());
						try (FileInputStream fis = new FileInputStream(file)) {
							byte[] buffer = new byte[4096];
							int read;
							while ((read = fis.read(buffer)) != -1) {
								out.write(buffer, 0, read);
							}
						}
						instance.li("[CLIENT] File upload complete.");
					}
					break;

				case START_NODE:
					if (textPayload != null) {
						out.writeUTF(textPayload);
					}
					break;

				default:
					break;
			}

			String response = in.readUTF();
			instance.li("[DAEMON RESPONSE] {}", response);
			return response.startsWith("OK");

		} catch (IOException e) {
			instance.le("[CLIENT] Communication error with {}:{} - {}", host, port, e.getMessage());
			return false;
		}
	}
}