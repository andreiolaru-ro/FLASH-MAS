package net.xqhs.flash.android;

import net.xqhs.flash.core.util.ClassFactory;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

/**
 * Class instantiation for the Android platform.
 *
 * @author danielberbece
 */
public class AndroidClassFactory implements ClassFactory {

    @Override
    public boolean canLoadClass(String className) {
        if (className == null)
            return false;
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public Object loadClassInstance(String className, MultiTreeMap creationData, boolean splitArguments)
            throws ClassNotFoundException, InstantiationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        Object ret;
        ClassLoader cl = new ClassLoader(AndroidClassFactory.class.getClassLoader()) {
            // nothing to extend
        };

        if (splitArguments) {
            List<String> constructorArguments = new LinkedList<>();
            if (creationData != null)
                for (String key : creationData.getSimpleNames())
                    constructorArguments.add(creationData.getValue(key));
            Class<?>[] argumentTypes = new Class<?>[constructorArguments.size()];
            int i = 0;
            for (Object obj : constructorArguments)
                argumentTypes[i++] = obj.getClass();
            Constructor<?> constructor = cl.loadClass(className).getConstructor(argumentTypes);
            ret = constructor.newInstance(constructorArguments.toArray());
        } else {
            ret = cl.loadClass(className).getConstructor(new Class[]{MultiTreeMap.class})
                    .newInstance(creationData);
        }

        return ret;
    }
}
