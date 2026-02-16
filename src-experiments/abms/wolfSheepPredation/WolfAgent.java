package abms.wolfSheepPredation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.xqhs.flash.abms.EnvironmentLinkShard;
import net.xqhs.flash.abms.SteppableEntity;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.support.Pylon;

public class WolfAgent extends BaseAgent implements SteppableEntity, EntityProxy<BaseAgent> {
	
	protected EnvironmentLinkShard	e		= new EnvironmentLinkShard();
	protected final Random			random	= new Random();
	
	public WolfAgent() {
		e.addContext(asContext());
	}
	
	@Override
	public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
		return (EntityProxy<C>) this;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		e.addGeneralContext(context);
		return super.addGeneralContext(context);
	}
	
	@Override
	public void step() {
		li("wolf step");
		Position currentPos = e.getCurrentPosition();
		if(currentPos == null) {
			return;
		}
		
		Set<Position> freeNeighbors = e.getFreeNeighborPositions(currentPos);
		if(freeNeighbors.isEmpty()) {
			return;
		}
		
		List<Position> freeList = new ArrayList<>(freeNeighbors);
		Position newPos = freeList.get(random.nextInt(freeList.size()));
		e.moveToPosition(newPos);
	}
	
	@Override
	public String getEntityName() {
		return getName() != null ? getName() : "Wolf";
	}
}
