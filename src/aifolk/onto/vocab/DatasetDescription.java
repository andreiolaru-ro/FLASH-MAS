package aifolk.onto.vocab;

import java.util.List;

import fr.inria.corese.core.Graph;

public class DatasetDescription extends ExtractableDescription {

  // The domain to which the dataset applies
  private String datasetDomain;

  private List<DataContextDescription> dataContextDescriptions;

  public DatasetDescription(final Graph modelDescriptionGraph, final String datasetNodeURI) {
    super(modelDescriptionGraph, datasetNodeURI);
  }

  /**
   * Get the URI of the dataset node
   * @return the URI of the dataset node
   */
  public String getDatasetNodeURI() {
    return getMainNodeURI();
  }

  /**
   * Get the dataset domain 
   * @return the domain to which the dataset applies
   */
  public String getDatasetDomain() {
    return datasetDomain;
  }

  /**
   * Get the list of data context descriptions
   * @return the list of data context descriptions that characterize the dataset
   */
  public List<DataContextDescription> getDataContextDescriptions() {
    return dataContextDescriptions;
  }


  @Override
  protected void extractDescription() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'extractDescription'");
  }
  
}
