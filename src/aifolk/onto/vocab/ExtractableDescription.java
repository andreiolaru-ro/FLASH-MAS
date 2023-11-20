package aifolk.onto.vocab;

import fr.inria.corese.core.Graph;
import fr.inria.corese.kgram.api.core.Node;

/**
 * This class represents a description of a model, dataset, or data context. It is used to extract the description from the RDF model.
 * All instances will make reference to a model graph and a main node URI.
 */
public abstract class ExtractableDescription {
  protected Graph modelDescriptionGraph;
  protected String mainNodeURI;
  
  private boolean populated = false;

  /**
   * Constructor
   * @param modelDescriptionGraph - the Corese Graph object that will be used to store the model description
   * @param mainNodeURI - the URI of the main node of the description
   */
  public ExtractableDescription(final Graph modelDescriptionGraph, final String mainNodeURI) {
    this.modelDescriptionGraph = modelDescriptionGraph;
    this.mainNodeURI = mainNodeURI;
  }

  /**
   * Get the Corese Graph object that will be used to store the model description
   * @return the Corese Graph object that will be used to store the model description
   */
  public Graph getModelDescriptionGraph() {
    return modelDescriptionGraph;
  }

  /**
   * Get the URI of the main node of the description
   * @return the URI of the main node of the description
   */
  public String getMainNodeURI() {
    return mainNodeURI;
  }

  /**
   * Get the main node of the description
   * @return the main node of the description
   */
  public Node getMainNode() {
    return modelDescriptionGraph.getNode(mainNodeURI);
  }
   
  
  /**
   * Populate the description from the RDF model
   */
  public void populateDescription(final boolean forceUpdate) {
    // if not populated or force update is true extract the description
    if (forceUpdate || !populated) {
      extractDescription();
      populated = true;
    }
  }

  /**
   * Check if the description is populated
   * @return true if the description is populated, false otherwise
   */
  public boolean isPopulated() {
    return populated;
  }

  /**
   * Extract the description from the RDF model
   */
  protected abstract void extractDescription();
}
