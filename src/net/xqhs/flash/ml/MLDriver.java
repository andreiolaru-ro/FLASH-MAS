package net.xqhs.flash.ml;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.node.Node;
import net.xqhs.util.logging.Unit;

public class MLDriver extends Unit implements Entity<Node>, EntityProxy<MLDriver> {
	
	@Override
	public boolean start() {
		// TODO Auto-generated method stub
		
		// start the python server, capture the server's stdin, stdout, stderr
		
		return false;
	}
	
	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean isRunning() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void addModel() {
		// send to the Python server all information needed to load the model
	}
	
	public void predict() {
		
	}
	
	// TODO other methods
	
	@Override
	public boolean addContext(EntityProxy<Node> context) {
		return false;
	}
	
	@Override
	public boolean removeContext(EntityProxy<Node> context) {
		return false;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		return false;
	}
	
	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
		return false;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<MLDriver> asContext() {
		return this;
	}
	
	@Override
	public String getEntityName() {
		return getName();
	}
	
}
