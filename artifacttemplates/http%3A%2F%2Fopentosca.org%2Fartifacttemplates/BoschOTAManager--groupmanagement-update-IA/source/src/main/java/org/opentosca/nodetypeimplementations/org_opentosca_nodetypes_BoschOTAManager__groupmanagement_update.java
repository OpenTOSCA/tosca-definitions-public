package org.opentosca.nodetypeimplementations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
public class org_opentosca_nodetypes_BoschOTAManager__groupmanagement_update extends AbstractIAService {

	private final IALogger LOG = new IALogger(org_opentosca_nodetypes_BoschOTAManager__groupmanagement_update.class);
	private final String containerHost = "container";

	@WebMethod
	@SOAPBinding
	@Oneway
	public void updateGroup(
		@WebParam(name="distributionSetName", targetNamespace="http://nodetypeimplementations.opentosca.org/") String distributionSetName,
		@WebParam(name="nameOfGroup", targetNamespace="http://nodetypeimplementations.opentosca.org/") String nameOfGroup,
		@WebParam(name="tenant", targetNamespace="http://nodetypeimplementations.opentosca.org/") String tenant,
		@WebParam(name="user", targetNamespace="http://nodetypeimplementations.opentosca.org/") String user,
		@WebParam(name="password", targetNamespace="http://nodetypeimplementations.opentosca.org/") String password,
		@WebParam(name="host", targetNamespace="http://nodetypeimplementations.opentosca.org/") String host
	) {
		// This HashMap holds the return parameters of this operation.
		final HashMap<String,String> returnParameters = new HashMap<String, String>();
		LOG.debug("Starting to update all devices in " + nameOfGroup);
		Utils utils = new Utils(containerHost);
		String credentials = utils.createCredentials(tenant, user, password);
		host = utils.generateHost(host);

		String csar = utils.getCSAR();
		LOG.debug("Using CSAR: " + csar);
		String servicetemplate = utils.getServiceTemplate(csar);
		LOG.debug("Usind ServiceTemplate: " + servicetemplate);
		String nodeTemplate = utils.getNodetemplates(csar, servicetemplate, "IoT-Group");
		LOG.debug("Using NodeTemplate: " + nodeTemplate);
		String baseHost = "http://" + utils.getHost() + ":1337/csars/" + csar + "/servicetemplates/" + servicetemplate
				+ "/nodetemplates/" + nodeTemplate + "/instances/";

		//Get all deviceIDs from the group
		String nodeTemplateID = utils.getInstanceIDbyProperty(baseHost, nameOfGroup, "groupName");
		LOG.debug("Using NodeTemplateInstanceID: " + nodeTemplateID);

		String xmlProperties = utils.httpRequests(baseHost + nodeTemplateID + "/properties/", "", "GET", "application/xml");
		String[] existingProperties = utils.getProperties(xmlProperties).split(",");

		//Update all devices in that list
		for(String device : existingProperties){
			updateDevice(device, distributionSetName, credentials, host, utils, csar, servicetemplate);
		}

		LOG.debug("Finished to update all devices in " + nameOfGroup);
		returnParameters.put("success", "success");
		sendResponse(returnParameters);
	}

	private void updateDevice(String deviceName, String distributionSetName, String credentials, String host, Utils utils,
							  String csar, String servicetemplate){

		AddDStoTarget addDStoTarget = new AddDStoTarget(deviceName, distributionSetName, credentials, host);

		//creating parameters for the instance creation in OpenTOSCA
		String nodeTemplate = utils.getNodetemplates(csar, servicetemplate, "Device");
		LOG.debug("Using NodeTemplate: " + nodeTemplate);
		String baseHost = "http://" + utils.getHost() + ":1337/csars/" + csar + "/servicetemplates/" + servicetemplate
				+ "/nodetemplates/" + nodeTemplate + "/instances/";
		LOG.debug("Using " + "http://" + utils.getHost() + ":1337/csars/" + csar + "/servicetemplates/" + servicetemplate
				+ "/nodetemplates/" + nodeTemplate + "/instances/" + " as URL for updating Instance properties");
		String nodeTemplateID = utils.getInstanceIDbyProperty(baseHost, deviceName, "deviceID");
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
	}
}
