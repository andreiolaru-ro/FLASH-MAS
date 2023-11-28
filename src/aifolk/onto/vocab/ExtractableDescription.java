package aifolk.onto.vocab;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.logic.RDF;
import fr.inria.corese.core.logic.RDFS;
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
   * Constructor - used to create a description that will be populated later by a Builder
   */
  public ExtractableDescription() {
  }

  /**
   * Constructor used to create a description that will be populated later by a Builder
   * @param mainNodeURI - the URI of the main node of the description
   */
  public ExtractableDescription(final String mainNodeURI) {
    this.mainNodeURI = mainNodeURI;
  }

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

  
  // ================================== STATIC HELPER METHODS ==================================
  /**
   * Get the Graph of AI Folk concepts from a file.
   * @param filePath - the path to the file containing the serialized model in Turtle format
   * @return The Graph object.
   * @throws LoadException
   */
  public static Graph getGraphFromFile(final String filePath) throws LoadException {
    // Load the corese graph from the file
    final Graph modelDescriptionGraph = Graph.create();
    modelDescriptionGraph.init();
    
    final Load ld = Load.create(modelDescriptionGraph);
    ld.parse(filePath);

    return modelDescriptionGraph;
  }

  /**
   * Get the Graph of AI Folk concepts from a string serialization.
   * @param serializedGraph - the string serialization of the model in Turtle format 
   * @return The Graph object.
   * @throws LoadException
   */
  public static Graph getGraphFromString(final String serializedGraph) throws LoadException {
    // Load the corese graph from the string serialization
    final Graph modelDescriptionGraph = Graph.create();
    modelDescriptionGraph.init();
    
    // Create a Load instance to parse the string, by creating an InputStream from the string
    final InputStream sis = new ByteArrayInputStream(serializedGraph.getBytes());

    final Load ld = Load.create(modelDescriptionGraph);
    ld.parse(sis, Loader.TURTLE_FORMAT);

    return modelDescriptionGraph;
  }

  public static List<String> getAllConceptInstanceURIs(final Graph descriptionGraph, final String conceptTypeURI) {
    final QueryProcess exec = QueryProcess.create(descriptionGraph);
    final String query = "SELECT ?concept WHERE { ?concept <" + RDF.TYPE + "> <" + conceptTypeURI + "> }";

    try {
      final Mappings map = exec.query(query);
      
      if (map.size() == 0) {
        System.err.println("No concept instance found in graph " + descriptionGraph.getName() + " for concept type " + conceptTypeURI + ".");
      }
      else {
        final List<String> conceptURIs = new ArrayList<>();
        for (final Mapping m: map) {
          conceptURIs.add(m.getValue("?concept").getLabel());
        }

        return conceptURIs;
      }

    } catch (final EngineException e) {
      System.err.println("Error while executing query " + query + " on graph " + descriptionGraph.getName() + ".");
      System.err.println(e.getMessage());
    }

    return null;
  }

  public static String getSingleConceptURI(final Graph descriptionGraph, final String conceptTypeURI) {
    final List<String> conceptURIs = getAllConceptInstanceURIs(descriptionGraph, conceptTypeURI);
    if (conceptURIs != null) {
      if (conceptURIs.size() >= 1) {
        return conceptURIs.get(0);
      }
      else {
        System.err.println("More than one concept instance found in graph " + descriptionGraph.getName() + " for concept type " + conceptTypeURI + ".");
      }
    }
    return null;
  }
  


  public static IDatatype extractSingleObject(final Graph modelDescriptionGraph, final String subjectURI, final String propertyURI) {
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
      System.err.println("No literal found in graph " + modelDescriptionGraph.getName() + " for subject " + subjectURI + " and property " + propertyURI + ".");
      return null;
    }
    
    if (object.isLiteral()) {
      return object;
    }
    
    System.err.println("Object " + object.toString() + " found in graph " + modelDescriptionGraph.getName() + " for subject " + subjectURI + " and property " + propertyURI + " is not a literal.");
    return null;
  }
  
  public static List<IDatatype> extractObjects(final Graph modelDescriptionGraph, final String subjectURI, final String propertyURI) {
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

        return objects;
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

  /**
   * Check if the main node has a given type (which may be a superclass of the immediate type) using a property path SPAQRL ASK query
   * @param modelDescriptionGraph - the Corese Graph object that contains the model description
   * @param mainNodeURI - the URI of the main node for which the type is checked
   * @param superClassURI - the URI of the type that is checked
   * @return true if the main node has the given type, false otherwise
   */
  public static boolean checkClassType(final Graph modelDescriptionGraph, final String mainNodeURI, final String superClassURI) {
    final String propertyPath = "<" + RDF.TYPE + ">" + "/" + "<" + RDFS.SUBCLASSOF + ">" + "*";
    final String query = "ASK WHERE { <" + mainNodeURI + "> " + propertyPath + " <" + superClassURI + "> }";
    final QueryProcess exec = QueryProcess.create(modelDescriptionGraph);
    
    try {
      final Mappings m = exec.query(query);
      return !m.isEmpty();
    } catch (final Exception e) {
      System.err.println("Error while executing the query: " + query + ". Reason: " + e);
    
    }

    return false;
  }
  
}
