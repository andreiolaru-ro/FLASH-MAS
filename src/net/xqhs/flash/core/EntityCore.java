package net.xqhs.flash.core;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.logging.Unit;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static net.xqhs.flash.core.agent.AgentEvent.AgentEventType.*;

/**
 * Basic implementation of the {@link ConfigurableEntity} interface, supporting:
 * <ul>
 * <li>Logging: the class extends {@link Unit}.
 * <li>Management of the name of the entity: name is found in the configuration given to
 * {@link #configure(MultiTreeMap)}, as a simple value for the key {@value DeploymentConfiguration#NAME_ATTRIBUTE_NAME};
 * the same name is given to the log.
 * <li>running state: running state changes with the {@link #start()} and {@link #stop()} methods.
 * <li>context: <i>main</i> context and <i>general</i> context are managed separately, but if
 * {@link #isMainContext(Object)} is implemented, main context can be managed via
 * {@link #addGeneralContext(net.xqhs.flash.core.Entity.EntityProxy)} and
 * {@link #removeGeneralContext(net.xqhs.flash.core.Entity.EntityProxy)}; context can be retrieved by extending classes
 * via {@link #getContext()} and {@link #getFullContext()}. Main context is also added as general context.
 * </ul>
 *
 * @param <P> - the type of {@link Entity} which is the direct (or main) context for this entity.
 * @author Andrei Olaru
 */
public class EntityCore<P extends Entity<?>> extends Unit implements ConfigurableEntity<P>, Serializable {
    /**
     * The parameter name for focusing (use the "highlighted" state of the log) logging on this entity.
     */
    public static final String HIGHLIGHT_FOCUS = "focus";
    /**
     * The serial UID.
     */
    private static final long serialVersionUID = 6709023622970061354L;
    /**
     * The name provided in the configuration.
     */
    protected String name = null;
    /**
     * The {@link MultiTreeMap} with which this entity was {@link #configure}d.
     */
    private MultiTreeMap entityConfiguration = new MultiTreeMap();
    /**
     * The running state of the entity.
     */
    private boolean running = false;
    /**
     * The <i>main</i> context of this entity.
     */
    private EntityProxy<P> mainContext = null;

    /**
     * The <i>general</i> context of this entity.
     */
    private Set<EntityProxy<? extends Entity<?>>> fullContext = new HashSet<>();

    /**
     * This implementation only reads the name given in the configuration and assigns it to the log.
     */
    @Override
    public boolean configure(MultiTreeMap configuration) {
        if (configuration == null)
            return false;
        if (configuration.isSimple(DeploymentConfiguration.NAME_ATTRIBUTE_NAME)) {
            name = configuration.getAValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
            setUnitName(name);
        }
        if (configuration.isSimple(HIGHLIGHT_FOCUS)) {
            setHighlighted();
        }
        entityConfiguration = configuration;
        return true;
    }

    /**
     * @return the shard configuration stored at {@link #configure(MultiTreeMap)} time.
     */
    public MultiTreeMap getConfiguration() {
        return entityConfiguration;
    }

    /**
     * This implementation only changes the state to <i>running</i>. If the entity has already been started, it returns
     * <code>false</code>.
     */
    @Override
    public boolean start() {
        if (running)
            return ler(false, "Entity is already running");
        lf("[] starting", name);
        running = true;
        net.xqhs.flash.core.recorder.RecorderService.record(this.getName(), AGENT_START.toString(), this.getClass().getName());
        return true;
    }

    /**
     * This implementation only changes the state to <i>not running</i>. If the entity is already stopped, it returns
     * <code>false</code>.
     */
    @Override
    public boolean stop() {
        if (!running)
            return ler(false, "Entity is already stopped");
        lf("[] stopped", name);
        net.xqhs.flash.core.recorder.RecorderService.record(this.getName(), AGENT_STOP.toString());
        running = false;
        return true;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * This implementation returns the name given in the configuration.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns <code>true</code> if the main context has changed.
     */
    @Override
    public boolean addContext(EntityProxy<P> context) {
        if (mainContext == context)
            return false;
        if (mainContext != null)
            fullContext.remove(mainContext);
        mainContext = context;
        fullContext.add(context);
        return true;
    }

    /**
     * This implementation returns <code>true</code> if the "main context" was the same instance as the provided
     * argument.
     */
    @Override
    public boolean removeContext(EntityProxy<P> context) {
        if (mainContext == null)
            return ler(false, "There was no main context present.");
        if (mainContext != context)
            return ler(false, "Main context was not the same as the given argument.");
        mainContext = null;
        fullContext.remove(context);
        return true;
    }

    /**
     * @return the <i>main</i> context of the entity.
     * @see #addContext(net.xqhs.flash.core.Entity.EntityProxy) and {@link #isMainContext(Object)}.
     */
    protected EntityProxy<P> getContext() {
        return mainContext;
    }

    /**
     * Confirms that the given instance is of the same type as the main context of the entity (with which the extending
     * class is parameterized.)
     * <p>
     * Extending classes may return any value if they do not need to differentiate between "main" context and general
     * context elements. They should <b>not</b> call the overridden method.
     * <p>
     * The implementation works if this method remains unimplemented, but main context will not be recognized as such by
     * {@link #addGeneralContext(net.xqhs.flash.core.Entity.EntityProxy)} and
     * {@link #removeGeneralContext(net.xqhs.flash.core.Entity.EntityProxy)}.
     *
     * @param context - the given context.
     * @return <code>true</code> if the given instance is of the same type as the main context.
     */
    @SuppressWarnings("static-method")
    public boolean isMainContext(Object context) {
        throw new UnsupportedOperationException("This functionality is not implemented.");
    }

    /**
     * This implementation returns <code>true</code> if a change really happened, that is, if the given argument was not
     * part of the context.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        try {
            if (isMainContext(context))
                return addContext((EntityProxy<P>) context);
        } catch (UnsupportedOperationException e) {
            // nothing to do
        }
        return fullContext.add(context);
    }

    /**
     * This implementation returns <code>true</code> if a change really happened, that is, if the given argument was
     * indeed part of the context.
     * <p>
     * WARNING: possible errors when a different instance of the same type with the current main context is removed, as
     * it will nullify the main context, but the previous main context will remain in the full context.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
        try {
            if (isMainContext(context))
                return removeContext((EntityProxy<P>) context);
        } catch (UnsupportedOperationException e) {
            // nothing to do
        }
        return fullContext.remove(context);
    }

    /**
     * @return the elements forming the general context of this entity.
     */
    protected Set<EntityProxy<? extends Entity<?>>> getFullContext() {
        return fullContext;
    }

    @Override
    public <C extends Entity<P>> EntityProxy<C> asContext() {
        throw new UnsupportedOperationException("This functionality is not implemented.");
    }

    /**
     * [RECORDER HOOK]
     * We extend the logging method from the parent class Unit
     * Intercepted calls: li() info, le() error, lf() trace, etc.
     */
    @Override
    protected void l(net.xqhs.util.logging.Logger.Level messageLevel, String message, Object... arguments) {
        super.l(messageLevel, message, arguments);
        String formattedMessage = Unit.compose(message, arguments);
        net.xqhs.flash.core.recorder.RecorderService.record(this.getUnitName(), "LOG_" + messageLevel.name(),
                formattedMessage);
    }

    // Fix for the console displaying the full path of entities
    @Override
    public String toString() {
        String n = this.getName();
        return (n != null) ? n : super.toString();
    }
}
