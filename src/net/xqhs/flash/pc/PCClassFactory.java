/*******************************************************************************
 * Copyright (C) 2018 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.pc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import net.xqhs.flash.core.util.ClassFactory;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * Class instantiation for the PC platform.
 * 
 * @author andreiolaru
 */
public class PCClassFactory implements ClassFactory
{
	@Override
	public boolean canLoadClass(String className)
	{
		if(className == null)
			return false;
		try
		{
			Class.forName(className);
			return true;
		} catch(ClassNotFoundException e)
		{
			return false;
		}
	}
	
	@Override
	public Object loadClassInstance(String className, MultiTreeMap creationData, boolean splitArguments)
			throws ClassNotFoundException, InstantiationException, NoSuchMethodException, IllegalAccessException,
			InvocationTargetException
	{
		ClassLoader cl = null;
		cl = new ClassLoader(PCClassFactory.class.getClassLoader()) {
			// nothing to extend
		};
		Object ret;
		if(splitArguments)
		{
			List<String> constructorArguments = new LinkedList<>();
			if(creationData != null)
				for(String key : creationData.getSimpleNames())
					constructorArguments.add(creationData.getValue(key));
			Class<?>[] argumentTypes = new Class<?>[constructorArguments.size()];
			int i = 0;
			for(Object obj : constructorArguments)
				argumentTypes[i++] = obj.getClass();
			Constructor<?> constructor = cl.loadClass(className).getConstructor(argumentTypes);
			ret = constructor.newInstance(constructorArguments.toArray());
		}
		else
		{
			ret = cl.loadClass(className).getConstructor(new Class[] { MultiTreeMap.class })
					.newInstance(creationData);
		}
		return ret;
	}
	
}
