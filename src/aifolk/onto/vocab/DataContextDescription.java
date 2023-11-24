package aifolk.onto.vocab;

import fr.inria.corese.core.Graph;
import fr.inria.corese.kgram.api.core.Node;

/**
 * Abstract class representing the base for description of a data context. 
 * It is used to give dommain-specific details about the data present in a Dataset or a scenario TaskCharacterization.
 * All instances will make reference to a model graph and a main node URI.
 */
public abstract class DataContextDescription extends ExtractableDescription {
  
  protected DataContextDescription() {
    super();
  }

  protected DataContextDescription(final String mainNodeURI) {
    super(mainNodeURI);
  }

  public DataContextDescription(final Graph modelDescriptionGraph, final String mainNodeURI) {
    super(modelDescriptionGraph, mainNodeURI);
  }

  /**
   * Get the URI of the data context node
   * @return the URI of the data context node
   */
  public String getDataContextURI() {
    return getMainNodeURI();
  }

  /**
   * Get the data context node
   * @return the data context node
   */
  public Node getDataContextNode() {
    return getMainNode();
  }

}
