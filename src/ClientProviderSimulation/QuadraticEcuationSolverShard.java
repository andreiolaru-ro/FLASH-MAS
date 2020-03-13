package ClientProviderSimulation;

import static java.lang.Math.*;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.MessagingPylonProxy;

public class QuadraticEcuationSolverShard extends AgentShardCore {
    /**
     * The constructor assigns the designation to the shard.
     * <p>
     * IMPORTANT: extending classes should only perform in the constructor initializations that do not depend on the
     * parent agent or on other shards, as when the shard is created, the {@link AgentShardCore#parentAgent} member is
     * <code>null</code>. The assignment of a parent (as any parent change) is notified to extending classes by calling
     * the method {@link AgentShardCore#parentChangeNotifier}.
     * <p>
     * Event registration is not dependent on the parent, so it can be performed in the constructor or in the
     * {@link #shardInitializer()} method.
     *
     * @param designation - the designation of the shard, as instance of {@link AgentShardDesignation.StandardAgentShard}.
     */
    protected QuadraticEcuationSolverShard(AgentShardDesignation designation) {
        super(designation);
    }

    private MessagingPylonProxy pylon;
    public static final String EQUATION_ROOTS = "quadratic equation roots";


    public void findQuadraticEqationRoots(int a, int b, int c) {
        String x1 = "0", x2 = "0";
        if (a != 0) {
            int d  = b*b - 4*a*c;
            double sqrt_val = sqrt(abs(d));

            if(d > 0) {
                x1 = (double)(-b + sqrt_val) / (2 * a) + "";
                x2 = (double)(-b - sqrt_val) / (2 * a) + "";
            } else {
                x1 = -(double)b / ( 2 * a ) + " + i"
                        + sqrt_val;
                x2 = -(double)b / ( 2 * a )
                        + " - i" + sqrt_val;
            }
        }

        AgentEvent event = new AgentEvent(AgentEvent.AgentEventType.AGENT_WAVE);
        event.add(EQUATION_ROOTS, x1 + " " + x2);
        getAgent().postAgentEvent(event);

    }



    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if(!(context instanceof MessagingPylonProxy))
            throw new IllegalStateException("Pylon Context is not of expected type.");
        pylon = (MessagingPylonProxy) context;
        return true;
    }

}
