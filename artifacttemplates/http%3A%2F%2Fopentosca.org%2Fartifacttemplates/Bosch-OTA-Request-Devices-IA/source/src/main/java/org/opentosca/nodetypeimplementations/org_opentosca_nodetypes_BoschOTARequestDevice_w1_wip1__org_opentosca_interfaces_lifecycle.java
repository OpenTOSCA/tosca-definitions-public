package org.opentosca.nodetypeimplementations;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

/**
 * Implementing the Install Operation for the BoschOTA-Manager
 * Requesting all Devices and Distribution Sets from the OTA-Manager and creating OpenTOSCA-Container instances of it
 */
@WebService
public class org_opentosca_nodetypes_BoschOTARequestDevice_w1_wip1__org_opentosca_interfaces_lifecycle extends AbstractIAService {

	private final IALogger LOG = new IALogger(org_opentosca_nodetypes_BoschOTARequestDevice_w1_wip1__org_opentosca_interfaces_lifecycle.class);
	private final String containerHost = "container";

	/**
	 * The install operation for the OTA-Manager
	 * @param tenant the tenant of the OTA manager
	 * @param user the user of the OTA manager
	 * @param password the password of the OTA manager
	 * @param host the host of the OTA manager
	 */
	@WebMethod
	@SOAPBinding
	@Oneway
	public void install(
		@WebParam(name="tenant", targetNamespace="http://nodetypeimplementations.opentosca.org/") String tenant,
		@WebParam(name="user", targetNamespace="http://nodetypeimplementations.opentosca.org/") String user,
		@WebParam(name="password", targetNamespace="http://nodetypeimplementations.opentosca.org/") String password,
		@WebParam(name="host", targetNamespace="http://nodetypeimplementations.opentosca.org/") String host
	) {
		// This HashMap holds the return parameters of this operation.
		final HashMap<String, String> returnParameters = new HashMap<String, String>();
		LOG.debug("Starting install operation");
		Utils utils = new Utils(containerHost);
		//http basic auth
		String credentials = utils.createCredentials(tenant, user, password);

		//creating parameters for the instance creation in OpenTOSCA
		String csar = utils.getCSAR();
		LOG.debug("Using CSAR: " + csar);
		String servicetemplate = utils.getServiceTemplate(csar);
		LOG.debug("Usind ServiceTemplate: " + servicetemplate);
		String instanceID = utils.getInstanceID(csar, servicetemplate);
		LOG.debug("Usind InstanceID: " + instanceID);
		host = utils.generateHost(host);
		LOG.debug("Using Host: " + host);

		LOG.debug("Start loading devices");
		getDevices(host, credentials, utils, csar, servicetemplate, instanceID).forEach(returnParameters::putIfAbsent);
		LOG.debug("Finished loading devices");

		LOG.debug("Start loading distribution sets");
		getDistributionSets(host, credentials, utils, csar, servicetemplate, instanceID).forEach(returnParameters::putIfAbsent);
		LOG.debug("Finished loading distribution sets");

		sendResponse(returnParameters);
		LOG.debug("Finished install operation");
	}

	/**
	 * Requesting all Distribution Sets from the OTA-Manager and creating OpenTOSCA container instances of it
	 * @param host the host of the OTA manager
	 * @param authStringEnc the basic http auth values
	 * @param utils a util class
 	 * @param csar the csar for the distribution set instance creation
	 * @param servicetemplate the servicetemplate for the distribution set instance creation
	 * @param instanceID the instanceID for the distribution set instance creation
	 * @return a Map of values (well only one value...) with one distribution set id value
	 */
	private HashMap<String, String> getDistributionSets(String host, String authStringEnc, Utils utils, String csar, String servicetemplate, String instanceID) {
		final HashMap<String, String> returnParameters = new HashMap<>();
		//request devices
		String requestURL = host + "/rest/v1/distributionsets";
		JSONObject response = utils.getHTTPRequestResponse(requestURL, authStringEnc);
		//parse json response
		if (response != null) {
			int size = response.getInt("size");
			JSONArray devices = response.getJSONArray("content");
			for (int i = 0; i < size; i++) {
				JSONObject element = devices.getJSONObject(i);
				String resultID = Integer.toString(element.getInt("id"));
				LOG.debug("Using Distribution Set with ID: " + resultID);
				if(i == 0){
					//as we just can return one value to OpenTOSCA
					returnParameters.put("distributionSet", resultID);
				} else {
					//we do the other values per API call
					String nodetemplates = utils.getNodetemplates(csar, servicetemplate, "Distribution");
					utils.createInstance(csar, servicetemplate, nodetemplates, instanceID,
							Collections.singletonList("distributionSet"), Collections.singletonList(resultID));
				}
			}
		} else {
			returnParameters.put("distributionSet", "error in DistributionSet");
		}
		return returnParameters;
	}

	/**
	 * Requesting all devices from the OTA-Manager and creating OpenTOSCA container instances of it
	 * @param host the host of the OTA manager
	 * @param authStringEnc the basic http auth values
	 * @param utils a util class
	 * @param csar the csar for the device instance creation
	 * @param servicetemplate the servicetemplate for the device instance creation
	 * @param instanceID the instanceID for the device instance creation
	 * @return a Map of values (well only one value...) with one device id value
	 */
	private HashMap<String, String> getDevices(String host, String authStringEnc, Utils utils, String csar, String servicetemplate, String instanceID) {
		final HashMap<String, String> returnParameters = new HashMap<>();
		//request devices
		String requestURL = host + "/rest/v1/targets";
		JSONObject response = utils.getHTTPRequestResponse(requestURL, authStringEnc);
		//parse json response
		if (response != null) {
			int size = response.getInt("size");
			JSONArray devices = response.getJSONArray("content");
			for (int i = 0; i < size; i++) {
				JSONObject element = devices.getJSONObject(i);
				String resultID = element.getString("controllerId");

				JSONObject assignedDS = utils.getHTTPRequestResponse(requestURL + "/" + resultID + "/assignedDS", authStringEnc);
				String deviceDS = "No DS assigned";
				if(assignedDS != null) {
					deviceDS = Integer.toString(assignedDS.getInt("id"));
				}

				LOG.debug("Usind Device with ID: " + resultID + " and assignedDS: " + assignedDS);
				if(i == 0){
					//as we just can return one value to OpenTOSCA
					returnParameters.put("deviceID", resultID);
					returnParameters.put("assignedDS", deviceDS);
				} else {
					//we do the other values per API call
					String nodetemplates = utils.getNodetemplates(csar, servicetemplate, "Device");
					utils.createInstance(csar, servicetemplate, nodetemplates, instanceID,
							Arrays.asList("deviceID", "assignedDS"), Arrays.asList(resultID, deviceDS));
				}
			}
		} else {
			returnParameters.put("deviceID", "error in device");
			returnParameters.put("assignedDS", "error in device");
		}
		return returnParameters;
	}
}
