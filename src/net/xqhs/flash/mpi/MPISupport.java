package net.xqhs.flash.mpi;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.*;

import mpi.*;

public class MPISupport extends DefaultPylonImplementation {
    public static final String					MPI_SUPPORT_NAME	= "MPI pylon";

    public MPIMessagingPylonProxy messagingProxy		= new MPIMessagingPylonProxy() {

        @Override
        public boolean send(String destination, String content, int tag)
        {
            try {
                MPI.COMM_WORLD.send(content.toCharArray(), content.length(), MPI.CHAR, Integer.parseInt(destination), tag);
            } catch (MPIException e) {
                e.printStackTrace();
            }

            return true;
        }

        @Override
        public boolean send(String destination, int content, int tag) {
            int[] message = new int[1];
            message[0] = content;

            try {
                MPI.COMM_WORLD.send(message, 1, MPI.INT, Integer.parseInt(destination), tag);
            } catch (MPIException e) {
                e.printStackTrace();
            }

            return true;
        }

        @Override
        public String receive(String source, int messageLength, mpi.Datatype datatype, int tag) {
            char[] message = new char[messageLength];

            try {
                MPI.COMM_WORLD.recv(message, messageLength, MPI.CHAR, Integer.parseInt(source), tag);
            } catch (mpi.MPIException e) {
                System.out.println("MPI couldn't receive message");
            }

            return new String(message);
        }

        @Override
        public int receive(String source, int tag) {
            int[] value = new int[1];

            try {
                MPI.COMM_WORLD.recv(value, 1, MPI.INT, Integer.parseInt(source), tag);
            } catch (mpi.MPIException e) {
                System.out.println("MPI couldn't receive message");
            }

            return value[0];
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
        public boolean sendMessage(String source, String target, String content) {
            return false;
        }

        @Override
        public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context)
        {
            if(!(context instanceof MPIMessagingPylonProxy))
                throw new IllegalStateException("Pylon Context is not of expected type.");
            pylon = (MPIMessagingPylonProxy) context;
            return true;
        }

        public boolean sendMessage(String target, String content, int tag) {
            pylon.send(target, content, tag);
            return true;
        }

        public boolean sendMessage(String target, int content, int tag) {
            pylon.send(target, content, tag);
            return true;
        }

        public String receiveMessage(String source, int messageLength, mpi.Datatype datatype, int tag) {
            return pylon.receive(source, messageLength, datatype, tag);
        }

        public int receiveMessage(String source, int tag) {
            return pylon.receive(source, tag);
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
