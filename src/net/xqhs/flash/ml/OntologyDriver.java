package net.xqhs.flash.ml;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;


public class OntologyDriver extends EntityCore<Node> implements EntityProxy<OntologyDriver> {

	@Override
	public boolean configure(MultiTreeMap configuration) {
		setUnitName(configuration.getAValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME));
		return true;
	}
	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
		li("Ontology driver up");
		return true;
	}
	
	@Override
	public boolean stop() {
		if(!super.stop())
			return false;
		li("Ontology driver stopped");
		return true;
	}
	
	// TODO
	// Query buildQuery(Set<Criteria>)
	// String[] runQuery(Query, InstanceSet) -- returns modelIDs
	// addModelDescription(InstanceSet, String modelID, Description)
	// removeModelDescription(InstanceSet, String modelID)
	// updateModelDescription(InstanceSet, String modelID, updateData)
	
	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<OntologyDriver> asContext() {
		return this;
	}
	
	@Override
	public String getEntityName() {
		return getName();
	}
	
}
