package aifolk.onto.vocab;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class DrivingSegmentationVocabulary {

    // Define a namespace for the ontology
    private static final String NAMESPACE = "http://aimas.cs.pub.ro/ai-folk/ontology/drivingSegmentation#";

    // Define instances of ValueFactory and IRIs for each term
    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    // Object Properties
    public static final IRI hasEnvironmentCondition = createIRI("hasEnvironmentCondition");
    public static final IRI hasSceneCategory = createIRI("hasSceneCategory");

    // Data Properties
    public static final IRI hasAbsentLaneMarkingsPercentage = createIRI("hasAbsentLaneMarkingsPercentage");
    public static final IRI hasAvgNumCrossIntersections = createIRI("hasAvgNumCrossIntersections");
    public static final IRI hasAvgNumPedestrians = createIRI("hasAvgNumPedestrians");
    public static final IRI hasAvgNumTIntersections = createIRI("hasAvgNumTIntersections");
    public static final IRI hasAvgNumTrafficParticipants = createIRI("hasAvgNumTrafficParticipants");
    public static final IRI hasMaxNumCrossIntersections = createIRI("hasMaxNumCrossIntersections");
    public static final IRI hasMaxNumPedestrians = createIRI("hasMaxNumPedestrians");
    public static final IRI hasMaxNumTIntersections = createIRI("hasMaxNumTIntersections");
    public static final IRI hasMaxNumTrafficParticipants = createIRI("hasMaxNumTrafficParticipants");
    public static final IRI hasMinNumCrossIntersections = createIRI("hasMinNumCrossIntersections");
    public static final IRI hasMinNumPedestrians = createIRI("hasMinNumPedestrians");
    public static final IRI hasMinNumTIntersections = createIRI("hasMinNumTIntersections");
    public static final IRI hasMinNumTrafficPartcipants = createIRI("hasMinNumTrafficPartcipants");
    public static final IRI hasParkingLotPercentage = createIRI("hasParkingLotPercentage");
    public static final IRI hasPedestrianSegmentationMaskRatio = createIRI("hasPedestrianSegmentationMaskRatio");
    public static final IRI hasPercentRepresentation = createIRI("hasPercentRepresentation");
    public static final IRI hasRoadSegmentationMaskRatio = createIRI("hasRoadSegmentationMaskRatio");
    public static final IRI hasTrafficParticipantSegmentationMaskRatio = createIRI("hasTrafficParticipantSegmentationMaskRatio");

    // Classes
    public static final IRI City = createIRI("City");
    public static final IRI DawnCondition = createIRI("DawnCondition");
    public static final IRI DaylightCondition = createIRI("DaylightCondition");
    public static final IRI DrivingSceneCategory = createIRI("DrivingSceneCategory");
    public static final IRI DrivingSceneContext = createIRI("DrivingSceneContext");
    public static final IRI DuskCondition = createIRI("DuskCondition");
    public static final IRI EnvironmentCondition = createIRI("EnvironmentCondition");
    public static final IRI Highway = createIRI("Highway");
    public static final IRI IlluminationCondition = createIRI("IlluminationCondition");
    public static final IRI MeanIoU = createIRI("MeanIoU");
    public static final IRI NightCondition = createIRI("NightCondition");
    public static final IRI OvercastCondition = createIRI("OvercastCondition");
    public static final IRI Parking = createIRI("Parking");
    public static final IRI RainyCondition = createIRI("RainyCondition");
    public static final IRI Rural = createIRI("Rural");
    public static final IRI SunnyCondition = createIRI("SunnyCondition");
    public static final IRI WeatherCondition = createIRI("WeatherCondition");

    // Helper method to create an IRI from a local name
    private static IRI createIRI(final String localName) {
        return vf.createIRI(NAMESPACE, localName);
    }
}
