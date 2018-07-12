package org.opentosca;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionDetails {

  private static Logger logger = LoggerFactory.getLogger(ConnectionDetails.class);

  private static ConnectionDetails INSTANCE = new ConnectionDetails();

  private List<ConnectionDetail> connectionDetails = new ArrayList<>();

  private ConnectionDetails() {
  }

  public static ConnectionDetails getInstance() {
    return INSTANCE;
  }

  public void add(final ConnectionDetail connectionDetail) {
    logger.debug("Adding connections details: {}", connectionDetail.toString());
    this.connectionDetails.add(connectionDetail);
  }

  public List<ConnectionDetail> getConnectionDetails() {
    logger.debug("Returning list of <{}> connection details", this.connectionDetails.size());
    return this.connectionDetails;
  }
}
