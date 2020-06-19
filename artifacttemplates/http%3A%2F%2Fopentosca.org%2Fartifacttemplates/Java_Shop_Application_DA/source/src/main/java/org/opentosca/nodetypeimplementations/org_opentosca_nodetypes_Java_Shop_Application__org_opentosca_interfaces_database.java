package org.opentosca.nodetypeimplementations;

import java.util.HashMap;
import java.util.Map;
import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import org.opentosca.ConnectionDetail;
import org.opentosca.ConnectionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebService
public class org_opentosca_nodetypes_Java_Shop_Application__org_opentosca_interfaces_database extends
    AbstractService {

  private static Logger logger = LoggerFactory.getLogger(
      org_opentosca_nodetypes_Java_Shop_Application__org_opentosca_interfaces_database.class);

  @WebMethod
  @SOAPBinding
  @Oneway
  public void connectTo(
      @WebParam(name = "VMIP", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String host,
      @WebParam(name = "DBMSPort", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String port,
      @WebParam(name = "DBName", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String name,
      @WebParam(name = "DBUser", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String user,
      @WebParam(name = "DBPassword", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String password
  ) {
    logger.info("BEGIN - connectTo()");
    final Map<String, String> responsePayload = new HashMap<>();
    final ConnectionDetail connectionDetail = new ConnectionDetail(host, port, name, user,
        password);
    logger.info("Connection Details: {}", connectionDetail.toString());
    ConnectionDetails.getInstance().add(connectionDetail);
    responsePayload.put("Result", "SUCCESS");
    this.sendResponse(responsePayload);
    logger.info("END - connectTo()");
  }
}
