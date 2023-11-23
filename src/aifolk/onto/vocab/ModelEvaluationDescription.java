package aifolk.onto.vocab;

import java.util.Date;

import org.semarglproject.vocab.core.RDF;

import fr.inria.corese.core.Graph;
import fr.inria.corese.kgram.api.core.Node;
import fr.inria.corese.sparql.api.IDatatype;
import fr.inria.corese.sparql.datatype.CoreseDateTime;


public class ModelEvaluationDescription extends ExtractableDescription {
  
  private String metricType;
  private double score;
  private Date evaluationDate;
  private DatasetDescription datasetDescription;

  public ModelEvaluationDescription(final Graph modelDescriptionGraph, final String evaluationNodeURI) {
    super(modelDescriptionGraph, evaluationNodeURI);
  }

  // Getters for the properties
  public String getEvaluationNodeURI() {
    return getMainNodeURI();
  }

  public Node getEvaluationNode() {
    return getMainNode();
  }

  public String getMetricType() {
    return metricType;
  }

  public Node getMetricTypeNode() {
    return modelDescriptionGraph.getNode(metricType);
  }

  public double getScore() {
    return score;
  }

  public Date getEvaluationDate() {
    return evaluationDate;
  }

  public DatasetDescription getDatasetDescription() {
    return datasetDescription;
  }

  public String getDatasetNodeURI() {
    if (datasetDescription != null) {
      return datasetDescription.getMainNodeURI();
    }
    return null;
  }

  public Node getDatasetNode() {
    if (datasetDescription != null) {
      return datasetDescription.getMainNode();
    }
    return null;
  }

  @Override
  public void populateDescription(final boolean forceUpdate) {
    // call the super method
    super.populateDescription(forceUpdate);

    // call populate on the dataset description if it exists
    if (datasetDescription != null) {
      datasetDescription.populateDescription(forceUpdate);
    }
  }

  @Override
  protected void extractDescription() {
    // extract the metric type
    final String metricNodeURI = extractSingleObjectURI(modelDescriptionGraph, mainNodeURI, CoreVocabulary.HAS_METRIC.stringValue());
    if (metricNodeURI != null) {
      // get the metric node type URI
      metricType = extractSingleObjectURI(modelDescriptionGraph, metricNodeURI, RDF.TYPE);
    }

    // extract the score
    final IDatatype scoreVal = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, CoreVocabulary.EVAL_SCORE.stringValue());
    if (scoreVal != null) {
      score = scoreVal.doubleValue();
    }

    // extract the evaluation date
    final IDatatype objectRes = extractSingleObjectLiteral(modelDescriptionGraph, mainNodeURI, CoreVocabulary.EVAL_DATE.stringValue());
    if (objectRes != null) {
      final CoreseDateTime evaluationDateVal = (CoreseDateTime) objectRes.cast(fr.inria.corese.sparql.datatype.RDF.xsddateTime);
      evaluationDate = evaluationDateVal.getCalendar().toGregorianCalendar().getTime();
    }

    // extract the dataset description URI if it exists
    final String datasetNodeURI = extractSingleObjectURI(modelDescriptionGraph, mainNodeURI, CoreVocabulary.USES_DATASET.stringValue());
    if (datasetNodeURI != null) {
      datasetDescription = new DatasetDescription(modelDescriptionGraph, datasetNodeURI);
    }
  }
}

