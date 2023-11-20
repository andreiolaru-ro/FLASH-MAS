package aifolk.onto.vocab;

import java.util.Date;

import fr.inria.corese.core.Graph;
import fr.inria.corese.kgram.api.core.Node;


public class EvaluationDescription extends ExtractableDescription {
  
  private String metricType;
  private double score;
  private Date evaluationDate;
  private DatasetDescription datasetDescription;

  public EvaluationDescription(final Graph modelDescriptionGraph, final String evaluationNodeURI) {
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
  protected void extractDescription() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'extractDescription'");
  }
}

