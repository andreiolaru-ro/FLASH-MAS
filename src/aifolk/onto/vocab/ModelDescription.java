package aifolk.onto.vocab;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.kgram.api.core.Node;
import fr.inria.corese.kgram.core.Mappings;
import fr.inria.corese.sparql.api.IDatatype;
import fr.inria.corese.sparql.exceptions.EngineException;

public class ModelDescription extends ExtractableDescription {
    
    // Model Rerenfece URI
    private String modelReferenceURI;

    // List of model evaluations
    private List<EvaluationDescription> modelEvaluations;


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
    public List<EvaluationDescription> getModelEvaluations() {
      // return an empty list if the model description graph is null or if the model node is null or if the model node has no model evaluations
      if (modelDescriptionGraph == null || mainNodeURI == null || modelEvaluations == null) {
        return new ArrayList<>();
      }

      return modelEvaluations;
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


    private String getModelReferenceURI(final Graph modelDescriptionGraph, final String mainNodeURI) {
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


    private List<EvaluationDescription> getModelEvaluations(final Graph modelDescriptionGraph, final String mainNodeURI) {
      final List<EvaluationDescription> modelEvaluations = new ArrayList<>();

      final QueryProcess exec = QueryProcess.create(modelDescriptionGraph);
      final String query = "select ?evaluationNode where { <" + mainNodeURI + "> <" + CoreVocabulary.EVALUATED_BY.stringValue() + "> ?evaluationNode }";

      try {
        final Mappings map = exec.query(query);
        
        if (map.size() == 0) {
          System.err.println("No model evaluations found for the model node " + mainNodeURI + ".");
        }
        else {
          for (final IDatatype nodeVal : map.getValue("?evaluationNode")) {
            final String evaluationNodeURI = nodeVal.getLabel();
            final EvaluationDescription evaluation = new EvaluationDescription(modelDescriptionGraph, evaluationNodeURI);

            modelEvaluations.add(evaluation);
          }
        }

      } catch (final EngineException e) {
        e.printStackTrace();
      }

      return modelEvaluations;
    }

    /** Get the instance of the Model as a graph node in the model description graph.
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
        // Load the corese graph from the file
        final Graph modelDescriptionGraph = Graph.create();
        modelDescriptionGraph.init();
        
        final Load ld = Load.create(modelDescriptionGraph);
        ld.parse(modelDescriptionFilePath);

        // get the model node URI
        final String modelNodeURI = getModelNodeURI(modelDescriptionGraph);

        return new ModelDescription(modelDescriptionGraph, modelNodeURI);
    }

    /**
     * Get the ModelDescription object from a string serialization of the RDF graph.
     * @param serializedModelDescription
     * @return The ModelDescription object.
     * @throws LoadException
     */
    public static ModelDescription getFromString(final String serializedModelDescription) throws LoadException {
        // Load the corese graph from the string serialization
        final Graph modelDescriptionGraph = Graph.create();
        modelDescriptionGraph.init();
        
        // Create a Load instance to parse the string, by creating an InputStream from the string
        final InputStream sis = new ByteArrayInputStream(serializedModelDescription.getBytes());

        final Load ld = Load.create(modelDescriptionGraph);
        ld.parse(sis, Loader.TURTLE_FORMAT);

        // get the model node URI
        final String modelNodeURI = getModelNodeURI(modelDescriptionGraph);

        return new ModelDescription(modelDescriptionGraph, modelNodeURI);
    }

    // make main method for simple testing purposes
    public static void main(final String[] args) throws LoadException {
      final String modelDescriptionFilePath = "/home/alex/work/AI-MAS/projects/2022-AI-Folk/dev/aifolk-project/ontology/aifolk-drivingsegmentation-v1.ttl";
      final ModelDescription md = ModelDescription.getFromFile(modelDescriptionFilePath);

      final String modelNode = md.getModelNodeURI();
      System.out.println("Model node: " + modelNode);

      System.out.println("Model description: ");
      System.out.println(md.getSerializedModelDescription());
    }


   
}
