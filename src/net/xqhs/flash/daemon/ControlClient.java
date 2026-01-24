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
	 * Full deployment sequence: Upload JAR -> Start Node.
	 */
	public static void deployAndStart(String host, int port, String localJarPath, String nodeArguments) {
		instance.li(">>> Starting Deployment to {}:{}", host, port);

		boolean uploadSuccess = sendCommand(host, port, FlashMasDaemon.DaemonCommand.UPLOAD_JAR, localJarPath, null);

		if (uploadSuccess) {
			sendCommand(host, port, FlashMasDaemon.DaemonCommand.START_NODE, null, nodeArguments);
		} else {
			instance.le(">>> Deployment aborted due to upload failure.");
		}
	}

	/**
	 * Helper method to send a specific command.
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
						instance.li("[CLIENT] Arguments sent.");
					}
					break;

				default:
					break;
			}

			String response = in.readUTF();
			instance.li("[DAEMON RESPONSE] {}", response);

			return response.startsWith("OK");

		} catch (IOException e) {
			instance.le("[CLIENT] Communication error with {}", host, e);
			return false;
		}
	}
}