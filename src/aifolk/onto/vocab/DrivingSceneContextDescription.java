package aifolk.onto.vocab;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fr.inria.corese.core.Graph;
import fr.inria.corese.kgram.api.core.Node;
import fr.inria.corese.sparql.api.IDatatype;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;

/**
 * Class representing the description of a driving scene context.
 * It gives details about the environment conditions and scene categories that are represented in the data context, as well as the 
 * content specifics such as min/avg/max number of cross or T intersections, min/avg/max number of lanes, etc. 
 * All instances will make reference to a model graph and a main node URI.
 */
public class DrivingSceneContextDescription extends DataContextDescription {
  
  /**
   * The list of environment conditions that are represented in the data context. Each condition has a value URI and a type URI.
   */
  protected final List<EnvironmentConditionDescription> representedEnvConditions;

  /**
   * The list scene categories that are represented in the data context. Each category has a value URI and a type URI. 
   */
  protected final List<SceneCategoryDescription> representedSceneCategories;


  protected Optional<Integer> minNumTrafficParticipants = Optional.empty();
  protected Optional<Integer> avgNumTrafficParticipants = Optional.empty();
  protected Optional<Integer> maxNumTrafficParticipants = Optional.empty();
  
  protected Optional<Integer> minNumCrossIntersections = Optional.empty();
  protected Optional<Integer> avgNumCrossIntersections = Optional.empty();
  protected Optional<Integer> maxNumCrossIntersections = Optional.empty();
  
  protected Optional<Integer> minNumTIntersections = Optional.empty();
  protected Optional<Integer> avgNumTIntersections = Optional.empty();
  protected Optional<Integer> maxNumTIntersections = Optional.empty();
  
  protected Optional<Integer> minNumPedestrians = Optional.empty();
  protected Optional<Integer> avgNumPedestrians = Optional.empty();
  protected Optional<Integer> maxNumPedestrians = Optional.empty();
  
  protected Optional<Double> trafficParticipantSegmentationMaskRatio = Optional.empty();
  protected Optional<Double> pedestrianSegmentationMaskRatio = Optional.empty();
  protected Optional<Double> parkingLotPercentage = Optional.empty();
  

  // Constructor using Builder pattern
  protected DrivingSceneContextDescription() {
    this(null, null);
  }
  
  /**
   * Create a new driving scene context description
   * @param modelDescriptionGraph - the Corese Graph object that contains the model description
   * @param mainNodeURI - the URI of the main node of driving scene context description
   */
  public DrivingSceneContextDescription(final Graph modelDescriptionGraph, final String mainNodeURI) {
    super(modelDescriptionGraph, mainNodeURI);
    representedEnvConditions = new ArrayList<>();
    representedSceneCategories = new ArrayList<>();
  }

  /**
   * Get the URI of the driving scene context node
   * @return the URI of the data context node
   */
  public String getDrivingSceneContextURI() {
    return getMainNodeURI();
  }

  /**
   * Get the driving scene context node
   * @return the data context node
   */
  public Node getDrivingSceneNode() {
    return getMainNode();
  }

  /**
   * Get the list of environment conditions that are represented in the data context. 
   * @return the list of environment conditions that are represented in the data context.
   */
  public List<EnvironmentConditionDescription> getRepresentedEnvConditions() {
    return representedEnvConditions;
  }

  /**
   * Get the list of scene categories that are represented in the data context. 
   * @return the list of scene categories that are represented in the data context.
   */
  public List<SceneCategoryDescription> getRepresentedSceneCategories() {
    return representedSceneCategories;
  }

  @Override
  protected void extractDescription() {
    // extract the environment conditions
    final List<String> envConditionURIs = extractObjectURIs(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasEnvironmentCondition.stringValue());
    if (envConditionURIs != null) {
      for (final String envConditionURI : envConditionURIs) {
        final EnvironmentConditionDescription envConditionDescription = new EnvironmentConditionDescription(modelDescriptionGraph, envConditionURI);
        representedEnvConditions.add(envConditionDescription);
      }
    }

    // extract the scene categories
    final List<String> sceneCategoryURIs = extractObjectURIs(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasSceneCategory.stringValue());
    if (sceneCategoryURIs != null) {
      for (final String sceneCategoryURI : sceneCategoryURIs) {
        final SceneCategoryDescription sceneCategoryDescription = new SceneCategoryDescription(modelDescriptionGraph, sceneCategoryURI);
        representedSceneCategories.add(sceneCategoryDescription);
      }
    }

    // extract the specifics
    extractSpecifics();
  }

  /**
   * Get the minimum number of traffic participants.
   * @return the minimum number of traffic participants
   */
  public Optional<Integer> getMinNumTrafficParticipants() {
    return minNumTrafficParticipants;
  }

  /**
   * Get the average number of traffic participants.
   * @return the average number of traffic participants
   */
  public Optional<Integer> getAvgNumTrafficParticipants() {
    return avgNumTrafficParticipants;
  }

  /**
   * Get the maximum number of traffic participants.
   * @return the maximum number of traffic participants
   */
  public Optional<Integer> getMaxNumTrafficParticipants() {
    return maxNumTrafficParticipants;
  }

  /**
   * Get the minimum number of cross intersections.
   * @return the minimum number of cross intersections
   */
  public Optional<Integer> getMinNumCrossIntersections() {
    return minNumCrossIntersections;
  }

  /**
   * Get the average number of cross intersections.
   * @return the average number of cross intersections
   */
  public Optional<Integer> getAvgNumCrossIntersections() {
    return avgNumCrossIntersections;
  }

  /**
   * Get the maximum number of cross intersections.
   * @return the maximum number of cross intersections
   */
  public Optional<Integer> getMaxNumCrossIntersections() {
    return maxNumCrossIntersections;
  }

  /**
   * Get the minimum number of T intersections.
   * @return the minimum number of T intersections
   */
  public Optional<Integer> getMinNumTIntersections() {
    return minNumTIntersections;
  }

  /**
   * Get the average number of T intersections.
   * @return the average number of T intersections
   */
  public Optional<Integer> getAvgNumTIntersections() {
    return avgNumTIntersections;
  }

  /**
   * Get the maximum number of T intersections.
   * @return the maximum number of T intersections
   */
  public Optional<Integer> getMaxNumTIntersections() {
    return maxNumTIntersections;
  }

  /**
   * Get the minimum number of pedestrians.
   * @return the minimum number of pedestrians
   */
  public Optional<Integer> getMinNumPedestrians() {
    return minNumPedestrians;
  }

  /**
   * Get the average number of pedestrians.
   * @return the average number of pedestrians
   */
  public Optional<Integer> getAvgNumPedestrians() {
    return avgNumPedestrians;
  }

  /**
   * Get the maximum number of pedestrians.
   * @return the maximum number of pedestrians
   */
  public Optional<Integer> getMaxNumPedestrians() {
    return maxNumPedestrians;
  }

  /**
   * Get the traffic participant segmentation mask ratio.
   * @return the traffic participant segmentation mask ratio
   */
  public Optional<Double> getTrafficParticipantSegmentationMaskRatio() {
    return trafficParticipantSegmentationMaskRatio;
  }

  /**
   * Get the pedestrian segmentation mask ratio.
   * @return the pedestrian segmentation mask ratio
   */
  public Optional<Double> getPedestrianSegmentationMaskRatio() {
    return pedestrianSegmentationMaskRatio;
  }

  /**
   * Get the parking lot percentage.
   * @return the parking lot percentage
   */
  public Optional<Double> getParkingLotPercentage() {
    return parkingLotPercentage;
  }

  /**
   * Extract the specifics of the data context. This includes the min/avg/max number of cross or T intersections, min/avg/max number of lanes, etc.
   */
  private void extractSpecifics() {
    // ========== extract the min/avg/max number of traffic participants ==========
    final IDatatype minNumTrafficPartcipantsVal = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasMinNumTrafficPartcipants.stringValue());
    if (minNumTrafficPartcipantsVal != null) {
       minNumTrafficParticipants = Optional.of(Integer.valueOf(minNumTrafficPartcipantsVal.intValue()));
    }

    final IDatatype avgNumTrafficPartcipantsVal = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasAvgNumTrafficParticipants.stringValue());
    if (avgNumTrafficPartcipantsVal != null) {
      avgNumTrafficParticipants = Optional.of(Integer.valueOf(avgNumTrafficPartcipantsVal.intValue()));
    }

    final IDatatype maxNumTrafficPartcipantsVal = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasMaxNumTrafficParticipants.stringValue());
    if (maxNumTrafficPartcipantsVal != null) {
      maxNumTrafficParticipants = Optional.of(Integer.valueOf(maxNumTrafficPartcipantsVal.intValue()));
    }

    // ==========  extract the min/avg/max number of cross intersections ==========
    final IDatatype minNumCrossIntersectionsVal = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasMinNumCrossIntersections.stringValue());
    if (minNumCrossIntersectionsVal != null) {
      minNumCrossIntersections = Optional.of(Integer.valueOf(minNumCrossIntersectionsVal.intValue()));
    }

    final IDatatype avgNumCrossIntersectionsVal = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasAvgNumCrossIntersections.stringValue());
    if (avgNumCrossIntersectionsVal != null) {
      avgNumCrossIntersections = Optional.of(Integer.valueOf(avgNumCrossIntersectionsVal.intValue()));
    }

    final IDatatype maxNumCrossIntersectionsVal = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasMaxNumCrossIntersections.stringValue());
    if (maxNumCrossIntersectionsVal != null) {
      maxNumCrossIntersections = Optional.of(Integer.valueOf(maxNumCrossIntersectionsVal.intValue()));
    }

    // ==========  extract the min/avg/max number of T intersections ==========
    final IDatatype minNumTIntersectionsVal = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasMinNumTIntersections.stringValue());
    if (minNumTIntersectionsVal != null) {
      minNumTIntersections = Optional.of(Integer.valueOf(minNumTIntersectionsVal.intValue()));
    }

    final IDatatype avgNumTIntersectionsVal = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasAvgNumTIntersections.stringValue());
    if (avgNumTIntersectionsVal != null) {
      avgNumTIntersections = Optional.of(Integer.valueOf(avgNumTIntersectionsVal.intValue()));
    }

    final IDatatype maxNumTIntersectionsVal = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasMaxNumTIntersections.stringValue());
    if (maxNumTIntersectionsVal != null) {
      maxNumTIntersections = Optional.of(Integer.valueOf(maxNumTIntersectionsVal.intValue()));
    }

    // ==========  extract the min/avg/max number of pedestrians ==========
    final IDatatype minNumPedestriansVal = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasMinNumPedestrians.stringValue());
    if (minNumPedestriansVal != null) {
      minNumPedestrians = Optional.of(Integer.valueOf(minNumPedestriansVal.intValue()));
    }

    final IDatatype avgNumPedestriansVal = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasAvgNumPedestrians.stringValue());
    if (avgNumPedestriansVal != null) {
      avgNumPedestrians = Optional.of(Integer.valueOf(avgNumPedestriansVal.intValue()));
    }

    final IDatatype maxNumPedestriansVal = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasMaxNumPedestrians.stringValue());
    if (maxNumPedestriansVal != null) {
      maxNumPedestrians = Optional.of(Integer.valueOf(maxNumPedestriansVal.intValue()));
    }

    // ==========  extract the traffic participant segmentation mask ratio ==========
    final IDatatype trafficParticipantSegmentationMaskRatioVal = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasTrafficParticipantSegmentationMaskRatio.stringValue());
    if (trafficParticipantSegmentationMaskRatioVal != null) {
      trafficParticipantSegmentationMaskRatio = Optional.of(Double.valueOf(trafficParticipantSegmentationMaskRatioVal.doubleValue()));
    }

    // ==========  extract the pedestrian segmentation mask ratio ==========
    final IDatatype pedestrianSegmentationMaskRatioVal = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasPedestrianSegmentationMaskRatio.stringValue());
    if (pedestrianSegmentationMaskRatioVal != null) {
      pedestrianSegmentationMaskRatio = Optional.of(Double.valueOf(pedestrianSegmentationMaskRatioVal.doubleValue()));
    }

    // ==========  extract the parking lot percentage ==========
    final IDatatype parkingLotPercentageVal = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, DrivingSegmentationVocabulary.hasParkingLotPercentage.stringValue());
    if (parkingLotPercentageVal != null) {
      parkingLotPercentage = Optional.of(Double.valueOf(parkingLotPercentageVal.doubleValue()));
    }
  }


  @Override
  public void populateDescription(final boolean forceUpdate) {
    // call the super method
    super.populateDescription(forceUpdate);

    // call populateDescription on the environment conditions
    if (representedEnvConditions != null) {
      for (final EnvironmentConditionDescription envConditionDescription : representedEnvConditions) {
        envConditionDescription.populateDescription(forceUpdate);
      }
    }

    // call populateDescription on the scene categories
    if (representedSceneCategories != null) {
      for (final SceneCategoryDescription sceneCategoryDescription : representedSceneCategories) {
        sceneCategoryDescription.populateDescription(forceUpdate);
      }
    }
  }

  // Builder class
  public static class Builder {
    private final List<Consumer<DrivingSceneContextDescription>> operations;

    private Builder() {
      operations = new ArrayList<>();
    }

    public static Builder create(final String mainNodeURI) {
      final Builder builder = new Builder();
      builder.operations.add(ctx -> ctx.mainNodeURI = mainNodeURI);
      return builder;
    }

    public Builder addRepresentedCondition(final EnvironmentConditionDescription representedEnvCondition) {
      operations.add(ctx -> ctx.representedEnvConditions.add(representedEnvCondition));
      return this;
    }

    public Builder addRepresentedSceneCategory(final SceneCategoryDescription representedSceneCategory) {
      operations.add(ctx -> ctx.representedSceneCategories.add(representedSceneCategory));
      return this;
    }

    public Builder withMinNumCrossIntersections(final int minNumCrossIntersections) {
      operations.add(ctx -> ctx.minNumCrossIntersections = Optional.of(Integer.valueOf(minNumCrossIntersections)));
      return this;
    }

    public Builder withAvgNumCrossIntersections(final int avgNumCrossIntersections) {
      operations.add(ctx -> ctx.avgNumCrossIntersections = Optional.of(Integer.valueOf(avgNumCrossIntersections)));
      return this;
    }

    public Builder withMaxNumCrossIntersections(final int maxNumCrossIntersections) {
      operations.add(ctx -> ctx.maxNumCrossIntersections = Optional.of(Integer.valueOf(maxNumCrossIntersections)));
      return this;
    }

    public Builder withMinNumTIntersections(final int minNumTIntersections) {
      operations.add(ctx -> ctx.minNumTIntersections = Optional.of(Integer.valueOf(minNumTIntersections)));
      return this;
    }

    public Builder withAvgNumTIntersections(final int avgNumTIntersections) {
      operations.add(ctx -> ctx.avgNumTIntersections = Optional.of(Integer.valueOf(avgNumTIntersections)));
      return this;
    }

    public Builder withMaxNumTIntersections(final int maxNumTIntersections) {
      operations.add(ctx -> ctx.maxNumTIntersections = Optional.of(Integer.valueOf(maxNumTIntersections)));
      return this;
    }

    public Builder withMinNumPedestrians(final int minNumPedestrians) {
      operations.add(ctx -> ctx.minNumPedestrians = Optional.of(Integer.valueOf(minNumPedestrians)));
      return this;
    }

    public Builder withAvgNumPedestrians(final int avgNumPedestrians) {
      operations.add(ctx -> ctx.avgNumPedestrians = Optional.of(Integer.valueOf(avgNumPedestrians)));
      return this;
    }

    public Builder withMaxNumPedestrians(final int maxNumPedestrians) {
      operations.add(ctx -> ctx.maxNumPedestrians = Optional.of(Integer.valueOf(maxNumPedestrians)));
      return this;
    }

    public Builder withMinNumTrafficParticipants(final int minNumTrafficParticipants) {
      operations.add(ctx -> ctx.minNumTrafficParticipants = Optional.of(Integer.valueOf(minNumTrafficParticipants)));
      return this;
    }

    public Builder withAvgNumTrafficParticipants(final int avgNumTrafficParticipants) {
      operations.add(ctx -> ctx.avgNumTrafficParticipants = Optional.of(Integer.valueOf(avgNumTrafficParticipants)));
      return this;
    }

    public Builder withMaxNumTrafficParticipants(final int maxNumTrafficParticipants) {
      operations.add(ctx -> ctx.maxNumTrafficParticipants = Optional.of(Integer.valueOf(maxNumTrafficParticipants)));
      return this;
    }

    public Builder withTrafficParticipantSegmentationMaskRatio(final double trafficParticipantSegmentationMaskRatio) {
      operations.add(ctx -> ctx.trafficParticipantSegmentationMaskRatio = Optional.of(Double.valueOf(trafficParticipantSegmentationMaskRatio)));
      return this;
    }

    public Builder withPedestrianSegmentationMaskRatio(final double pedestrianSegmentationMaskRatio) {
      operations.add(ctx -> ctx.pedestrianSegmentationMaskRatio = Optional.of(Double.valueOf(pedestrianSegmentationMaskRatio)));
      return this;
    }

    public Builder withParkingLotPercentage(final double parkingLotPercentage) {
      operations.add(ctx -> ctx.parkingLotPercentage = Optional.of(Double.valueOf(parkingLotPercentage)));
      return this;
    }

    public DrivingSceneContextDescription build() {
      final DrivingSceneContextDescription ctx = new DrivingSceneContextDescription();
      operations.forEach(op -> op.accept(ctx));
      return ctx;
    }
  }
}
