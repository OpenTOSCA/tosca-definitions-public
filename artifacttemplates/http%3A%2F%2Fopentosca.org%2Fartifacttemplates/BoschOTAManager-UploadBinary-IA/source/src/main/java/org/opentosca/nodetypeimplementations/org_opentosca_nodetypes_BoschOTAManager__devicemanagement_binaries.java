package org.opentosca.nodetypeimplementations;

<<<<<<< HEAD
import java.util.Collections;
=======
>>>>>>> 1daa924ff2006fd378696a5c389deb100ce8969a
import java.util.HashMap;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlElement;

<<<<<<< HEAD
/**
 * Implementing the devicemanagement operations for Rollout/HawkBit in OpenTOSCA
 * Offers methods upload new update binaries and add them to existing devices
 */
@WebService
public class org_opentosca_nodetypes_BoschOTAManager__devicemanagement_binaries extends AbstractIAService {

	private final IALogger LOG = new IALogger(org_opentosca_nodetypes_BoschOTAManager__devicemanagement_binaries.class);
	private final String containerHost = "container";

	/**
	 * Method to create a new software module, add a binary to it and add the software module to a new distribution set
	 * @param tenant the tenant of Rollout/HawkBit
	 * @param user the user of Rollout/HawkBit
	 * @param password the password of Rollout/HawkBit
	 * @param host the URL of Rollout/HawkBit
	 * @param distributionSetName the name of the new SoftwareModule and Distribution Set
	 * @param urlToBinary the URL to the File containing the Update we want to upload in Rollout/Hawkbit
	 */
=======
@WebService
public class org_opentosca_nodetypes_BoschOTAManager__devicemanagement_binaries extends AbstractIAService {

>>>>>>> 1daa924ff2006fd378696a5c389deb100ce8969a
	@WebMethod
	@SOAPBinding
	@Oneway
	public void uploadBinary(
<<<<<<< HEAD
		@WebParam(name="tenant", targetNamespace="http://nodetypeimplementations.opentosca.org/") String tenant,
		@WebParam(name="user", targetNamespace="http://nodetypeimplementations.opentosca.org/") String user,
		@WebParam(name="password", targetNamespace="http://nodetypeimplementations.opentosca.org/") String password,
		@WebParam(name="host", targetNamespace="http://nodetypeimplementations.opentosca.org/") String host,
		@WebParam(name="urlToBinary", targetNamespace="http://nodetypeimplementations.opentosca.org/") String urlToBinary,
		@WebParam(name="distributionSetName", targetNamespace="http://nodetypeimplementations.opentosca.org/") String distributionSetName
	) {
		final HashMap<String, String> returnParameters = new HashMap<>();
		LOG.debug("Starting uploadBinary");
		Utils utils = new Utils(containerHost);
		//http basic auth
		String credentials = utils.createCredentials(tenant,user, password);
		host = utils.generateHost(host);
		LOG.debug("Using Host: " + host);
		urlToBinary = utils.generateHost(urlToBinary);
		LOG.debug("Using FileServer: " + urlToBinary);

		CreateSMaddBinary upload = new CreateSMaddBinary(credentials, host, urlToBinary, distributionSetName);

		if(upload.getSoftwareModuleID() != null) {
			CreateDSaddSM creation = new CreateDSaddSM(upload.getSoftwareModuleID(), credentials, host, distributionSetName);
			//creating parameters for the instance creation in OpenTOSCA
			String csar = utils.getCSAR();
			LOG.debug("Using CSAR: " + csar);
			String servicetemplate = utils.getServiceTemplate(csar);
			LOG.debug("Using ServiceTemplate: " + servicetemplate);
			String instanceID = utils.getInstanceID(csar, servicetemplate);
			LOG.debug("Using InstanceID: " + instanceID);

			String nodetemplates = utils.getNodetemplates(csar, servicetemplate, "Distribution");
			utils.createInstance(csar, servicetemplate, nodetemplates, instanceID,
					Collections.singletonList("distributionSet"), Collections.singletonList(Integer.toString(creation.getDistributionSetID())));
		}
		LOG.debug("Ended uploadBinary");
		returnParameters.put("success", "success");
		sendResponse(returnParameters);
	}
=======
		@WebParam(name="tenant", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String tenant,
		@WebParam(name="user", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String user,
		@WebParam(name="password", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String password,
		@WebParam(name="host", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String host,
		@WebParam(name="urlToBinary", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String urlToBinary,
		@WebParam(name="DistributionSetName", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String DistributionSetName
	) {
		// This HashMap holds the return parameters of this operation.
		final HashMap<String,String> returnParameters = new HashMap<String, String>();

		// TODO: Implement your operation here.


		// Output Parameter 'sucess' (optional)
		// TODO: Set sucess parameter here.
		// Do NOT delete the next line of code. Set "" as value if you want to return nothing or an empty result!
		returnParameters.put("sucess", "TODO");

		sendResponse(returnParameters);
	}



>>>>>>> 1daa924ff2006fd378696a5c389deb100ce8969a
}
