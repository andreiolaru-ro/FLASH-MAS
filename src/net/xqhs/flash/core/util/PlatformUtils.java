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
package net.xqhs.flash.core.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Vector;

import net.xqhs.flash.android.AndroidClassFactory;
import net.xqhs.flash.pc.PCClassFactory;
import net.xqhs.util.logging.LogWrapper.LoggerType;

/**
 * Platform-related functionality. All functions should be static.
 * <p>
 * This class contains functions that may perform differently depending on the current platofrm / OS (e.g. Android, PC,
 * etc).
 * <p>
 * Known platforms are enumerated in {@link Platform}.
 * 
 * @author Andrei Olaru
 * 
 */
public class PlatformUtils
{
	/**
	 * This enumeration contains all supported platforms.
	 * 
	 * @author Andrei Olaru
	 */
	public enum Platform {
	/**
	 * The current machine runs an OS that contains a standard Java VM.
	 */
	PC,
	
	/**
	 * The current machine runs an OS that uses the Dalvik VM.
	 */
	ANDROID,
	}
	
	/**
	 * @return the current platform, as an instance of {@link Platform}.
	 */
	public static Platform getPlatform()
	{
		if(System.getProperty("java.vm.name").equals("Dalvik"))
		{
			
			return Platform.ANDROID;
		}
		
		return Platform.PC;
	}
	
	/**
	 * @return the type of log (on of {@link LoggerType}) appropriate for the current platform.
	 */
	public static LoggerType platformLogType()
	{
		// return LoggerType.GLOBAL;
		return LoggerType.MODERN;
	}
	
	/**
	 * @return the URI of the local machine.
	 */
	public static String getLocalHostURI() {
		// return "localhost";
		if (getPlatform() == Platform.ANDROID) {
			return LoggerType.JAVA;
		}
		return LoggerType.CONSOLE;
	}
	
	/**
	 * @return a {@link ClassFactory} instance to create new instances.
	 */
	public static ClassFactory getClassFactory()
	{
		switch(getPlatform())
		{
		case PC:
			return new PCClassFactory();
		case ANDROID:
			return new AndroidClassFactory();
		default:
			break;
		}
		return null;
	}
	
	/**
	 * @return the class for the Simulation Manager GUI.
	 */
	public static String getSimulationGuiClass()
	{
		String platformName = getPlatform().toString();
		switch(getPlatform())
		{
		case PC:
			return "tatami." + platformName.toLowerCase() + ".agent.visualization." + platformName.toUpperCase()
					+ "SimulationGui";
		default:
			break;
		}
		return null;
	}
	
	/**
	 * Converts the arguments into a {@link Vector} containing all arguments passed to the method.
	 * 
	 * @param arguments
	 *            the arguments to assemble into the vector.
	 * @return the vector containing all arguments.
	 */
	public static Vector<Object> toVector(Object... arguments)
	{
		return new Vector<>(Arrays.asList(arguments));
	}
	
	/**
	 * Method to print an exception as a string.
	 * 
	 * @param e
	 *            - the exception.
	 * @return a {@link String} containing all details of the exception.
	 */
	public static String printException(Exception e)
	{
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
	
	/**
	 * Performs system exit, depending on platform.
	 * 
	 * @param exitCode
	 *            - the exit code.
	 */
	public static void systemExit(int exitCode)
	{
		System.exit(0);
	}
	
}
