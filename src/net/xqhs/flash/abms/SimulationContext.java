package net.xqhs.flash.abms;

import java.util.Deque;

import net.xqhs.flash.abms.SimulationContext.ActionRecord.ActionStatus;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.util.MultiValueMap;

public interface SimulationContext {
	public class ActionRecord {
		public enum ActionStatus {
			INITIALIZED, PLANNED, PENDING, FAILED, COMPLETED,
		}
		
		ActionStatus	status;
		EntityProxy<?>	entity;
		MultiValueMap	actionData				= null;
		MultiValueMap	completionInformation	= null;
		
		public ActionRecord(EntityProxy<?> e, MultiValueMap data) {
			status = ActionStatus.INITIALIZED;
			entity = e;
			actionData = (MultiValueMap) data.lock();
		}
		
		public void setStatus(ActionStatus status, MultiValueMap information) {
			this.status = status;
			if(information != null)
				this.completionInformation = information;
		}
		
		public ActionStatus getStatus() {
			return status;
		}
		
		public MultiValueMap getCompletionInformation() {
			return completionInformation;
		}
		
		public MultiValueMap getActionData() {
			return actionData;
		}
		
		public EntityProxy<?> getEntity() {
			return entity;
		}
	}
	
	public abstract class BaseContext extends EntityCore<Simulation> implements SimulationContext {
		public enum BaseActionData implements ActionData {
			ACTION
			
			;
			
			@Override
			public String s() {
				return this.toString();
			}
		}
		
		protected Deque<ActionRecord> pendingActions;
		
		@Override
		public boolean addPendingAction(ActionRecord action) {
			if(action.getStatus() != ActionStatus.INITIALIZED)
				return false;
			pendingActions.add(action);
			action.setStatus(ActionStatus.PENDING, null);
			return true;
		}
		
		public abstract void validateAndExecutependingActions();
	}
	
	public interface ActionData {
		String s();
	}
	
	boolean addPendingAction(ActionRecord action);
}



