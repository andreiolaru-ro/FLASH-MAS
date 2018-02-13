package net.xqhs.flash.core.util;

/**
 * Interface for platform-specific class-loading classes.
 * 
 * @author andreiolaru
 */
public interface ClassFactory
{
	/**
	 * @param classPath
	 *            - classpath for the class to load.
	 * @param creationData
	 *            - data to use for creating the new instance. This may be <code>null</code>.
	 * @param splitArguments
	 *            - if <code>true</code>, all first values of simple keys from <code>creationData</code> are passed
	 *            individually to the constructor. If <code>splitArguments</code> is <code>true</code> and
	 *            <code>creationData</code> is <code>null</code>, the constructor will be called with no arguments.
	 * @return a new instance for the class.
	 * @throws Exception
	 *             - when something goes wrong with the class loading.
	 */
	public Object loadClassInstance(String classPath, TreeParameterSet creationData, boolean splitArguments)
			throws Exception;
	
	/**
	 * @param classPath
	 *            - classpath for the class to load.
	 * @return <code>true</code> if the class can be loaded using
	 *         {@link #loadClassInstance(String, TreeParameterSet, boolean)}.
	 */
	public boolean canLoadClass(String classPath);
}
