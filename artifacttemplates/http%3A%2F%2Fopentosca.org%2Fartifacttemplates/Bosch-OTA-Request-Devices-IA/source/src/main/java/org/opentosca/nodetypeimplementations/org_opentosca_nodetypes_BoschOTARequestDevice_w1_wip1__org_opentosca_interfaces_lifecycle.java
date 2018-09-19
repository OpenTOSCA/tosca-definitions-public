package org.opentosca.nodetypeimplementations;

import java.util.HashMap;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlElement;

@WebService
public class org_opentosca_nodetypes_BoschOTARequestDevice_w1_wip1__org_opentosca_interfaces_lifecycle extends AbstractIAService {

	@WebMethod
	@SOAPBinding
	@Oneway
	public void install(
		@WebParam(name="tenant", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String tenant,
		@WebParam(name="user", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String user,
		@WebParam(name="password", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String password,
		@WebParam(name="host", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String host
	) {
		// This HashMap holds the return parameters of this operation.
		final HashMap<String,String> returnParameters = new HashMap<String, String>();

		// TODO: Implement your operation here.


		// Output Parameter 'deviceID' (required)
		// TODO: Set deviceID parameter here.
		// Do NOT delete the next line of code. Set "" as value if you want to return nothing or an empty result!
		returnParameters.put("deviceID", "TODO");

		// Output Parameter 'deviceName' (required)
		// TODO: Set deviceName parameter here.
		// Do NOT delete the next line of code. Set "" as value if you want to return nothing or an empty result!
		returnParameters.put("deviceName", "TODO");

		// Output Parameter 'distributionSet' (required)
		// TODO: Set distributionSet parameter here.
		// Do NOT delete the next line of code. Set "" as value if you want to return nothing or an empty result!
		returnParameters.put("distributionSet", "TODO");

		sendResponse(returnParameters);
	}



}
