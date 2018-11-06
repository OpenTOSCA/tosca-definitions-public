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
public class org_opentosca_nodetypes_BoschOTAManager__groupmanagement_creategroup extends AbstractIAService {

	private final IALogger LOG = new IALogger(org_opentosca_nodetypes_BoschOTAManager__groupmanagement_creategroup.class);
	private final String containerHost = "container";


	@WebMethod
	@SOAPBinding
	@Oneway
	public void createGroup(
		@WebParam(name="nameOfGroup", targetNamespace="http://nodetypeimplementations.opentosca.org/") String nameOfGroup,
		@WebParam(name="deviceName", targetNamespace="http://nodetypeimplementations.opentosca.org/") String deviceName
	) {
		// This HashMap holds the return parameters of this operation.
		final HashMap<String,String> returnParameters = new HashMap<String, String>();
		LOG.debug("Start creating group with name " + nameOfGroup + " and devices " + deviceName);

		Utils utils = new Utils(containerHost);
		String csar = utils.getCSAR();
		LOG.debug("Using CSAR: " + csar);
		String servicetemplate = utils.getServiceTemplate(csar);
		LOG.debug("Usind ServiceTemplate: " + servicetemplate);
		String instanceID = utils.getInstanceID(csar, servicetemplate);
		LOG.debug("Using InstanceID: " + instanceID);
		String nodeTemplate = utils.getNodetemplates(csar, servicetemplate, "IoT-Group");
		LOG.debug("Using NodeTemplate: " + nodeTemplate);


		String[] devices = deviceName.split(" ");
		StringBuilder deviceList = new StringBuilder();

		for (String device : devices) {
			deviceList.append(device);
			deviceList.append(",");
		}

		deviceList.deleteCharAt(deviceList.length()-1);

		if(nameOfGroup.isEmpty()){
			nameOfGroup = "default";
		}
		if(deviceList.toString().isEmpty()){
			deviceList.append("noMembers");
		}

		utils.createInstance(csar, servicetemplate, nodeTemplate, instanceID,
				Arrays.asList("groupName", "deviceList"),
				Arrays.asList(nameOfGroup, deviceList.toString()));

		returnParameters.put("groupName", nameOfGroup);
		returnParameters.put("deviceList", deviceList.toString());
		sendResponse(returnParameters);
		LOG.debug("Finished creating group");
	}
}
