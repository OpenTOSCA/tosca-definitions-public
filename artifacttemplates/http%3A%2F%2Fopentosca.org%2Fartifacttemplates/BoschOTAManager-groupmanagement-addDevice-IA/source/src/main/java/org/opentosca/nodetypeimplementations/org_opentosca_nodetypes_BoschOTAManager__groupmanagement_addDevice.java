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
public class org_opentosca_nodetypes_BoschOTAManager__groupmanagement_addDevice extends AbstractIAService {

	private final IALogger LOG = new IALogger(org_opentosca_nodetypes_BoschOTAManager__groupmanagement_addDevice.class);
	private final String containerHost = "container";


	@WebMethod
	@SOAPBinding
	@Oneway
	public void addDeviceToGroup(
		@WebParam(name="deviceName", targetNamespace="http://nodetypeimplementations.opentosca.org/") String deviceName,
		@WebParam(name="nameOfGroup", targetNamespace="http://nodetypeimplementations.opentosca.org/") String nameOfGroup
	) {
		// This HashMap holds the return parameters of this operation.
		final HashMap<String,String> returnParameters = new HashMap<String, String>();
		LOG.debug("Adding" + deviceName + "to" + nameOfGroup);
		Utils utils = new Utils(containerHost);
		String csar = utils.getCSAR();
		LOG.debug("Using CSAR: " + csar);
		String servicetemplate = utils.getServiceTemplate(csar);
		LOG.debug("Usind ServiceTemplate: " + servicetemplate);
		String nodeTemplate = utils.getNodetemplates(csar, servicetemplate, "IoT-Group");
		LOG.debug("Using NodeTemplate: " + nodeTemplate);
		String baseHost = "http://" + utils.getHost() + ":1337/csars/" + csar + "/servicetemplates/" + servicetemplate
				+ "/nodetemplates/" + nodeTemplate + "/instances/";
		LOG.debug("Using " + baseHost + " as URL for updating Instance properties");
		String nodeTemplateID = utils.getInstanceIDbyProperty(baseHost, nameOfGroup);
		LOG.debug("Using NodeTemplateInstanceID: " + nodeTemplateID);

		String xmlProperties = utils.httpRequests(baseHost + nodeTemplateID + "/properties/", "", "GET", "application/xml");
		String existingProperties = utils.getProperties(xmlProperties);

		StringBuilder deviceList = new StringBuilder();
		deviceList.append(existingProperties);
		deviceList.append(",");
		deviceList.append(deviceName);

		List<String> properties = Arrays.asList("groupName", "deviceList");
		List<String> propertiesValue = Arrays.asList(nameOfGroup, deviceList.toString());

		LOG.debug("Setting Properties");
		StringBuilder propValues = new StringBuilder("<Properties>");
		for(int i = 0; i < properties.size(); i++) {
			propValues.append("<").append(properties.get(i)).append(">").append(propertiesValue.get(i)).append("</").append(properties.get(i)).append(">");
		}
		propValues.append("</Properties>");
		utils.httpRequests(baseHost + nodeTemplateID + "/properties/", propValues.toString(), "PUT", "application/xml");

		LOG.debug("Ended adding" + deviceName + "to" + nameOfGroup);
		returnParameters.put("success", "success");
		sendResponse(returnParameters);
	}
}
