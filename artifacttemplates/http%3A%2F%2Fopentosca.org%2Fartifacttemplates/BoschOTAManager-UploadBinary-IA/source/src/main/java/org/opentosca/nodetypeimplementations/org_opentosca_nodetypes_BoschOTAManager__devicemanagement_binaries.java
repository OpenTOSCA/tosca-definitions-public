package org.opentosca.nodetypeimplementations;

import java.util.HashMap;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlElement;

@WebService
public class org_opentosca_nodetypes_BoschOTAManager__devicemanagement_binaries extends AbstractIAService {

	@WebMethod
	@SOAPBinding
	@Oneway
	public void uploadBinary(
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



}
