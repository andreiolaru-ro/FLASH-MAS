package example.helloWorld;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.util.logging.Unit;

public class HelloAgent extends Unit implements Agent {
	
	@Override
	public boolean start() {
		// TODO Auto-generated method stub
		System.out.println("Hello World");
		// li("Hello World");
		return true;
	}
	
	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean isRunning() {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean addContext(EntityProxy<Pylon> context) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean removeContext(EntityProxy<Pylon> context) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
