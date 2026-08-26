package net.xqhs.flash.pythonBridge;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.xqhs.flash.core.agent.AgentWave;

/**
 * Bridge that runs Python <i>inside this JVM</i> through JEP (Java Embedded Python). There is no server, no socket and no serialization:
 * {@code interp.invoke("module.function", args)} calls straight into a CPython interpreter loaded into the
 * process, and Python values come back as ordinary Java objects (a dict becomes a {@link HashMap}, a float
 * becomes a {@link Double}).
 * <p>
 */
public class PythonJepBridgeDriver extends PythonBridgeDriver {

    /** Python module invoked through JEP */
    protected static final String	DEFAULT_PYTHON_MODULE		= "jep_module";
    /** Function called when a wave carries no destination element. */
    protected static final String	DEFAULT_FUNCTION			= "run_inference";
    protected static final String	WINDOWS_JEP_LIBRARY			= "jep.dll";
    protected static final String	UNIX_JEP_LIBRARY			= "libjep.so";
    protected static final int		INTERPRETER_INIT_TIMEOUT_S	= 60;

    protected final String			pythonModule;
    protected final ExecutorService	jepExecutor					= Executors.newSingleThreadExecutor();
    protected volatile Object		interpreter;
    protected volatile URLClassLoader jepClassLoader;
    protected Method				invokeMethod;

    public PythonJepBridgeDriver(String pythonModule, String venvRelativePath) {
        super(venvRelativePath);
        this.pythonModule = pythonModule;
    }

    public PythonJepBridgeDriver() {
        this(DEFAULT_PYTHON_MODULE, DEFAULT_VENV_PATH);
    }

    /**
     * First runs the Python module as a plain script, importing all packages (including jep).
     * Then the embedded interpreter is created.
     */
    @Override
    protected AttemptResult attemptStart() {
        Path probe = pythonModuleFile(pythonModule + ".py");
        AttemptResult result = runScriptToCompletion(probe);
        if(!result.started)
            return result;
        result.started = initInterpreter();
        return result;
    }

    /** Loads jep from the venv and creates the interpreter, all on {@link #jepExecutor}'s thread. */
    protected boolean initInterpreter() {
        try {
            String sitePackages = queryVenvPython("import sysconfig; print(sysconfig.get_paths()['purelib'])");
            // note: this deliberately avoids `import jep` -- jep raises "not supported in standalone Python"
            // when imported outside a JVM, so its location has to be found without importing it.
            String jepDir = queryVenvPython(
                    "import importlib.util, os; print(os.path.dirname(importlib.util.find_spec('jep').origin))");
            if(sitePackages == null || jepDir == null) {
                le("Could not locate site-packages ([]) or the jep package ([]) in the venv.", sitePackages, jepDir);
                return false;
            }
            File jepJar = findJepJar(new File(jepDir));
            if(jepJar == null) {
                le("No jep-*.jar found in []", jepDir);
                return false;
            }
            String jepLibrary = new File(jepDir, isWindows() ? WINDOWS_JEP_LIBRARY : UNIX_JEP_LIBRARY).getAbsolutePath();
            li("Loading jep from [] (native library [])", jepJar, jepLibrary);

            jepClassLoader = new URLClassLoader(new URL[] { jepJar.toURI().toURL() }, getClass().getClassLoader());
            String moduleDir = pythonModuleFile(pythonModule + ".py").getParent().toAbsolutePath().toString();

            return jepExecutor.submit(() -> {
                Class<?> mainInterpreter = Class.forName("jep.MainInterpreter", true, jepClassLoader);
                try {
                    mainInterpreter.getMethod("setJepLibraryPath", String.class).invoke(null, jepLibrary);
                } catch(Exception e) {
                    // already set by an earlier instance in this JVM
                    lf("jep library path was already set: []", e);
                }
                Class<?> configClass = Class.forName("jep.JepConfig", true, jepClassLoader);
                Object config = configClass.getDeclaredConstructor().newInstance();
                configClass.getMethod("addIncludePaths", String[].class).invoke(config,
                        (Object) new String[] { sitePackages, moduleDir });
                configClass.getMethod("redirectStdout", java.io.OutputStream.class).invoke(config, System.out);
                configClass.getMethod("redirectStdErr", java.io.OutputStream.class).invoke(config, System.err);

                Class<?> sharedClass = Class.forName("jep.SharedInterpreter", true, jepClassLoader);
                sharedClass.getMethod("setConfig", configClass).invoke(null, config);
                interpreter = sharedClass.getDeclaredConstructor().newInstance();
                interpreter.getClass().getMethod("exec", String.class).invoke(interpreter, "import " + pythonModule);
                invokeMethod = interpreter.getClass().getMethod("invoke", String.class, Object[].class);
                li("Embedded Python interpreter ready; module [] imported.", pythonModule);
                return Boolean.TRUE;
            }).get(INTERPRETER_INIT_TIMEOUT_S, TimeUnit.SECONDS);
        } catch(Exception e) {
            le("Failed to initialize the embedded interpreter: []", e);
            return false;
        }
    }

    protected File findJepJar(File jepDir) {
        File[] jars = jepDir.listFiles((dir, name) -> name.startsWith("jep") && name.endsWith(".jar"));
        return (jars == null || jars.length == 0) ? null : jars[0];
    }

    @Override
    protected void stopPython() {
        try {
            if(interpreter != null)
                jepExecutor.submit((Callable<Void>) () -> {
                    ((AutoCloseable) interpreter).close();
                    interpreter = null;
                    return null;
                }).get(10, TimeUnit.SECONDS);
        } catch(Exception e) {
            lf("Closing the interpreter failed: []", e);
        }
        jepExecutor.shutdownNow();
        try {
            if(jepClassLoader != null)
                jepClassLoader.close();
        } catch(Exception e) {
            lf("Closing the jep class loader failed: []", e);
        }
    }

    /**
     * Calls the Python function named by the wave's first destination element (defaulting to
     * {@link #DEFAULT_FUNCTION}), passing the wave's payload as a single dict argument. The Python return value
     * arrives as a plain Java object; its string form becomes the reply's content.
     */
    @Override
    protected AgentWave doProcess(AgentWave wave) throws Exception {
        checkReady();
        String[] destinations = wave.getDestinationElements();
        String function = (destinations != null && destinations.length > 0) ? destinations[0] : DEFAULT_FUNCTION;

        Map<String, Object> payload = new HashMap<>();
        for(String key : wave.getContentElements()) {
            List<String> values = wave.getValues(key);
            payload.put(key, values.size() == 1 ? values.get(0) : values);
        }

        Object result = jepExecutor.submit(() -> invokeMethod.invoke(interpreter, pythonModule + "." + function,
                new Object[] { payload })).get();
        return wave.createReply(String.valueOf(result));
    }
}