package org.opentosca.nodetypeimplementations;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.util.*;


/**
 * Implementing the devicemanagement operations for Rollout/HawkBit in OpenTOSCA
 * Offers methods to add them to existing devices
 */
@WebService
public class org_opentosca_nodetypes_BoschOTAUpdateDevice__org_opentosca_iot_interfaces_devicemanagement extends AbstractIAService {

	private final IALogger LOG = new IALogger(org_opentosca_nodetypes_BoschOTAUpdateDevice__org_opentosca_iot_interfaces_devicemanagement.class);
	private final String containerHost = "container";

	/**
	 * Method to update a given device with a given distribution set
	 * @param tenant the tenant of Rollout/HawkBit
	 * @param user the user of Rollout/HawkBit
	 * @param password the password of Rollout/HawkBit
	 * @param host the URL of Rollout/HawkBit
	 * @param deviceName the Name of the device to update
	 * @param distributionSetName the name of the new SoftwareModule and Distribution Set with the update
	 */
	@WebMethod
	@SOAPBinding
	@Oneway
	public void updateDevice(
		@WebParam(name="tenant", targetNamespace="http://nodetypeimplementations.opentosca.org/") String tenant,
		@WebParam(name="user", targetNamespace="http://nodetypeimplementations.opentosca.org/") String user,
		@WebParam(name="password", targetNamespace="http://nodetypeimplementations.opentosca.org/") String password,
		@WebParam(name="host", targetNamespace="http://nodetypeimplementations.opentosca.org/") String host,
		@WebParam(name="deviceName", targetNamespace="http://nodetypeimplementations.opentosca.org/") String deviceName,
		@WebParam(name="distributionSetName", targetNamespace="http://nodetypeimplementations.opentosca.org/") String distributionSetName
	) {
		final HashMap<String, String> returnParameters = new HashMap<>();
		LOG.debug("Starting updateDevice");
		Utils utils = new Utils(containerHost);
		String credentials = utils.createCredentials(tenant,user, password);
		host = utils.generateHost(host);

		AddDStoTarget addDStoTarget = new AddDStoTarget(deviceName, distributionSetName, credentials, host);

		//creating parameters for the instance creation in OpenTOSCA
		String csar = utils.getCSAR();
		LOG.debug("Using CSAR: " + csar);
		String servicetemplate = utils.getServiceTemplate(csar);
		LOG.debug("Usind ServiceTemplate: " + servicetemplate);
		String nodeTemplate = utils.getNodetemplates(csar, servicetemplate, "Device");
		LOG.debug("Using NodeTemplate: " + nodeTemplate);
		String baseHost = "http://" + utils.getHost() + ":1337/csars/" + csar + "/servicetemplates/" + servicetemplate
				+ "/nodetemplates/" + nodeTemplate + "/instances/";
		LOG.debug("Using " + "http://" + utils.getHost() + ":1337/csars/" + csar + "/servicetemplates/" + servicetemplate
				+ "/nodetemplates/" + nodeTemplate + "/instances/" + " as URL for updating Instance properties");
		String nodeTemplateID = utils.getInstanceIDbyProperty(baseHost, deviceName);
		LOG.debug("Using NodeTemplateInstanceID: " + nodeTemplateID);

		List<String> properties = Arrays.asList("deviceID", "assignedDS");
		List<String> propertiesValue = Arrays.asList(deviceName, distributionSetName);

		LOG.debug("Setting Properties");
		StringBuilder propValues = new StringBuilder("<Properties>");
		for(int i = 0; i < properties.size(); i++) {
			propValues.append("<").append(properties.get(i)).append(">").append(propertiesValue.get(i)).append("</").append(properties.get(i)).append(">");
		}
		propValues.append("</Properties>");
		utils.httpRequests(baseHost + nodeTemplateID + "/properties/", propValues.toString(), "PUT", "application/xml");

		LOG.debug("Ended updateDevice");
		returnParameters.put("success", "success");
		sendResponse(returnParameters);
	}
}
