package aifolk.onto.vocab;

import java.util.ArrayList;
import java.util.List;

import fr.inria.corese.core.Graph;

public class DatasetDescription extends ExtractableDescription {

  // The domain to which the dataset applies
  private String datasetDomainURI;

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
  public String getDatasetDomainURI() {
    return datasetDomainURI;
  }

  /**
   * Get the list of data context descriptions
   * @return the list of data context descriptions that characterize the dataset
   */
  public List<DataContextDescription> getDataContextDescriptions() {
    return dataContextDescriptions;
  }


  @Override
  public void populateDescription(final boolean forceUpdate) {
    // call the super method
    super.populateDescription(forceUpdate);

    // call populateDescription on the data context descriptions
    if (dataContextDescriptions != null) {
      for (final DataContextDescription dataContextDescription : dataContextDescriptions) {
        dataContextDescription.populateDescription(forceUpdate);
      }
    }
  }

  
  @Override
  protected void extractDescription() {
    // extract the dataset domain URI
    datasetDomainURI = extractSingleObjectURI(modelDescriptionGraph, mainNodeURI, CoreVocabulary.APPLIES_TO_DOMAIN.stringValue());

    // extract the data context descriptions
    final List<String> dataContextURIs = extractObjectURIs(modelDescriptionGraph, mainNodeURI, CoreVocabulary.HAS_DATA_CONTEXT.stringValue());
    if (dataContextURIs != null) {
      dataContextDescriptions = new ArrayList<>();
      for (final String dataContextURI : dataContextURIs) {
        /*
         * TODO: for now we assume we only have driving scene contexts. We need a mechanism to manage the type of the data context.
         * For now, access to the data context details is done by casting the data context description to a driving scene context description.
         */ 
        final DataContextDescription dataContextDescription = new DrivingSceneContextDescription(modelDescriptionGraph, dataContextURI);
        dataContextDescriptions.add(dataContextDescription);
      }
    }
  }
  
}
