package aifolk.onto;

import aifolk.onto.vocab.ExtractableDescription;
import aifolk.onto.vocab.ModelDescription;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.LoadException;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ml.MLDriver;

public class OntologyDriver extends EntityCore<Node> implements EntityProxy<OntologyDriver> {
	
	static final String FILE_KEY = "load";
	
	Graph graph = null;
	
	@Override
	public boolean configure(final MultiTreeMap configuration) {
		setUnitName(configuration.getAValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME));
		for(String file : configuration.getValues(FILE_KEY))
			try {
				graph = ExtractableDescription.getGraphFromFile(MLDriver.ML_DIRECTORY_PATH + file);
				li("Loaded [], graph is [].", file, graph.toString2());
			} catch(LoadException e) {
				le("Cannot load graph []:", file, e);
			}
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
	
	public Graph getGraph() {
		return graph;
	}
	
	// TODO
	// Query buildQuery(Set<Criteria>)
	
	public QueryResult[] runQuery(final Query query) {
		// TODO
		return null;
	}
	
	public boolean addModelDescription(final String modelID, final ModelDescription descr) {
		// TODO
		return true;
	}
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
