package org.opentosca.nodetypeimplementations;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.util.*;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlElement;

/**
 * Call which holds the functionality to compare the Rollout instance with the corresponding TOSCA instance and update the TOSCA devices and
 * distribution sets to the changes in Rollout
 */
@WebService
public class org_opentosca_nodetypes_BoschOTAManager__deviemanagement_sync extends AbstractIAService {

	private final IALogger LOG = new IALogger(org_opentosca_nodetypes_BoschOTAManager__deviemanagement_sync.class);
	private final String containerHost = "container";


	/**
	 * Method to compare the Rollout instance with the corresponding TOSCA instance and update the TOSCA devices and
	 * distribution sets to the changes in Rollout
	 * @param tenant the tenant of Rollout/HawkBit
	 * @param user the user of Rollout/HawkBit
	 * @param password the password of Rollout/HawkBit
	 * @param host the URL of Rollout/HawkBit
	 */
	@WebMethod
	@SOAPBinding
	@Oneway
	public void syncTOSCAwithRollout(
		@WebParam(name="tenant", targetNamespace="http://nodetypeimplementations.opentosca.org/") String tenant,
		@WebParam(name="user", targetNamespace="http://nodetypeimplementations.opentosca.org/") String user,
		@WebParam(name="password", targetNamespace="http://nodetypeimplementations.opentosca.org/") String password,
		@WebParam(name="host", targetNamespace="http://nodetypeimplementations.opentosca.org/") String host
	) {
		LOG.debug("Starting sync of Rollput of with OpenTOSCA");
		Utils utils = new Utils(containerHost);
		List<Device> devicesInRollout;
		List<DistributionSet> dsInRollout;
		HashMap<String, Device> devicesInTosca = new HashMap<>();
		HashMap<String, DistributionSet> dsInTosca = new HashMap<>();

		//http basic auth
		String credentials = utils.createCredentials(tenant, user, password);
		host = utils.generateHost(host);
		LOG.debug("Using Host: " + host);

		// 1) Get all devices and DS from Rollout
		LOG.debug("Start loading devices");
		devicesInRollout = getDevices(host, credentials, utils);
		LOG.debug("Finished loading devices");

		LOG.debug("Start loading distribution sets");
		dsInRollout = getDistributionSets(host, credentials, utils);
		LOG.debug("Finished loading distribution sets");

		// 2) Get all devices and DS from OpenTOSCA
		//creating parameters for the instance creation in OpenTOSCA
		String csar = utils.getCSAR();
		LOG.debug("Using CSAR: " + csar);
		String servicetemplate = utils.getServiceTemplate(csar);
		LOG.debug("Usind ServiceTemplate: " + servicetemplate);
		String instanceID = utils.getInstanceID(csar, servicetemplate);
		LOG.debug("Using InstanceID: " + instanceID);

		//requesting devices
		getDevicesFromTOSCA(utils, csar, servicetemplate).forEach(devicesInTosca::putIfAbsent);
		//requesting distribution sets
		getDistributionSetFromTosca(utils, csar, servicetemplate).forEach(dsInTosca::putIfAbsent);

		//3) Check which devices and DS are already in OpenTOSCA and Rollout
		Map<String, Device> devicesToChange = new HashMap<>();
		//fill all devices in TOSCA in it
		devicesToChange.putAll(devicesInTosca);
		for(int i = 0; i < devicesInRollout.size(); i++){
			boolean hasPartner = false;
			for (Map.Entry<String, Device> entry: devicesInTosca.entrySet()) {
				if(devicesInRollout.get(i).getDeviceID().equals(entry.getValue().getDeviceID())){
					//remove device from Rollout
					devicesToChange.remove(entry.getKey());
					hasPartner = true;
					break;
				}
			}
			if(!hasPartner){
				//add new device from Rollout
				devicesToChange.put("new_" + i, devicesInRollout.get(i));
			}
		}

		Map<String, DistributionSet> dsToChange = new HashMap<>();
		//fill all ds in TOSCA in it
		dsToChange.putAll(dsInTosca);
		for(int i = 0; i < dsInRollout.size(); i++){
			boolean hasPartner = false;
			for (Map.Entry<String, DistributionSet> entry: dsInTosca.entrySet()) {
				if(dsInRollout.get(i).getDistributionSetID().equals(entry.getValue().getDistributionSetID())){
					//remove ds from Rollout
					dsToChange.remove(entry.getKey());
					hasPartner = true;
					break;
				}
			}
			if(!hasPartner){
				//add new ds from Rollout
				dsToChange.put("new_" + i, dsInRollout.get(i));
			}
		}

		//TODO: 4) Add new devices and DS to OpenTOSCA or remove that one, that are to many
		String nodetemplates = utils.getNodetemplates(csar, servicetemplate, "Device");
		LOG.debug("Using NodeTemplate: "+ nodetemplates);
		for (Map.Entry<String, Device> entry : devicesToChange.entrySet()) {
			if(entry.getKey().startsWith("new")){
				utils.createInstance(csar, servicetemplate, nodetemplates, instanceID,
						Arrays.asList("deviceID", "assignedDS"),
						Arrays.asList(entry.getValue().getDeviceID(), entry.getValue().getAssignedDS()));
			} else {
				utils.deteInstance(csar, servicetemplate, nodetemplates, entry.getKey());
			}
		}

		nodetemplates = utils.getNodetemplates(csar, servicetemplate, "Distribution");
		LOG.debug("Using NodeTemplate: "+ nodetemplates);
		for (Map.Entry<String, DistributionSet> entry : dsToChange.entrySet()) {
			if(entry.getKey().startsWith("new")){
				utils.createInstance(csar, servicetemplate, nodetemplates, instanceID,
						Collections.singletonList("distributionSet"),
						Collections.singletonList((entry.getValue().getDistributionSetID())));
			} else {
				utils.deteInstance(csar, servicetemplate, nodetemplates, entry.getKey());
			}
		}

		LOG.debug("Ended sync of Rollput of with OpenTOSCA");
		final HashMap<String,String> returnParameters = new HashMap<String, String>();
		returnParameters.put("success", "success");

		sendResponse(returnParameters);
	}


	/**
	 * Requesting all devices from a given TOSCA instance
	 * @param utils the utils class
	 * @param csar the csar which a service template is created of which holds the devices
	 * @param servicetemplate the service templates which holds the nodetemplates of the devices
	 * @return a list of all devices with their TOSCA ID
	 */
	private Map<String, Device> getDevicesFromTOSCA(Utils utils, String csar, String servicetemplate){
		Map<String, Device> devicesInTosca = new HashMap<>();
		String deviceTemplate = utils.getNodetemplates(csar, servicetemplate, "Device");
		LOG.debug("Using NodeTemplate for devices: " + deviceTemplate);
		String baseHost = "http://" + utils.getHost() + ":1337/csars/" + csar + "/servicetemplates/" + servicetemplate
				+ "/nodetemplates/" + deviceTemplate + "/instances/";
		LOG.debug("Using " + baseHost + " as URL for requesting devices");
		JSONArray deviceListofTOSCA = utils.getAllInstancesOfOneNodeTemplate(baseHost);

		for(int i = 0; i < deviceListofTOSCA.length(); i++){
			String url = deviceListofTOSCA.getJSONObject(i).getJSONObject("_links").getJSONObject("self").getString("href");
			url = url + "/properties/";
			String propertis = utils.httpRequests(url, "", "GET", "application/xml");
			Document xmlProperties = utils.loadXMLFromString(propertis);

			try {
				String deviceID = xmlProperties.getElementsByTagName("deviceID").item(0).getFirstChild().getNodeValue();
				String assignedDS = xmlProperties.getElementsByTagName("assignedDS").item(0).getFirstChild().getNodeValue();
				String idInTosca = Integer.toString(deviceListofTOSCA.getJSONObject(i).getInt("id"));
				Device deviceInTosca = new Device(deviceID, assignedDS);
				devicesInTosca.put(idInTosca, deviceInTosca);

			} catch (NullPointerException e){
				e.printStackTrace();
			}
		}
		return  devicesInTosca;
	}

	/**
	 * Requesting all ds from a given TOSCA instance
	 * @param utils the utils class
	 * @param csar the csar which a service template is created of which holds the ds
	 * @param servicetemplate the service templates which holds the nodetemplates of the ds
	 * @return a list of all ds with their TOSCA ID
	 */
	private Map<String, DistributionSet> getDistributionSetFromTosca(Utils utils, String csar, String servicetemplate){
		Map<String, DistributionSet> distributionSetInTosca = new HashMap<>();
		String deviceTemplate = utils.getNodetemplates(csar, servicetemplate, "Distribution");
		LOG.debug("Using NodeTemplate for devices: " + deviceTemplate);
		String baseHost = "http://" + utils.getHost() + ":1337/csars/" + csar + "/servicetemplates/" + servicetemplate
				+ "/nodetemplates/" + deviceTemplate + "/instances/";
		LOG.debug("Using " + baseHost + " as URL for requesting distribution sets");
		JSONArray dsListofTOSCA = utils.getAllInstancesOfOneNodeTemplate(baseHost);

		for(int i = 0; i < dsListofTOSCA.length(); i++){
			String url = dsListofTOSCA.getJSONObject(i).getJSONObject("_links").getJSONObject("self").getString("href");
			url = url + "/properties/";
			String propertis = utils.httpRequests(url, "", "GET", "application/xml");
			Document xmlProperties = utils.loadXMLFromString(propertis);

			try {
				String dsID = xmlProperties.getElementsByTagName("distributionSet").item(0).getFirstChild().getNodeValue();
				String idInTosca = Integer.toString(dsListofTOSCA.getJSONObject(i).getInt("id"));
				DistributionSet dsInTosca = new DistributionSet(dsID);
				distributionSetInTosca.put(idInTosca, dsInTosca);

			} catch (NullPointerException e){
				e.printStackTrace();
			}
		}
		return  distributionSetInTosca;
	}


	/**
	 * Requesting all Distribution Sets from the OTA-Manager
	 * @param host the host of the OTA manager
	 * @param authStringEnc the basic http auth values
	 * @return a list of values with distribution sets id value
	 */
	private List<DistributionSet> getDistributionSets(String host, String authStringEnc, Utils utils) {
		final List<DistributionSet>  returnParameters = new ArrayList<>();
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
				LOG.debug("Adding Distribution Set with ID: " + resultID);
				returnParameters.add(new DistributionSet(resultID));
			}
		}
		return returnParameters;
	}

	/**
	 * Requesting all devices from the OTA-Manager
	 * @param host the host of the OTA manager
	 * @param authStringEnc the basic http auth values
	 * @return a Map of values (well only one value...) with one device id value
	 */
	private List<Device> getDevices(String host, String authStringEnc, Utils utils) {
		final List<Device>  returnParameters = new ArrayList<>();
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
				if (assignedDS != null) {
					deviceDS = Integer.toString(assignedDS.getInt("id"));
				}
				LOG.debug("Adding Device with ID: " + resultID + " and assignedDS: " + deviceDS);
				returnParameters.add(new Device(resultID, deviceDS));
			}
		}
		return returnParameters;
	}
}
