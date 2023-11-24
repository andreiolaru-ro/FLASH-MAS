package aifolk.onto.vocab;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.logic.RDF;
import fr.inria.corese.kgram.api.core.Node;
import fr.inria.corese.sparql.api.IDatatype;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;

/**
 * This class represents a description of an environment condition (e.g. Daylight illumination, Overcast weather). 
 * It is used to extract the description from the RDF model.
 * All instances will make reference to a model graph and a main node URI.
 */
public class EnvironmentConditionDescription extends ExtractableDescription {
  /**
   * The URI of the environment condition type node (e.g. Daylight illumination, Overcast weather) 
   */
  protected String conditionTypeURI;

  /**
   * The percentage of the data context that is represented by this environment condition
   */
  protected double representationPercentage;

  protected boolean isWeatherCondition = false;

  protected boolean isIlluminationCondition = false;

  protected EnvironmentConditionDescription() {
    super(null, null);
  }
  
  /**
   * Create a new environment condition description
   * @param modelDescriptionGraph - the Corese Graph object that contains the model description
   * @param mainNodeURI - the URI of the main node of environment condition description
   */
  public EnvironmentConditionDescription(final Graph modelDescriptionGraph, final String mainNodeURI) {
    super(modelDescriptionGraph, mainNodeURI);

    checkIlluminationCondition();
    checkWeatherCondition();
  }

  /**
   * Check if this environment condition is a weather condition using a property path SPAQRL ASK query
   */
  private void checkWeatherCondition() {
    isWeatherCondition = checkClassType(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.WeatherCondition.stringValue());
  }

  /**
   * Check if this environment condition is an illumination condition using a property path SPAQRL ASK query
   */
  private void checkIlluminationCondition() {
    isIlluminationCondition = checkClassType(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.IlluminationCondition.stringValue());
  }

  /**
   * Get the URI of the environment condition node
   * @return the URI of the environment condition node
   */
  public String getEnvironmentConditionURI() {
    return getMainNodeURI();
  }

  /**
   * Get the environment condition node
   * @return the environment condition node
   */
  public Node getEnvironmentConditionNode() {
    return getMainNode();
  }

  /**
   * Get the URI of the environment condition type node (e.g. Daylight illumination, Overcast weather) 
   * @return the URI of the environment condition type node
   */
  public String getConditionTypeURI() {
    return conditionTypeURI;
  }

  /**
   * Get the environment condition type name (e.g. Daylight illumination, Overcast weather) from its URI. This will be the IRI local name.
   * @return the environment condition type name
   */
  public String getConditionTypeName() {
    return SimpleValueFactory.getInstance().createIRI(conditionTypeURI).getLocalName();
  }

  /**
   * Get the percentage of the data context that is represented by this environment condition
   * @return the percentage of the data context that is represented by this environment condition
   */
  public double getRepresentationPercentage() {
    return representationPercentage;
  }

  /**
   * Check if this environment condition is a weather condition
   * @return true if this environment condition is a weather condition, false otherwise
   */
  public boolean isWeatherCondition() {
    return isWeatherCondition;
  }

  /**
   * Check if this environment condition is an illumination condition
   * @return true if this environment condition is an illumination condition, false otherwise
   */
  public boolean isIlluminationCondition() {
    return isIlluminationCondition;
  }

  @Override
  protected void extractDescription() {
    // extract the condition type URI by looking at the RDF type of the main node
    conditionTypeURI = extractSingleObjectURI(modelDescriptionGraph, mainNodeURI, RDF.TYPE);

    // extract the representation percentage
    final IDatatype representationPercentageVal = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasPercentRepresentation.stringValue());
    if (representationPercentageVal != null) {
      representationPercentage = representationPercentageVal.doubleValue();
    }
  }

  public static class Builder {
        private final List<Consumer<EnvironmentConditionDescription>> operations;

        private Builder() {
            this.operations = new ArrayList<>();
        }

        public static Builder createWeatherBuilder(final String mainNodeURI, final String weatherConditionTypeURI) {
            final Builder builder = new Builder();
            builder.operations.add(desc -> desc.setMainNodeURI(mainNodeURI));
            builder.operations.add(desc -> desc.conditionTypeURI = weatherConditionTypeURI);
            builder.operations.add(desc -> desc.isWeatherCondition = true);
            return builder;
        }

        public static Builder createIlluminationBuilder(final String mainNodeURI, final String illuminationConditionTypeURI) {
            final Builder builder = new Builder();
            builder.operations.add(desc -> desc.setMainNodeURI(mainNodeURI));
            builder.operations.add(desc -> desc.conditionTypeURI = illuminationConditionTypeURI);
            builder.operations.add(desc -> desc.isIlluminationCondition = true);
            return builder;
        }

        public Builder setRepresentationPercentage(final double representationPercentage) {
            operations.add(desc -> desc.representationPercentage = representationPercentage);
            return this;
        }

        public EnvironmentConditionDescription build() {
            final EnvironmentConditionDescription desc = new EnvironmentConditionDescription();
            operations.forEach(op -> op.accept(desc));
            return desc;
        }
    }

  
}
