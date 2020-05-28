package net.xqhs.flash.mpi;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.*;

import mpi.*;
import java.util.HashMap;
import static stefania.TreasureHunt.util.Constants.*;

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
        public boolean send(String source, String destination, String content) {
            try {
                MPI.COMM_WORLD.send(content.toCharArray(), content.length(), MPI.CHAR, Integer.parseInt(destination), 0);
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

    public static class MPIMessaging extends AbstractNameBasedMessagingShard {

        private static final long	serialVersionUID	= 1L;
        private MessagingPylonProxy pylon;
        private String message;

        public MPIMessaging()
        {
            super();
            this.message = "";
        }

        public String getMessage() {
            return this.message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context)
        {
            if(!(context instanceof MessagingPylonProxy))
                throw new IllegalStateException("Pylon Context is not of expected type.");
            pylon = (MessagingPylonProxy) context;
            return true;
        }

        @Override
        public boolean sendMessage(String source, String destination, String content) {
            if(pylon == null) { // FIXME: use logging
                System.out.println("No pylon added as context.");
                return false;
            }

            pylon.send(source, destination, content);
            return true;
        }

        @Override
        public void receiveMessage(String source, String destination, String content) {
            try {
                Status status = MPI.COMM_WORLD.probe(Integer.parseInt(source), MPITagValue);
                int length = status.getCount(MPI.CHAR);
                char[] rawMessage = new char[length];
                MPI.COMM_WORLD.recv(rawMessage, length, MPI.CHAR, Integer.parseInt(source), MPITagValue);
                setMessage(String.valueOf(rawMessage));
            } catch (MPIException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getRecommendedShardImplementation(AgentShardDesignation shardName)
    {
        if(shardName.equals(AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MESSAGING)))
            return MPISupport.MPIMessaging.class.getName();
        return super.getRecommendedShardImplementation(shardName);
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
