package aifolk.onto.vocab;

import java.util.ArrayList;
import java.util.List;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.kgram.api.core.Node;
import fr.inria.corese.kgram.core.Mapping;
import fr.inria.corese.kgram.core.Mappings;
import fr.inria.corese.sparql.api.IDatatype;
import fr.inria.corese.sparql.exceptions.EngineException;

/**
 * This class represents a description of a model, dataset, or data context. It is used to extract the description from the RDF model.
 * All instances will make reference to a model graph and a main node URI.
 */
public abstract class ExtractableDescription {
  protected Graph modelDescriptionGraph;
  protected String mainNodeURI;
  
  protected boolean populated = false;

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

  /*
   * Setter for the Corese Graph object that will be used to store the model description
   */
  public void setModelDescriptionGraph(final Graph modelDescriptionGraph) {
    this.modelDescriptionGraph = modelDescriptionGraph;
  }

  /**
   * Get the URI of the main node of the description
   * @return the URI of the main node of the description
   */
  public String getMainNodeURI() {
    return mainNodeURI;
  }

  /**
   * Set the URI of the main node of the description
   */
  public void setMainNodeURI(final String mainNodeURI) {
    this.mainNodeURI = mainNodeURI;
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

  private static IDatatype extractSingleObject(final Graph modelDescriptionGraph, final String subjectURI, final String propertyURI) {
    final QueryProcess exec = QueryProcess.create(modelDescriptionGraph);
    final String query = "SELECT ?object WHERE { <" + subjectURI + "> <" + propertyURI + "> ?object }";

    try {
      final Mappings map = exec.query(query);
      
      if (map.size() == 0) {
        System.err.println("No object found in graph " + modelDescriptionGraph.getName() + " for subject " + subjectURI + " and property " + propertyURI + ".");
      }
      else {
        return map.get(0).getValue("?object");
      }

    } catch (final EngineException e) {
      System.err.println("Error while executing query " + query + " on graph " + modelDescriptionGraph.getName() + ".");
      System.err.println(e.getMessage());
    }

    return null;
  }

  public static String extractSingleObjectURI(final Graph modelDescriptionGraph, final String subjectURI, final String propertyURI) {
    final IDatatype object = extractSingleObject(modelDescriptionGraph, subjectURI, propertyURI);
    if (object != null) {
      return object.getLabel();
    }
    return null;
  }


  public static IDatatype extractSingleObjectLiteral(final Graph modelDescriptionGraph, final String subjectURI, final String propertyURI) {
    final IDatatype object = extractSingleObject(modelDescriptionGraph, subjectURI, propertyURI);
    if (object == null) {
      return null;
    }
    
    if (object.isLiteral()) {
      return object;
    }
    
    System.err.println("Object " + object.toString() + " found in graph " + modelDescriptionGraph.getName() + " for subject " + subjectURI + " and property " + propertyURI + " is not a literal.");
    return null;
  }
  
  private static List<IDatatype> extractObjects(final Graph modelDescriptionGraph, final String subjectURI, final String propertyURI) {
    final QueryProcess exec = QueryProcess.create(modelDescriptionGraph);
    final String query = "SELECT ?object WHERE { <" + subjectURI + "> <" + propertyURI + "> ?object }";

    try {
      final Mappings mappings = exec.query(query);
      
      if (mappings.size() == 0) {
        System.err.println("No objects found in graph " + modelDescriptionGraph.getName() + " for subject " + subjectURI + " and property " + propertyURI + ".");
      }
      else {
        final List<IDatatype> objects = new ArrayList<>();
        for (final Mapping m: mappings) {
          objects.add(m.getValue("?object"));
        }
      }

    } catch (final EngineException e) {
      System.err.println("Error while executing query " + query + " on graph " + modelDescriptionGraph.getName() + ".");
      System.err.println(e.getMessage());
    }

    return null;
  }

  public static List<String> extractObjectURIs(final Graph modelDescriptionGraph, final String subjectURI, final String propertyURI) {
    final List<IDatatype> objects = extractObjects(modelDescriptionGraph, subjectURI, propertyURI);
    if (objects != null) {
      final List<String> objectURIs = new ArrayList<>();
      for (final IDatatype object: objects) {
        objectURIs.add(object.getLabel());
      }
      return objectURIs;
    }
    
    return null;
  }
  
}
