package org.opentosca.nodetypeimplementations;

import java.util.HashMap;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlElement;

@WebService
public class org_opentosca_nodetypes_BoschOTAUpdateDevice__org_opentosca_iot_interfaces_devicemanagement extends AbstractIAService {

	@WebMethod
	@SOAPBinding
	@Oneway
	public void uploadBinary(
		@WebParam(name="tenant", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String tenant,
		@WebParam(name="user", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String user,
		@WebParam(name="password", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String password,
		@WebParam(name="host", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String host,
		@WebParam(name="urlToBinary", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String urlToBinary,
		@WebParam(name="distributionSet", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String distributionSet
	) {
		// TODO: Implement your operation here.
	}

	@WebMethod
	@SOAPBinding
	@Oneway
	public void updateDevice(
		@WebParam(name="tenant", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String tenant,
		@WebParam(name="user", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String user,
		@WebParam(name="password", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String password,
		@WebParam(name="host", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String host,
		@WebParam(name="deviceID", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String deviceID,
		@WebParam(name="distributionSet", targetNamespace="http://nodetypeimplementations.opentosca.org/") @XmlElement(required=true) String distributionSet
	) {
		// TODO: Implement your operation here.
	}



}
