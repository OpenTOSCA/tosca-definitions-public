package org.opentosca.NodeTypes;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlElement;

import org.apache.commons.logging.impl.Log4JLogger;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.client.IOSClientBuilder.V2;
import org.openstack4j.api.client.IOSClientBuilder.V3;
import org.openstack4j.api.types.Facing;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.FloatingIP;
import org.openstack4j.model.compute.Image;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.openstack.OSFactory;

@WebService(targetNamespace = "http://implementationartifacts.opentosca.org/")
public class org_opentosca_NodeTypes_OpenStack extends AbstractIAService {	

	@WebMethod
	@SOAPBinding
	@Oneway
	public void createVM(
			@WebParam(name = "HypervisorEndpoint", targetNamespace = "http://implementationartifacts.opentosca.org/") String HypervisorEndpoint,
			@WebParam(name = "HypervisorUserName", targetNamespace = "http://implementationartifacts.opentosca.org/") String HypervisorUserName,
			@WebParam(name = "HypervisorUserPassword", targetNamespace = "http://implementationartifacts.opentosca.org/") String HypervisorUserPassword,
			@WebParam(name = "HypervisorTenantID", targetNamespace = "http://implementationartifacts.opentosca.org/") String HypervisorTenantID,
			@WebParam(name = "VMType", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMType,
			@WebParam(name = "VMImageID", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMImageID,
			@WebParam(name = "VMUserName", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMUserName,
			@WebParam(name = "VMUserPassword", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMUserPassword,
			@WebParam(name = "VMPrivateKey", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMPrivateKey,
			@WebParam(name = "VMPublicKey", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMPublicKey,
			@WebParam(name = "VMKeyPairName", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMKeyPairName,
			@WebParam(name = "VMSecurityGroup", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMSecurityGroup) {

		final HashMap<String, String> returnParameters = new HashMap<String, String>();

		// we agreed in the IA is knowing the security group
		String SecurityGroup = "default";

		if (VMSecurityGroup != null && !VMSecurityGroup.isEmpty()) {
			SecurityGroup = VMSecurityGroup;
		}

		// User Data is not included in the signature, if can be set like this
		// String userData = "#!/bin/bash\nwget -qO-
		// http://install.opentosca.de/installOpenStack | sudo su - ubuntu";
		String userData = "";

		// Create OpenStack client
		OSClient os = authenticate(HypervisorEndpoint, HypervisorUserName, HypervisorUserPassword, HypervisorTenantID);

		// Get Flavor based on Type String
		List<? extends Flavor> flavours = os.compute().flavors().list();
		Flavor flavor = null;
		for (Flavor f : flavours) {
			if (f.getId().equals(VMType) || f.getName().equals(VMType)) {
				flavor = f;
				break;
			}
		}
		if (flavor == null) {
			throw new RuntimeException("Cannot find flavor for input " + VMType);
		}

		// Get Flavor based on Type String
		List<? extends Image> images = os.compute().images().list();
		Image image = null;
		for (Image i : images) {
			if (i.getId().equals(VMImageID) || i.getName().equals(VMImageID)) {
				image = i;
				break;
			}
		}
		if (image == null) {
			throw new RuntimeException("Cannot find image for input " + VMImageID);
		}

		// Start Server
		ServerCreate sc = Builders.server().name("OTIA" + System.currentTimeMillis()).flavor(flavor).image(image)
				.addSecurityGroup(SecurityGroup).keypairName(VMKeyPairName).build();

		// This hack sets user data because this setter is not available in the
		// API
		try {
			// User Data
			if (userData != null && !userData.trim().isEmpty()) {
				Field reflectionUserData = sc.getClass().getDeclaredField("userData");
				reflectionUserData.setAccessible(true);
				reflectionUserData.set(sc,
						org.apache.commons.codec.binary.Base64.encodeBase64String(userData.getBytes()));
			}

		} catch (Exception e1) {
			e1.printStackTrace();
		}

		// Start Server
		Server server = os.compute().servers().boot(sc);

		// Retrying for some minutes for OpenStack to setup the server
		for (int i = 1; i <= 60; i++) {
			// Wait
			try {
				Thread.sleep(i * 1000); // wait for i sec
			} catch (InterruptedException e) {
				// we just go on in this case.
			}

			// Get server's information
			server = os.compute().servers().get(server.getId());

			if (server.getStatus().equals(Status.ERROR)) {
				// An error occurred
				throw new RuntimeException("Failed to start server.");

			} else if (server.getStatus().equals(Status.ACTIVE)) {
				// Ok, it's done, we can go on
				break;
			}
			// otherwise we just continue waiting
		}

		// Get server's fixed IP from the list of addresses
		String serversInternalIP = null;
		for (List<? extends Address> addressesOfNetwork : server.getAddresses().getAddresses().values()) {
			for (Address address : addressesOfNetwork) {
				if (address.getType().equals("fixed")) {
					serversInternalIP = address.getAddr();
					break;
				}
			}
		}

		// Try to find a Floating IP which is not assigned to any instance yet.
		String floatingIp = null;
		for (FloatingIP fip : os.compute().floatingIps().list()) {
			if (fip.getInstanceId() == null) {
				floatingIp = fip.getFloatingIpAddress();				
				System.out.println("Found a FloatingIP with is not assigned: " + floatingIp);
				break;
			}
		}

		// If there is no free FloatingIP, we try to assign one from the first
		// pools which allows us to allocate one.
		if (floatingIp == null) {
			List<String> poolNames = os.compute().floatingIps().getPoolNames();
			for (String poolName : poolNames) {
				try {
					// Try to allocate IP from this pool
					floatingIp = os.compute().floatingIps().allocateIP(poolName).getFloatingIpAddress();
					// If it worked, we stop here
					if (floatingIp != null) {
						System.out.println("Allocated new FloatingIP " + floatingIp + " from pool " + poolName
								+ " because there was no FloatingIP available which was not assigned yet.");
						break;
					}
				} catch (Exception e) {
					System.err.println("Unable to allocate FloatingIP from pool " + poolName + "(" + e.getMessage()
							+ "). Trying next pool.");
				}
			}
		}

		// Have we been able to find a FloatingIP?
		if (floatingIp == null) {
			System.err.println(
					"Unable to find and allocate a FloatingIP. Machine will be started without floating IP and, therefore, might not be accessible from the outside.");
			// If not, we are setting the server's internal IP as floating IP,
			// so it is returned to the user, even if it might not be accesible.
			floatingIp = serversInternalIP;

		} else {
			// Assign Floating IP
			os.compute().floatingIps().addFloatingIP(server, floatingIp);
		}

		// Output Parameter 'VMInstanceId' (optional)
		// Do NOT delete the next line of code. Set "" as value if you want to
		// return nothing or an empty result!
		returnParameters.put("VMInstanceID", server.getId());

		// Output Parameter 'VMIP' (optional)
		// Do NOT delete the next line of code. Set "" as value if you want to
		// return nothing or an empty result!
		returnParameters.put("VMIP", floatingIp);

		sendResponse(returnParameters);
	}

	@WebMethod
	@SOAPBinding
	@Oneway
	public void terminateVM(
			@WebParam(name = "HypervisorEndpoint", targetNamespace = "http://implementationartifacts.opentosca.org/") String APIEndpoint,
			@WebParam(name = "HypervisorUserName", targetNamespace = "http://implementationartifacts.opentosca.org/") String APIUser,
			@WebParam(name = "HypervisorUserPassword", targetNamespace = "http://implementationartifacts.opentosca.org/") String APIPassword,
			@WebParam(name = "HypervisorTenantID", targetNamespace = "http://implementationartifacts.opentosca.org/") String HypervisorTenantID,
			@WebParam(name = "VMInstanceID", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMInstanceId) {
		// Create OpenStack client
		OSClient os = authenticate(APIEndpoint, APIUser, APIPassword, HypervisorTenantID);

		// Get the server object
		Server server = null;
		for (Server s : os.compute().servers().list()) {
			// Search by ID and Name (this is more than advertised in the
			// interface, i.e. publicDNSorInstanceId)
			if (s.getId().equals(VMInstanceId) || s.getName().equals(VMInstanceId)) {
				server = s;
				break;
			}

			// Search by IP
			for (List<? extends Address> addressesOfNetwork : s.getAddresses().getAddresses().values()) {
				for (Address address : addressesOfNetwork) {
					if (address.getAddr().equals(VMInstanceId)) {
						server = s;
						break;
					}
				}
			}
		}

		// Shutdown and delete this server
		os.compute().servers().delete(server.getId());

		final HashMap<String, String> returnParameters = new HashMap<String, String>();
		returnParameters.put("Result", "true");
		sendResponse(returnParameters);
	}

	/**
	 * Creates an OpenStack client from the input data
	 * 
	 * @param HypervisorEndpoint
	 *            - the IP of the OpenStack instance
	 * @param accessKey
	 *            - HypervisorUserName must be <tenant>.<user> or <user>
	 * @param secretKey
	 *            - OpenStack HypervisorUserPassword
	 * @return Authenticated OpenStack Client
	 */
	private OSClient authenticate(String HypervisorEndpoint, String HypervisorUserName, String HypervisorUserPassword,
			String HypervisorTenant) {
		// Process accessKey which has the format <tenantId>.<user> or <user>
		// Process accessKey which has the format <tenantId>.<user> or <user>

		// OSFactory.enableHttpLoggingFilter(true);
		OSClient osClient = null;

		// for better debugging
		// OSFactory.enableHttpLoggingFilter(true);

		// Authenticate with OpenStack

		V2 credentials = OSFactory.builderV2().endpoint("http://" + HypervisorEndpoint + ":5000/v2.0")
				.credentials(HypervisorUserName, HypervisorUserPassword);
		if (!HypervisorTenant.isEmpty()) {
			credentials.tenantName(HypervisorTenant);
		}

		try {
			osClient = credentials.authenticate();
		} catch (Exception e) {
			System.err.println("Couldn't connect with v2 API, trying v3");
			osClient = null;
		}

		// v3 auth
		if (osClient == null) {
			V3 creds = OSFactory.builderV3().useNonStrictSSLClient(true).perspective(Facing.PUBLIC)
					.endpoint("https://" + HypervisorEndpoint + ":5000/v3")
					.credentials(HypervisorUserName, HypervisorUserPassword, Identifier.byName("Default"));
			try {
				osClient = creds.authenticate();
			} catch (Exception e) {
				e.printStackTrace();
				osClient = null;
			}
		}

		return osClient;
	}

}
