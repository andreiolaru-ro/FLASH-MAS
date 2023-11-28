package aifolk.onto.vocab;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class CoreVocabulary {

    private static final SimpleValueFactory factory = SimpleValueFactory.getInstance();

    // Namespaces
    public static final String CORE_NAMESPACE = "http://aimas.cs.pub.ro/ai-folk/ontology/core#";
    public static final String ANNETTO_NAMESPACE = "http://w3id.org/annett-o/";

    // Classes
    public static final IRI APPLICATION_DOMAIN = createIRI("ApplicationDomain");
    public static final IRI BUILDINGS = createIRI("Buildings");
    public static final IRI DATA_CONTENT = createIRI("DataContent");
    public static final IRI DATA_CONTEXT = createIRI("DataContext");
    public static final IRI HANDWRITTEN_DIGITS = createIRI("HandWrittenDigits");
    public static final IRI INSTANCE_SEGMENTATION = createIRI("InstanceSegmentation");
    public static final IRI LETTERS_DIGITS = createIRI("LettersDigits");
    public static final IRI MODEL = createIRI("Model");
    public static final IRI MODEL_EVALUATION = createIRI("ModelEvaluation");
    public static final IRI NATURE = createIRI("Nature");
    public static final IRI NORMALIZATION = createIRI("Normalization");
    public static final IRI NUMERICAL_DIGITS = createIRI("NumericalDigits");
    public static final IRI OBJECT_DETECTION = createIRI("ObjectDetection");
    public static final IRI PEOPLE = createIRI("People");
    public static final IRI PREPROCESSING = createIRI("Preprocessing");
    public static final IRI ROADS = createIRI("Roads");
    public static final IRI SCENARIO = createIRI("Scenario");
    public static final IRI SEGMENTATION = createIRI("Segmentation");
    public static final IRI SEMANTIC_SEGMENTATION = createIRI("SemanticSegmentation");
    public static final IRI TRAINING_PROCEDURE = createIRI("TrainingProcedure");
    public static final IRI URBAN = createIRI("Urban");
    public static final IRI VEHICLES = createIRI("Vehicles");

    // Object Properties
    public static final IRI APPLIES_TO_DOMAIN = createIRI("appliesToDomain");
    public static final IRI CONTAINS_TASK = createIRI("containsTask");
    public static final IRI EVALUATED_BY = createIRI("evaluatedBy");
    public static final IRI HAS_DATA_CONTENT = createIRI("hasDataContent");
    public static final IRI HAS_DATA_CONTEXT = createIRI("hasDataContext");
    public static final IRI HAS_DOMAIN = createIRI("hasDomain");
    public static final IRI HAS_EVALUATION_RESULT = createIRI("hasEvaluationResult");
    public static final IRI USES_DATASET = createIRI("usesDataset");

    // Data Properties
    public static final IRI BATCH_SIZE = createIRI("batch_size");
    public static final IRI HAS_REFERENCE_URI = createIRI("hasReferenceURI");
    public static final IRI LEARNING_RATE_DECAY = createIRI("learning_rate_decay");
    public static final IRI LEARNING_RATE_DECAY_EPOCHS = createIRI("learning_rate_decay_epochs");
    public static final IRI NUMBER_OF_EPOCHS = createIRI("number_of_epochs");
    // ... (add other data properties)

    // Annetto Namespace
    public static final IRI TASK_CHARACTERIZATION = createIRI(ANNETTO_NAMESPACE, "TaskCharacterization");
    public static final IRI DERIVED_FROM = createIRI(ANNETTO_NAMESPACE, "derivedFrom");
    public static final IRI DERIVED_TEST_DATASET_FROM = createIRI(ANNETTO_NAMESPACE, "derivedTestDatasetFrom");
    public static final IRI DERIVED_TRAINING_DATASET_FROM = createIRI(ANNETTO_NAMESPACE, "derivedTrainingDatasetFrom");
    public static final IRI HAS_METRIC = createIRI(ANNETTO_NAMESPACE, "hasMetric");
    public static final IRI EVAL_SCORE = createIRI(ANNETTO_NAMESPACE, "eval_score");
    public static final IRI EVAL_DATE = createIRI(ANNETTO_NAMESPACE, "eval_date");
    

    private static IRI createIRI(final String localName) {
        return factory.createIRI(CORE_NAMESPACE, localName);
    }

    private static IRI createIRI(final String namespace, final String localName) {
        return factory.createIRI(namespace, localName);
    }
}

