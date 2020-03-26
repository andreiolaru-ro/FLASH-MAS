package monitoringAndControl;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessageReceiver;

public class CentralMessagingShard  extends AbstractMessagingShard {

    private static final long serialVersionUID = 1L;

    private MonitoringNodeProxy nodeProxy;

    public MessageReceiver inbox;

    public CentralMessagingShard() {
        super();
        inbox = new MessageReceiver() {
            @Override
            public void receive(String source, String destination, String content) {
                receiveMessage(source, destination, content);
            }
        };
    }

    @Override
    protected void receiveMessage(String source, String destination, String content) {
        super.receiveMessage(source, destination, content);
    }

    @Override
    public String extractAgentAddress(String endpoint) {
        return null;
    }

    @Override
    public String getAgentAddress() {
        return null;
    }

    @Override
    public boolean sendMessage(String source, String target, String content) {
        return false;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context)
    {
        if(!(context instanceof MonitoringNodeProxy))
            throw new IllegalStateException("Node Context is not of expected type.");
        nodeProxy = (MonitoringNodeProxy) context;
        nodeProxy.register(getAgent().getEntityName(), inbox);
        return true;
    }
}
