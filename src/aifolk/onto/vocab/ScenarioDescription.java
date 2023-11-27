package aifolk.onto.vocab;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.semarglproject.vocab.core.RDF;

import fr.inria.corese.core.Graph;
import fr.inria.corese.kgram.api.core.Node;

public class ScenarioDescription extends ExtractableDescription implements ExportableDescription {

  protected List<TaskDescription> taskCharacterizations = new ArrayList<TaskDescription>();

  protected ScenarioDescription() {
    super();
  }

  protected ScenarioDescription(final String mainNodeURI) {
    super(mainNodeURI);
  }

  public ScenarioDescription(final Graph modelDescriptionGraph, final String mainNodeURI) {
    super(modelDescriptionGraph, mainNodeURI);
  }

  /**
   * Get the URI of the scenario node
   * @return the URI of the scenario node
   */
  public String getScenarioURI() {
    return getMainNodeURI();
  }

  /**
   * Get the scenario node
   * @return the scenario node
   */
  public Node getScenarioNode() {
    return getMainNode();
  }

  /**
   * Get the list of task characterizations
   * @return the list of task characterizations that characterize the scenario
   */
  public List<TaskDescription> getTaskCharacterizations() {
    return taskCharacterizations;
  }

  @Override
  protected void extractDescription() {
    // extract the task characterizations
    final List<String> taskCharacterizationURIs = extractObjectURIs(modelDescriptionGraph, mainNodeURI, CoreVocabulary.CONTAINS_TASK.stringValue());
    if (taskCharacterizationURIs != null) {
      for (final String taskCharacterizationURI : taskCharacterizationURIs) {
        final TaskDescription taskDescription = new TaskDescription(modelDescriptionGraph, taskCharacterizationURI);
        taskCharacterizations.add(taskDescription);
      }
    }
  }
  
  @Override
  public void populateDescription(final boolean forceUpdate) {
    // call the super method
    super.populateDescription(forceUpdate);

    // call populateDescription on the task characterizations
    if (taskCharacterizations != null) {
      for (final TaskDescription taskDescription : taskCharacterizations) {
        taskDescription.populateDescription(forceUpdate);
      }
    }
  }

  public static class Builder {
    private final List<Consumer<ScenarioDescription>> operations;

    private Builder() {
      operations = new ArrayList<>();
    }

    public static Builder create(final String mainNodeURI) {
      final Builder builder = new Builder();
      builder.operations.add(desc -> desc.setMainNodeURI(mainNodeURI));
      return builder;
    }

    public Builder addTaskCharacterization(final TaskDescription taskDescription) {
      operations.add(desc -> desc.taskCharacterizations.add(taskDescription));
      return this;
    }
      
    public ScenarioDescription build() {
      final ScenarioDescription scenarioDescription = new ScenarioDescription();
      operations.forEach(operation -> operation.accept(scenarioDescription));
      return scenarioDescription;
    }
  }

  @Override
  public Graph exportToGraph() {
    final Graph exportGraph = Graph.create();

    // define the nodes
    final Node scenarioNode = exportGraph.addResource(getMainNodeURI());
    final Node scenarioTypeNode = exportGraph.addResource(CoreVocabulary.SCENARIO.stringValue());

    // define the properties
    final Node containsTask = exportGraph.addProperty(CoreVocabulary.CONTAINS_TASK.stringValue());

    // add the main node triple
    exportGraph.addEdge(scenarioNode, exportGraph.addProperty(RDF.TYPE), scenarioTypeNode);
    
    // add all the task characterizations
    for (final TaskDescription taskDescription : taskCharacterizations) {
      final Node taskNode = exportGraph.addResource(taskDescription.getMainNodeURI());
      exportGraph.addEdge(scenarioNode, containsTask, taskNode);
      exportGraph.merge(taskDescription.exportToGraph());
    }

    return exportGraph;
  }
}
