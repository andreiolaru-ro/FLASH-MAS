package websocketsTest;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

public class AgentClient extends WebSocketClient implements Agent{

    private String					name;
    private AbstractMessagingShard messagingShard;
    private MessagingPylonProxy pylon;
    public ShardContainer proxy	= new ShardContainer() {
        @Override
        public void postAgentEvent(AgentEvent event)
        {
            System.out.println(event.getValue(
                    AbstractMessagingShard.CONTENT_PARAMETER) + " de la "
                    + event.getValue(AbstractMessagingShard.SOURCE_PARAMETER)
                    + " la " + event.getValue(
                    AbstractMessagingShard.DESTINATION_PARAMETER));
            int message = Integer.parseInt(
                    event.getValue(AbstractMessagingShard.CONTENT_PARAMETER));
            if(message < 5)
            {
                Thread eventThread = new Thread(() -> getMessagingShard()
                        .sendMessage(
                                event.getValue(
                                        AbstractMessagingShard.DESTINATION_PARAMETER),
                                event.getValue(
                                        AbstractMessagingShard.SOURCE_PARAMETER),
                                Integer.toString(
                                        message + 1)));
                eventThread.start();
            }
        }

        @Override
        public String getEntityName()
        {
            return getName();
        }

    };

    public AgentClient(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public AgentClient(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("[ " + getName() + " ] " + "new connection opened " + this.isOpen());
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[ " + getName()  + " ] " + " closed with exit code " + code +
                " because of " + reason);    }

    @Override
    public void onMessage(String message) {
        System.out.println("[ " + getName() + " ] " + message);
    }

    @Override
    public void onMessage(ByteBuffer message) {
        System.out.println("[ " + getName() + " ] "  + "[ received ByteBuffer ] " + message);
    }

    @Override
    public void onError(Exception ex) {
        System.out.println(Arrays.toString(ex.getStackTrace()));
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean start()
    {
        if(name.equals("One"))
        {
            boolean agentInSamePylon = messagingShard.sendMessage(this.getName(), "Two", "1");
            if(!agentInSamePylon) {
                System.out.println("[ " + getName() + " ] : " + "source not in the same pylon");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stop()
    {
        return true;
    }

    @Override
    public boolean isRunning()
    {
        return true;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean addContext(Entity.EntityProxy<Pylon> context)
    {
        pylon = (MessagingPylonProxy) context;
        if(messagingShard != null)
            messagingShard.addGeneralContext(pylon);
        return true;
    }

    @Override
    public boolean addGeneralContext(Entity.EntityProxy<? extends Entity<?>> context)
    {
        return true;
    }

    @Override
    public boolean removeContext(Entity.EntityProxy<Pylon> context)
    {
        pylon = null;
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Entity.EntityProxy<Agent> asContext()
    {
        return proxy;
    }

    public boolean addMessagingShard(AbstractMessagingShard shard)
    {
        messagingShard = shard;
        shard.addContext(proxy);
        if(pylon != null)
            messagingShard.addGeneralContext(pylon);
        return true;
    }

    protected AbstractMessagingShard getMessagingShard()
    {
        return messagingShard;
    }
}



