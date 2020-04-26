package net.xqhs.flash.mpi;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.*;

import mpi.*;

public class MPISupport extends DefaultPylonImplementation {
    public static final String					MPI_SUPPORT_NAME	= "MPI pylon";

    public MPIMessagingPylonProxy messagingProxy		= new MPIMessagingPylonProxy() {

        @Override
        public boolean send(String source, String destination, String content)
        {
            try {
                MPI.COMM_WORLD.send(content.toCharArray(), content.length(), MPI.CHAR, Integer.parseInt(destination), 0);
            } catch (MPIException e) {
                e.printStackTrace();
            }

            return true;
        }

        @Override
        public String receive(String source, int messageLength, int tag, mpi.Datatype datatype) {
            char[] message = new char[messageLength];

            try {
                MPI.COMM_WORLD.recv(message, messageLength, MPI.CHAR, Integer.parseInt(source), 0);
            } catch (mpi.MPIException e) {
                System.out.println("MPI couldn't receive message");
            }

            return new String(message);
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
        private MPIMessagingPylonProxy pylon;

        public MPIMessaging()
        {
            super();
        }

        @Override
        public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context)
        {
            if(!(context instanceof MPIMessagingPylonProxy))
                throw new IllegalStateException("Pylon Context is not of expected type.");
            pylon = (MPIMessagingPylonProxy) context;
            return true;
        }

        @Override
        public boolean sendMessage(String source, String target, String content) {

            pylon.send(source, target, content);

            return true;
        }

        public String receiveMessage(String source, int messageLength, int tag, mpi.Datatype datatype) {
            return pylon.receive(source, messageLength, tag, datatype);
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
