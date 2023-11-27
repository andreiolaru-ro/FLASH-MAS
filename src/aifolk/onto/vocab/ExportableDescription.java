package aifolk.onto.vocab;


import java.util.Optional;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.transform.Transformer;

public interface ExportableDescription {
  
  /**
   * Export the description to a Corese Graph object
   * @return the Corese Graph object that represents the description
   */
  public Graph exportToGraph();

  public static Optional<String> graphToString(final Graph graph) {
    final Transformer transformer = Transformer.create(graph, Transformer.TURTLE);
    try {
      return Optional.of(transformer.transform());
    } catch (final Exception e) {
      System.err.println("Error while transforming the graph to a string. Reason: " + e);
    }
    
    return Optional.empty();
  }
}