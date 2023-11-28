package aifolk.onto.vocab;

import java.util.ArrayList;
import java.util.List;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.kgram.api.core.Node;
import fr.inria.corese.kgram.core.Mappings;
import fr.inria.corese.sparql.exceptions.EngineException;

public class ModelDescription extends ExtractableDescription {
    
    // Model Rerenfece URI
    private String modelReferenceURI;

    // List of model evaluations
    private List<ModelEvaluationDescription> modelEvaluations;


	  private ModelDescription(final Graph modelDescriptionGraph, final String modelNodeURI) {
      super(modelDescriptionGraph, modelNodeURI);
    }


    // get the Model node URI
    public String getModelNodeURI() {
      return getMainNodeURI();
    }

    // get the Model node
    public Node getModelNode() {
      return getMainNode();
    }

    /**
     * @return String representation as serialized Turtle of the RDF Graph stored in the modelDescriptionGraph object.
     */
    public String getSerializedModelDescription() {
      if (modelDescriptionGraph == null) {
        return "";
      }

      return modelDescriptionGraph.toString();
    }
    
    /**
     * @return The reference URI of the model which can be used to retrieve its properties and invoke it on the ML server.
     */
    public String getModelReferenceURI() {
      return modelReferenceURI;
    }
    
    /**
     * @return The list of model evaluations.
     */
    public List<ModelEvaluationDescription> getModelEvaluations() {
      // return an empty list if the model description graph is null or if the model node is null or if the model node has no model evaluations
      if (modelDescriptionGraph == null || mainNodeURI == null || modelEvaluations == null) {
        return new ArrayList<>();
      }

      return modelEvaluations;
    }

    @Override
    public void populateDescription(final boolean forceUpdate) {
      // first call the super method
      super.populateDescription(forceUpdate);

      // then call populateDescription on the model evaluations
      if (modelEvaluations != null) {
        for (final ModelEvaluationDescription evaluation : modelEvaluations) {
          evaluation.populateDescription(forceUpdate);
        }
      }
    }

    @Override
    protected void extractDescription() {
      if (modelDescriptionGraph == null || mainNodeURI == null) {
        return;
      }
      
      // get the model reference URI
      modelReferenceURI = getModelReferenceURI(modelDescriptionGraph, mainNodeURI);

      // get the model evaluations
      modelEvaluations = getModelEvaluations(modelDescriptionGraph, mainNodeURI);
    }


    private static String getModelReferenceURI(final Graph modelDescriptionGraph, final String mainNodeURI) {
      final QueryProcess exec = QueryProcess.create(modelDescriptionGraph);
      final String query = "select ?modelReferenceURI where { <" + mainNodeURI + "> <" + CoreVocabulary.HAS_REFERENCE_URI.stringValue() + "> ?modelReferenceURI }";

      try {
        final Mappings map = exec.query(query);
        
        if (map.size() == 0) {
          System.err.println("No model reference URI found for the model node " + mainNodeURI + ".");
        }
        else {
          return map.get(0).getValue("?modelReferenceURI").getLabel();
        }

      } catch (final EngineException e) {
        e.printStackTrace();
      }

      return null;
    }


    private static List<ModelEvaluationDescription> getModelEvaluations(final Graph modelDescriptionGraph, final String mainNodeURI) {
      final List<ModelEvaluationDescription> modelEvaluations = new ArrayList<>();
      
      final List<String> modelEvaluationURIs = extractObjectURIs(modelDescriptionGraph, mainNodeURI, CoreVocabulary.EVALUATED_BY.stringValue());
      if (modelEvaluationURIs != null) {
        for (final String modelEvaluationURI : modelEvaluationURIs) {
          final ModelEvaluationDescription evaluation = new ModelEvaluationDescription(modelDescriptionGraph, modelEvaluationURI);
          modelEvaluations.add(evaluation);
        }
      }

      return modelEvaluations;
    }

    /** Get the instance of the Model as a graph node in the model description graph.
     * @param modelDescriptionGraph The model description graph.
     * @return The URI of the model node.
     */
    private static String getModelNodeURI(final Graph modelDescriptionGraph) {
        // // we retrieve the model Node as being the first instance of the Model class that we find in the model description graph
        // final Node modelClassNode = modelDescriptionGraph.getNode(CoreVocabulary.MODEL.stringValue());

        // // get all instances that are of type modeClassNode and retain the first one
        // modelNode = modelDescriptionGraph.getNodes(NodeImpl.create(new CoreseURI(RDF.TYPE)), modelClassNode, 1).iterator().next();
        String modelNodeURI = null;

        final QueryProcess exec = QueryProcess.create(modelDescriptionGraph);
        final String query = "select ?model where { ?model a <" + CoreVocabulary.MODEL.stringValue() + "> }";

        try {
          final Mappings map = exec.query(query);
          
          if (map.size() == 0) {
            System.err.println("No model node found in the model description graph.");
          }
          else {
            modelNodeURI = map.get(0).getValue("?model").getLabel();
          }

        } catch (final EngineException e) {
          e.printStackTrace();
        }
      
        return modelNodeURI;
    }

    /**
     * Get the ModelDescription object from a file.
     * @param modelDescriptionFilePath
     * @return The ModelDescription object.
     * @throws LoadException
     */
    public static ModelDescription getFromFile(final String modelDescriptionFilePath) throws LoadException {
      final Graph modelDescriptionGraph = getGraphFromFile(modelDescriptionFilePath);  
      final String modelNodeURI = getSingleConceptURI(modelDescriptionGraph, CoreVocabulary.MODEL.stringValue());

      return new ModelDescription(modelDescriptionGraph, modelNodeURI);
    }

    /**
     * Get the ModelDescription object from a string serialization of the RDF graph.
     * @param serializedModelDescription
     * @return The ModelDescription object.
     * @throws LoadException
     */
    public static ModelDescription getFromString(final String serializedModelDescription) throws LoadException {
        final Graph modelDescriptionGraph = getGraphFromString(serializedModelDescription);  
      final String modelNodeURI = getSingleConceptURI(modelDescriptionGraph, CoreVocabulary.MODEL.stringValue());

        return new ModelDescription(modelDescriptionGraph, modelNodeURI);
    }

    // make main method for simple testing purposes
    public static void main(final String[] args) throws LoadException {
      final String modelDescriptionFilePath = "/home/alex/work/AI-MAS/projects/2022-AI-Folk/dev/aifolk-project/ontology/aifolk-drivingsegmentation-v1.ttl";
      final ModelDescription md = ModelDescription.getFromFile(modelDescriptionFilePath);
      md.populateDescription(true);

      final String modelNode = md.getModelNodeURI();
      System.out.println("Model node: " + modelNode);

      System.out.println("Model description: ");
      System.out.println(md.getSerializedModelDescription());
    }


}
