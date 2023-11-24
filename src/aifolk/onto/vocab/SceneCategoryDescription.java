package aifolk.onto.vocab;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.logic.RDF;
import fr.inria.corese.kgram.api.core.Node;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;

public class SceneCategoryDescription extends ExtractableDescription {

    protected String sceneTypeURI;

    protected SceneCategoryDescription() {
      super(null, null);
    }

    public SceneCategoryDescription(final Graph modelDescriptionGraph, final String mainNodeURI) {
      super(modelDescriptionGraph, mainNodeURI);
    }
  
    @Override
    protected void extractDescription() {
      // extract the scene type URI
      sceneTypeURI = extractSingleObjectURI(modelDescriptionGraph, mainNodeURI, RDF.TYPE);
    }

    /**
     * Get the URI of the scene type node - e.g. City, Highway, Rural, Parking lot
     * @return the URI of the scene type node
     */
    public String getSceneTypeURI() {
      return sceneTypeURI;
    }

    /**
     * Get the scene type name as the local name of the scene type URI
     * @return the scene type name as the local name of the scene type URI
     */
    public String getSceneTypeName() {
      if (sceneTypeURI == null) {
        return null;
      }
      return SimpleValueFactory.getInstance().createIRI(sceneTypeURI).getLocalName();
    }

    /**
     * Get the scene category node
     * @return the scene category node
     */
    public Node getSceneCategoryNode() {
      return getMainNode();
    }

    /**
     * Get the URI of the scene category node
     * @return the URI of the scene category node
     */
    public String getSceneCategoryURI() {
      return getMainNodeURI();
    }

    /**
     * Get the scene category name as the local name of the scene category URI
     * @return the scene category name as the local name of the scene category URI
     */
    public String getSceneCategoryName() {
      return SimpleValueFactory.getInstance().createIRI(getMainNodeURI()).getLocalName();
    }

    public static class Builder {
        private final List<Consumer<SceneCategoryDescription>> operations;

        private Builder() {
            this.operations = new ArrayList<>();
        }

        public static Builder create(final String mainNodeURI, final String sceneTypeURI) {
            final Builder builder = new Builder();
            builder.operations.add(desc -> desc.setMainNodeURI(mainNodeURI));
            builder.operations.add(desc -> desc.sceneTypeURI = sceneTypeURI);
            return builder;
        }

        public SceneCategoryDescription build() {
            final SceneCategoryDescription desc = new SceneCategoryDescription();
            operations.forEach(op -> op.accept(desc));
            return desc;
        }
    }
}
