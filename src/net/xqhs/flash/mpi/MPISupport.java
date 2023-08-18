package net.xqhs.flash.mpi;

import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.*;

import mpi.*;
import java.util.HashMap;
import static stefania.TreasureHunt.util.Constants.MPITagValue;

public class MPISupport extends DefaultPylonImplementation {
    public static final String					MPI_SUPPORT_NAME	= "MPI pylon";
    protected HashMap<String, MessageReceiver> messageReceivers	= new HashMap<>();

    public MessagingPylonProxy messagingProxy		= new MessagingPylonProxy() {

        @Override
        public boolean register(String agentName, MessageReceiver receiver) {
            messageReceivers.put(agentName, receiver);
            return true;
        }

        @Override
		public boolean unregister(String entityName, MessageReceiver registeredReceiver) {
			return messageReceivers.remove(entityName, registeredReceiver);
		}

		@Override
        public boolean send(String source, String destination, String content) {
            try {
                byte[] msg = content.getBytes();
//                char[] msg = content.toCharArray();
                MPI.COMM_WORLD.send(msg, msg.length, MPI.BYTE, Integer.parseInt(destination), MPITagValue);
            } catch (MPIException e) {
                e.printStackTrace();
            }

            return true;
        }

        @Override
        public String getRecommendedShardImplementation(
                AgentShardDesignation shardType)
        {
            return MPISupport.this
                    .getRecommendedShardImplementation(
                            shardType);
        }

        @Override
        public String getEntityName()
        {
            return getName();
        }
    };

    @Override
    public String getRecommendedShardImplementation(AgentShardDesignation shardName)
    {
        return null;
    }

    @Override
    public String getName()
    {
        return MPI_SUPPORT_NAME;
    }

    @SuppressWarnings("unchecked")
    @Override
    public EntityProxy<Pylon> asContext()
    {
        return messagingProxy;
    }
}
