package aifolk.onto.vocab;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.semarglproject.vocab.core.RDF;

import fr.inria.corese.core.Graph;
import fr.inria.corese.kgram.api.core.Node;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;

public class TaskDescription extends ExtractableDescription implements ExportableDescription {

  /**
   * The full URI of the task type - e.g. Segmentation, SupervisedClassification, etc
   */
  protected String taskTypeURI;

  /**
   * The name of the task type taken as the local name of the task type URI
   */
  protected String taskTypeName;

  /**
   * The full URI of the application domain of the task - e.g. AutonomousDriving, SearchAndRescue
   */
  protected String domainURI;

  /**
   * The name of the application domain taken as the local name of the domain URI
   */
  protected String domainName;


  protected List<DataContextDescription> dataContextDescriptions = new ArrayList<DataContextDescription>();

  protected TaskDescription() {
    super();
  }

  protected TaskDescription(final String mainNodeURI) {
    super(mainNodeURI);
  }

  public TaskDescription(final Graph modelDescriptionGraph, final String mainNodeURI) {
    super(modelDescriptionGraph, mainNodeURI);
  }

  /**
   * Get the URI of the task node
   * @return the URI of the task node
   */
  public String getTaskURI() {
    return getMainNodeURI();
  }

  /**
   * Get the task node
   * @return the task node
   */
  public Node getTaskNode() {
    return getMainNode();
  }

  /**
   * Get the full URI of the task type - e.g. Segmentation, SupervisedClassification, etc
   * @return the full URI of the task type
   */
  public String getTaskTypeURI() {
    return taskTypeURI;
  }

  /**
   * Get the name of the task type taken as the local name of the task type URI
   * @return the name of the task type
   */
  public String getTaskTypeName() {
    return taskTypeName;
  }

  /**
   * Get the full URI of the application domain of the task - e.g. AutonomousDriving, SearchAndRescue
   * @return the full URI of the application domain of the task
   */
  public String getDomainURI() {
    return domainURI;
  }

  /**
   * Get the name of the application domain taken as the local name of the domain URI
   * @return the name of the application domain of the task
   */
  public String getDomainName() {
    return domainName;
  }

  /**
   * Get the list of data context descriptions
   * @return the list of data context descriptions that characterize the task
   */
  public List<DataContextDescription> getDataContextDescriptions() {
    return dataContextDescriptions;
  }

  @Override
  protected void extractDescription() {
    // extract the task type URI by getting the RDF type of the main node
    final String extractedTaskTypeURI = extractSingleObjectURI(modelDescriptionGraph, mainNodeURI, RDF.TYPE);
    if (extractedTaskTypeURI != null) {
      this.taskTypeURI = extractedTaskTypeURI;
      this.taskTypeName = SimpleValueFactory.getInstance().createIRI(extractedTaskTypeURI).getLocalName();
    }

    // extract the domain URI
    domainURI = extractSingleObjectURI(modelDescriptionGraph, mainNodeURI, CoreVocabulary.HAS_DOMAIN.stringValue());
    if (domainURI != null) {
      domainName = SimpleValueFactory.getInstance().createIRI(domainURI).getLocalName();
    }

    // extract the data context descriptions
    final List<String> dataContextURIs = extractObjectURIs(modelDescriptionGraph, mainNodeURI, CoreVocabulary.HAS_DATA_CONTEXT.stringValue());
    if (dataContextURIs != null) {
      for (final String dataContextURI : dataContextURIs) {
        /*
         * TODO: for now we assume we only have driving scene contexts. We need a mechanism to manage the type of the data context.
         * For now, access to the data context details is done by casting the data context description to a driving scene context description.
         */ 
        final DataContextDescription dataContextDescription = new DrivingSceneContextDescription(modelDescriptionGraph, dataContextURI);
        dataContextDescriptions.add(dataContextDescription);
      }
    }
  }
  
  @Override
  public void populateDescription(final boolean forceUpdate) {
    // call the super method
    super.populateDescription(forceUpdate);

    // call populateDescription on the data context descriptions
    if (dataContextDescriptions != null) {
      for (final DataContextDescription dataContextDescription : dataContextDescriptions) {
        dataContextDescription.populateDescription(forceUpdate);
      }
    }
  }

  public static class Builder {
    private final List<Consumer<TaskDescription>> operations;

    private Builder() {
      operations = new ArrayList<>();
    }

    public static Builder create(final String mainNodeURI, final String taskTypeURI) {
      final Builder builder = new Builder();
      builder.operations.add(desc -> desc.setMainNodeURI(mainNodeURI));
      builder.operations.add(desc -> desc.taskTypeURI = taskTypeURI);
      builder.operations.add(desc -> desc.taskTypeName = SimpleValueFactory.getInstance().createIRI(taskTypeURI).getLocalName());
      return builder;
    }

    public Builder setDomainURI(final String domainURI) {
      operations.add(desc -> desc.domainURI = domainURI);
      operations.add(desc -> desc.domainName = SimpleValueFactory.getInstance().createIRI(domainURI).getLocalName());
      return this;
    }

    public Builder addDataContextDescription(final DataContextDescription dataContextDescription) {
      operations.add(desc -> desc.dataContextDescriptions.add(dataContextDescription));
      return this;
    }

    public TaskDescription build() {
      final TaskDescription taskDescription = new TaskDescription();
      operations.forEach(op -> op.accept(taskDescription));
      return taskDescription;
    }
  }

  @Override
  public Graph exportToGraph() {
    final Graph exportGraph = Graph.create();

    // define the nodes
    final Node taskNode = exportGraph.addResource(getMainNodeURI());
    final Node taskTypeNode = exportGraph.addResource(getTaskTypeURI());
    final Node domainNode = exportGraph.addResource(getDomainURI());

    // define the properties
    final Node hasDomain = exportGraph.addResource(CoreVocabulary.HAS_DOMAIN.stringValue());
    final Node hasDataContext = exportGraph.addResource(CoreVocabulary.HAS_DATA_CONTEXT.stringValue());
    final Node rdfType = exportGraph.addResource(RDF.TYPE);

    // add the triples
    exportGraph.addEdge(taskNode, rdfType, taskTypeNode);
    exportGraph.addEdge(taskNode, hasDomain, domainNode);

    // add the data context descriptions
    for (final DataContextDescription dataContextDescription : dataContextDescriptions) {
      final Node dataContextNode = exportGraph.addResource(dataContextDescription.getMainNodeURI());
      exportGraph.addEdge(taskNode, hasDataContext, dataContextNode);
      exportGraph.merge(dataContextDescription.exportToGraph());
    }

    return exportGraph;
  }
}
