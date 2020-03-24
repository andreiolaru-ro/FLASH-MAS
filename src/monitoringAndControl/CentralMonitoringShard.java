package monitoringAndControl;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;

import java.util.List;

public class CentralMonitoringShard extends AbstractMonitoringShard {

    private static final long serialVersionUID = 1L;

    private MonitoringNodeProxy nodeProxy;

    public MonitoringReceiver inbox;

    public CentralMonitoringShard() {
        super();
        inbox = new MonitoringReceiver() {
            @Override
            public void receive(String source, String destination, String content) {
                receiveCommand(source, destination, content);
            }
        };
    }

    @Override
    public boolean startEntity(String entityName) {
        String entityAddress = makePathHelper(entityName, "control");
        Agent agent = getAgentWithName(entityName);
        return agent.start();
    }

    @Override
    public boolean stopEntity(String entityName) {
        String entityAddress = makePathHelper(entityName, "control");
        Agent agent = getAgentWithName(entityName);
        return agent.stop();
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

    /*
    * source = name of CMACE;
    * target = name of agent destination;
    * command = command
    * */
    @Override
    public boolean sendCommand(String source, String target, String command) {
        return true;
    }


    @Override
    protected void receiveCommand(String source, String destination, String content)
    {
        super.receiveCommand(source, destination, content);
    }

    private Agent getAgentWithName(String entityName) {
        List<Agent> availableAgents = nodeProxy.getAgentTypeEntities();

        for(Agent a : availableAgents) {
            if(a.getName().equals(entityName)) {
                return a;
            }
        }
        return null;
    }

}
