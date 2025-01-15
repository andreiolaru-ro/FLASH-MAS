package net.xqhs.flash.core;

import java.util.HashSet;
import java.util.Set;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.logging.Unit;

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
 * @author Andrei Olaru
 *
 * @param <P>
 *            - the type of {@link Entity} which is the direct (or main) context for this entity.
 */
public class EntityCore<P extends Entity<?>> extends Unit implements ConfigurableEntity<P> {
	
	/**
	 * The running state of the entity.
	 */
	protected boolean	running	= false;
	/**
	 * The name provided in the configuration.
	 */
	protected String	name	= null;
	
	/**
	 * The <i>main</i> context of this entity.
	 */
	EntityProxy<P> mainContext = null;
	
	/**
	 * The <i>general</i> context of this entity.
	 */
	Set<EntityProxy<? extends Entity<?>>> fullContext = new HashSet<>();
	
	/**
	 * This implementation only reads the name given in the configuration and assigns it to the log.
	 */
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(configuration != null)
			if(configuration.isSimple(DeploymentConfiguration.NAME_ATTRIBUTE_NAME)) {
				name = configuration.getAValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
				setUnitName(name);
			}
		return true;
	}
	
	/**
	 * This implementation only changes the state to <i>running</i>. If the entity has already been started, it returns
	 * <code>false</code>.
	 */
	@Override
	public boolean start() {
		if(running)
			return false;
		lf("[] starting", name);
		running = true;
		return true;
	}
	
	/**
	 * This implementation only changes the state to <i>not running</i>. If the entity is already stopped, it returns
	 * <code>false</code>.
	 */
	@Override
	public boolean stop() {
		if(!running)
			return false;
		lf("[] stopped", name);
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
	
	@Override
	public boolean addContext(EntityProxy<P> context) {
		mainContext = context;
		fullContext.add(context);
		return true;
	}
	
	@Override
	public boolean removeContext(EntityProxy<P> context) {
		if(mainContext != context)
			return false;
		mainContext = null;
		fullContext.remove(context);
		return true;
	}
	
	/**
	 * @return the <i>main</i> context of the entity.
	 * 
	 * @see #addContext(net.xqhs.flash.core.Entity.EntityProxy) and {@link #isMainContext(Object)}.
	 */
	protected EntityProxy<P> getContext() {
		return mainContext;
	}
	
	/**
	 * Confirms that the given instance is of the same type as the main context of the entity (with which the extending
	 * class is parameterized.
	 * <p>
	 * Extending classes may return any value if they do not need to differentiate between "main" context and general
	 * context elements. They should <b>not</b> call the overridden method.
	 * <p>
	 * The implementation works if this method remains unimplemented, but main context will not be recognized as such by
	 * {@link #addGeneralContext(net.xqhs.flash.core.Entity.EntityProxy)} and
	 * {@link #removeGeneralContext(net.xqhs.flash.core.Entity.EntityProxy)}.
	 * 
	 * @param context
	 *            - the given context.
	 * @return <code>true</code> if the given instance is of the same type as the main context.
	 */
	@SuppressWarnings("static-method")
	public boolean isMainContext(Object context) {
		throw new UnsupportedOperationException("This functionality is not implemented.");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		try {
			if(isMainContext(context))
				addContext((EntityProxy<P>) context);
		} catch(UnsupportedOperationException e) {
			// nothing to do
		}
		return fullContext.add(context);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
		try {
			if(isMainContext(context))
				removeContext((EntityProxy<P>) context);
		} catch(UnsupportedOperationException e) {
			// nothing to do
		}
		return fullContext.remove(context);
	}
	
	/**
	 * @return the elements forming the general context of this entity.
	 */
	Set<EntityProxy<? extends Entity<?>>> getFullContext() {
		return fullContext;
	}
	
	@Override
	public <C extends Entity<P>> EntityProxy<C> asContext() {
		throw new UnsupportedOperationException("This functionality is not implemented.");
	}
	
}
