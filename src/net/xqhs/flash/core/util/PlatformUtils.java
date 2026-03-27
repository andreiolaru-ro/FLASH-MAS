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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import net.xqhs.flash.pc.PCClassFactory;
import net.xqhs.util.logging.LogWrapper.LoggerType;
import net.xqhs.util.logging.Logger;

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
public class PlatformUtils {
	/**
	 * This enumeration contains all supported platforms.
	 * 
	 * @author Andrei Olaru
	 */
	public static enum Platform {
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
	 * The argument for {@link LinkedBlockingQueue#wait(long)}, as wait without arguments does no wake, even if
	 * notified, if the state f the queue does not change.
	 */
	public static final long	GLOBAL_WAITING_TIME			= 1000;
	/**
	 * The default time to wait between tries in {@link #tryFor}.
	 */
	public static final long	DEFAULT_SPACE_BETWEEN_TRIES	= 1000;
	/**
	 * The default number of tries in {@link #tryFor}.
	 */
	public static final int		DEFAULT_NUMBER_OF_TRIES		= 10;
	
	/**
	 * @return the current platform, as an instance of {@link Platform}.
	 */
	public static Platform getPlatform() {
		if(System.getProperty("java.vm.name").equals("Dalvik")) {
			
			return Platform.ANDROID;
		}
		
		return Platform.PC;
	}
	
	/**
	 * @return the type of log (on of {@link LoggerType}) appropriate for the current platform.
	 */
	public static LoggerType platformLogType() {
		// return LoggerType.GLOBAL;
		return LoggerType.MODERN;
	}
	
	/**
	 * @return the URI of the local machine.
	 */
	public static String getLocalHostURI() {
		return "localhost";
	}
	
	/**
	 * @return a {@link ClassFactory} instance to create new instances.
	 */
	public static ClassFactory getClassFactory() {
		switch(getPlatform()) {
		case PC:
			return new PCClassFactory();
		default:
			break;
		}
		return null;
	}
	
	/**
	 * Creates a new instance of a class, sending no arguments to the constructor.
	 * 
	 * @param <T>
	 *            the type of the object that should be returned.
	 * @param className
	 *            - the classpath to instantiate.
	 * @return the newly instantiated object.
	 */
	public static <T extends Object> T quickInstance(String className) {
		try {
			@SuppressWarnings("unchecked")
			T obj = (T) getClassFactory().loadClassInstance(className, null, true);
			return obj;
		} catch(ClassNotFoundException | InstantiationException | NoSuchMethodException | IllegalAccessException
				| InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * @return the class for the Simulation Manager GUI.
	 */
	public static String getSimulationGuiClass() {
		String platformName = getPlatform().toString();
		switch(getPlatform()) {
		case PC:
			return "tatami." + platformName.toLowerCase() + ".agent.visualization." + platformName.toUpperCase()
					+ "SimulationGui";
		default:
			break;
		}
		return null;
	}
	
	/**
	 * Creates a {@link String} which results from the serialization of the {@link Object} instance.
	 * 
	 * @param obj
	 *            - the object.
	 * @return the serialized version.
	 */
	public static String serializeObject(Object obj) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(obj);
			oos.close();
		} catch(IOException e) {
			throw new RuntimeException("Serialization failed", e);
		}
		return Base64.getEncoder().encodeToString(baos.toByteArray());
	}
	
	/**
	 * Deserializes an object.
	 * 
	 * @param serial
	 *            - the serialization of the object.
	 * @return the object.
	 */
	public static Object deserializeObject(String serial) {
		byte[] data = Base64.getDecoder().decode(serial);
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
			Object o = ois.readObject();
			ois.close();
			return o;
		} catch(IOException e) {
			throw new RuntimeException("Serialization failed", e);
		} catch(ClassNotFoundException e) {
			throw new RuntimeException("Class not found", e);
		}
	}
	
	/**
	 * Converts the arguments into a {@link Vector} containing all arguments passed to the method.
	 * 
	 * @param arguments
	 *            the arguments to assemble into the vector.
	 * @return the vector containing all arguments.
	 */
	public static Vector<Object> toVector(Object... arguments) {
		return new Vector<>(Arrays.asList(arguments));
	}
	
	/**
	 * Method to print an exception as a string.
	 * 
	 * @param e
	 *            - the exception.
	 * @return a {@link String} containing all details of the exception.
	 */
	public static String printException(Exception e) {
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
	public static void systemExit(int exitCode) {
		System.exit(0);
	}
	
	/**
	 * Alias for {@link Supplier}.
	 * 
	 * @param <T>
	 *            - the type of the return value.
	 */
	@FunctionalInterface
	public interface CheckedSupplier<T> {
		/**
		 * Alias for {@link Supplier#get()}
		 * 
		 * @return the value.
		 * @throws Exception
		 */
		T get() throws Exception;
	}
	
	/**
	 * Calls {@link #tryFor(Logger, String, CheckedSupplier, int, long)} with {@link #DEFAULT_NUMBER_OF_TRIES} and
	 * {@link #DEFAULT_SPACE_BETWEEN_TRIES}.
	 * 
	 * @param log
	 *            -- the log to use for error reporting. If <code>null</code>, no logging messages are issued.
	 * @param message
	 *            -- a description of the attempted operation, to be used in logging messages.
	 * @param f
	 *            -- the function to try, without arguments and returning a {@link Boolean} that indicates success.
	 * @return <code>true</code> if the function return successfully in one of the number of tries.
	 */
	public static boolean tryFor(Logger log, String message, CheckedSupplier<Boolean> f) {
		return tryFor(log, message, f, DEFAULT_NUMBER_OF_TRIES, DEFAULT_SPACE_BETWEEN_TRIES);
	}
	
	/**
	 * Try executing a function for a number of times.
	 * 
	 * @param log
	 *            -- the log to use for error reporting. If <code>null</code>, no logging messages are issued.
	 * @param message
	 *            -- a description of the attempted operation, to be used in logging messages.
	 * @param f
	 *            -- the function to try, without arguments and returning a {@link Boolean} that indicates success.
	 * @param times
	 *            -- the number of times to try before failing.
	 * @param spacing
	 *            -- the time (in milliseconds) to wait between two tries.
	 * @return <code>true</code> if the function return successfully in one of the number of tries.
	 */
	public static boolean tryFor(Logger log, String message, CheckedSupplier<Boolean> f, int times, long spacing) {
		int timesLeft = times;
		try {
			while(timesLeft > 0) {
				boolean res;
				if(log != null)
					log.lf("Trying []; tries left: []", message, Integer.valueOf(timesLeft));
				try {
					res = f.get().booleanValue();
				} catch(Exception e) {
					return log != null
							? log.ler(false, "Exeption in try []: ", Integer.valueOf(-timesLeft), printException(e))
							: false;
				}
				if(res) {
					if(log != null)
						log.lf("Succeded to [] in try []", message, Integer.valueOf(-timesLeft));
					return true;
				}
				Thread.sleep(spacing);
				timesLeft--;
			}
		} catch(InterruptedException e) {
			return log != null ? log.ler(false, "Interrupted exeption: ", printException(e)) : false;
		}
		return false;
	}
	
}
