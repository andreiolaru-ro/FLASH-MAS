package monitoringAndControl;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Debug;

public abstract class AbstractMonitoringShard extends AgentShardCore {

    public static enum MonitoringDebug implements Debug.DebugItem {
        /**
         * General messaging debugging switch.
         */
        DEBUG_MONITORING(true),

        ;

        /**
         * Activation state.
         */
        boolean isset;

        /**
         * Default constructor.
         *
         * @param set - activation state.
         */
        private MonitoringDebug(boolean set) {
            isset = set;
        }

        @Override
        public boolean toBool() {
            return isset;
        }
    }

    /**
     * The serial UID.
     */
    private static final long serialVersionUID = -7541956285166819419L;

    /**
     * The string separating elements of an endpoint address.
     */
    public static final String ADDRESS_SEPARATOR = "/";
    /**
     * The name of the parameter in an {@link AgentEvent} associated with a command,
     * that corresponds to the source address of the log.
     */
    public static final String SOURCE_PARAMETER = "source address";

    public static final String DESTINATION_PARAMETER = "destination address";


    /*
    * The log received from an agent.
    * */
    public static final String LOG_CONTENT = "log";


    /**
     * Default constructor.
     */
    public AbstractMonitoringShard() {
        super(AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MONITORING));
    }

    protected void receiveCommand(String s, String source, String log) {
        AgentEvent event = new AgentEvent(AgentEvent.AgentEventType.AGENT_WAVE);
        try {
            event.add(SOURCE_PARAMETER, source);
            event.add(LOG_CONTENT, log);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Config locked:" + PlatformUtils.printException(e));
        }

        /*
        * Post event in CentralMonitoringAndControlEntity. This will forward the log in the GUI component.
        * */
        getAgent().postAgentEvent(event);
    }


    public String makePath(String targetAgent, String... internalElements) {
        throw new UnsupportedOperationException("not implemented");
    }


    @SuppressWarnings("static-method")
    public String makeInternalPath(String... internalElements) {
        return makePathHelper(null, internalElements);
    }


    // TODO: fixed with agent naming
    public String makeLocalPath(String... elements) {
        return makePathHelper(getAgentAddress(), elements);
    }


    /*
    * The path to the entity will be created using the separator.
    * */
    public static String makePathHelper(String start, String... elements) {
        String ret = (start != null) ? start : "";
        for (String elem : elements)
            if (elem != null)
                ret += ADDRESS_SEPARATOR + elem;
        return ret;
    }



    public String getAgentAddress() {
        throw new UnsupportedOperationException("not implemented");
    }

    public String[] extractInternalDestinationElements(AgentEvent event, String... prefixElementsToRemove) {
        String prefix = makeInternalPath(prefixElementsToRemove);
        String rem = extractInternalDestination(event, prefix);
        String[] ret = null;
        if (rem != null)
            ret = (rem.startsWith(ADDRESS_SEPARATOR) ? rem.substring(1) : rem).split(ADDRESS_SEPARATOR);
        return ret;
    }

    // TODO: logging
    public String extractInternalDestination(AgentEvent event, String prefixToRemove) {
        if (event == null) {
            return null;
        }
        String address = event.get(DESTINATION_PARAMETER);
        if (!address.startsWith(getAgentAddress())) {
            try {
                // getAgentLog().error("Address [] does not begin with this agent's address",
                // address);
            } catch (NullPointerException e) {
                // nothing
            }
            return null;
        }
        String rem = address.substring(getAgentAddress().length());
        if (!rem.startsWith(prefixToRemove)) {
            try {
                // getAgentLog().warn("Internal path [] does not begin with the specified prefix
                // []", rem, prefixToRemove);
            } catch (NullPointerException e) {
                // nothing
            }
            return rem;
        }
        return rem.substring(prefixToRemove.length());
    }


    public String extractInternalAddress(String endpoint) {
        throw new UnsupportedOperationException("not implemented");
    }


    public String[] extractInternalAddressElements(String endpoint) {
        String internalPath = extractInternalAddress(endpoint);
        return (internalPath.startsWith(ADDRESS_SEPARATOR) ? internalPath.substring(1) : internalPath)
                .split(ADDRESS_SEPARATOR);
    }

    public abstract  boolean startEntity(String address);

    public abstract boolean stopEntity(String address);

    public abstract boolean sendCommand(String source, String target, String content);

}
