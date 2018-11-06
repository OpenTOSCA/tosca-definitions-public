package org.opentosca.nodetypeimplementations;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * This class offers utility methods to handle the needed API logic to work with Rollout/HawkBit in the Context
 * of OpenTOSCA
 */
class Utils {
	private String host;
	private final IALogger LOG = new IALogger(Utils.class);

	public Utils(String containerHost){
		host = containerHost;
	}

	/**
	 * Creating the credentials for a HawkBit or Rollout instance
	 * @param tenant the tenant of the Rollout instance or empty is HawkBit instance
	 * @param user the user of the OTA-Manager
	 * @param password the password of the OTA-Manager
	 * @return an Base64 encoded String for Basic HTTP Authentication
	 */
	String createCredentials(String tenant, String user, String password){
		String authString;

		//check if it is a local hawkbit or a remote rollout service
		if(!tenant.isEmpty() ){
			authString = tenant + "\\" + user + ":" + password;
			LOG.debug("Creating authentication for Rollout");
		} else {
			// HawkBit does not have a tenant
			authString = user + ":" + password;
			LOG.debug("Creating authentication for HawkBit");
		}
		//encode for Basic HTTP Authentication
		byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
		return new String(authEncBytes);
	}

	/**
	 * Generates a URL for Javas HTTP API
	 * @param host the url, with http or without
	 * @return returns the url with leading http://
	 */
	String generateHost(String host){
		if (host.startsWith("http://")){
			return host;
		} else {
			return "http://" + host;
		}
	}

	/**
	 * Request all CSARs from your OpenTOSCA Container and checks if one is a BoschOTA Csar
	 * @return the CSAR of a BoschOTA if found, else null
	 */
	String getCSAR(){
		JSONArray csars = new JSONObject(httpRequests("http://" + host + ":1337/csars/", "", "GET", "application/json")).getJSONArray("csars");
		LOG.debug("Requesting CSARs from: " + "http://" + host + ":1337/csars/");
		for (int i = 0; i < csars.length(); i++){
			String boschOTA = csars.getJSONObject(i).getString("id");
			LOG.debug("Got CSAR: " + boschOTA);
			if(boschOTA.contains("BoschOTA")){
				LOG.debug("Using OTA-CSAR: " + boschOTA);
				return boschOTA;
			}
		}
		LOG.debug("No CSAR implementing BoschOTA was found");
		return null;
	}

	/**
	 * Request all service templates from the given CSAR and check for a BoschOTA Service Template
	 * @param csar the csar to get the service templates from
	 * @return the proper BoschOTA ServiceTemplate if found, else null
	 */
	String getServiceTemplate(String csar){
		JSONObject requestResult = new JSONObject(httpRequests("http://" + host + ":1337/csars/" + csar + "/servicetemplates/", "", "GET", "application/json"));
		LOG.debug("Requesting ServiceTemplates from: " + "http://" + host + ":1337/csars/" + csar + "/servicetemplates/");
		JSONArray links = requestResult.getJSONArray("service_templates");

		for (int i = 0; i < links.length(); i++){
			String serviceTemplateURL = links.getJSONObject(i).getJSONObject("_links").getJSONObject("self").getString("href");
			LOG.debug("Got ServiceTemplate: " + serviceTemplateURL);
			if(serviceTemplateURL.contains("BoschOTA")) {
				String serviceTemplate[] =serviceTemplateURL.split("/");
				LOG.debug("Using ServiceTemplate: " + serviceTemplate[serviceTemplate.length -1]);
				return serviceTemplate[serviceTemplate.length -1];
			}
		}
		LOG.debug("No fitting OTA-ServiceTemplate from Type BoschOTA was found");
		return null;
	}

	/**
	 * Request a NodeTemplate from a given csar, servicetemplate and node type
	 * @param csar the csar to search in
	 * @param servicetemplate the service template to search in
	 * @param nodeType the nodetype to find templates for
	 * @return the nodetemplate of the given nodetype if found, else null
	 */
	String getNodetemplates(String csar, String servicetemplate, String nodeType) {
		JSONObject requestResult = new JSONObject(httpRequests("http://" + host + ":1337/csars/" + csar + "/servicetemplates/" + servicetemplate + "/nodetemplates/"
				, "", "GET", "application/json"));
		LOG.debug("Requesting NodeTemplates from: " + "http://" + host + ":1337/csars/" + csar + "/servicetemplates/" + servicetemplate + "/nodetemplates/");
		JSONArray nodeTemplates = requestResult.getJSONArray("node_templates");

		for(int i = 0; i < nodeTemplates.length(); i++){
			LOG.debug("Check if: " + nodeTemplates.getJSONObject(i).getString("id") + " starts with: " + nodeType);
			if(nodeTemplates.getJSONObject(i).getString("id").startsWith(nodeType)){
				String[] nodeTemplate = nodeTemplates.getJSONObject(i).getJSONObject("_links").getJSONObject("self").getString("href").split("/");
				LOG.debug("Using NodeTemplate: " + nodeTemplate[nodeTemplate.length - 1]);
				return nodeTemplate[nodeTemplate.length - 1];
			}
		}
		LOG.debug("No NodeTemplate of Type: " + nodeType + " was found");
		return null;
	}

	/**
	 * Requesting the newest Instance ID of a given ServiceTemplate of a given CSAR
	 * @param csar the csar to get the service template from
	 * @param servicetemplate the servicetemplate to get the instances from
	 * @return the id of the newest service template if one found, else null
	 */
	String getInstanceID(String csar, String servicetemplate){
		JSONObject requestResult = new JSONObject(httpRequests("http://" + host + ":1337/csars/" + csar + "/servicetemplates/" + servicetemplate + "/instances/"
				, "", "GET", "application/json"));
		LOG.debug("Requesting ServiceTemplateInstances from " + "http://" + host + ":1337/csars/" + csar + "/servicetemplates/" + servicetemplate + "/instances/");
		JSONArray instances =  requestResult.getJSONArray("service_template_instances");
		Integer instanceID = null;
		long timestamp = 0L;

		//check for an instance which is in a CREATED state and is the newest created instance (there may be older versions laying around)
		for (int i = 0; i < instances.length(); i++){
			JSONObject instance = instances.getJSONObject(i);
			long instanceTimestamp = instance.getLong("created_at");
			String instanceState = instance.getString("state");
			int tempInstanceID = instance.getInt("id");
			if(instanceState.equals("CREATED") && instanceTimestamp > timestamp){
				timestamp = instanceTimestamp;
				instanceID = tempInstanceID;
			}
		}
		String response = instanceID == null ? null : Integer.toString(instanceID);
		LOG.debug("Using ServceTemplateInstance with ID: " + response);
		return response;
	}

	/**
	 *
	 * Create an OpenTOSCA instance of a nodetemplate for the given parameters (we need this, as we can only return one value per return value to OpenTOSCA)
	 * @param csar the csar of the nodetemplate instance
	 * @param servicetemplate the servicetemplate of the nodetemplate instance
	 * @param nodetemplates the nodetemplate where we want to create an instance
	 * @param instanceID the ID of the new instance
	 * @param properties the property names of the new instance
	 * @param propertiesValue the values of the properties
	 */
	void createInstance(String csar, String servicetemplate, String nodetemplates, String instanceID, List<String> properties, List<String> propertiesValue){
		String baseHost = "http://" + host + ":1337/csars/" + csar + "/servicetemplates/" + servicetemplate
				+ "/nodetemplates/" + nodetemplates + "/instances/";
		LOG.debug("Using " + baseHost + " as URL for creating Instances");

		//create Instance
		LOG.debug("Creating Instances");
		String newInstance = httpRequests(baseHost, instanceID, "POST", "text/plain");

		//set properties
		LOG.debug("Setting Properties");
		StringBuilder propValues = new StringBuilder("<Properties>");
		for(int i = 0; i < properties.size(); i++) {
			propValues.append("<").append(properties.get(i)).append(">").append(propertiesValue.get(i)).append("</").append(properties.get(i)).append(">");
		}
		propValues.append("</Properties>");
		httpRequests(newInstance + "/properties/", propValues.toString(), "PUT", "application/xml");

		//set state
		LOG.debug("Setting the state");
		httpRequests(newInstance + "/state/", "STARTED", "PUT", "text/plain");
	}

	/**
	 * Making a HTTP request for a given HTTP verb, getting an response message back.
	 * For not GET verbs we need to escape the message properly
	 * @param host the host for the request
	 * @param requestBody the body for the request
	 * @param httpMethod the http verb
	 * @param contenType the contentype of the request
	 * @return the respose of the http request as string (escaped if it is a not GET)
	 */
	String httpRequests(final String host, final String requestBody, String httpMethod, String contenType) {
		LOG.debug("Requesting URL: " + host + " with HTTP Method: " + httpMethod + " with ContentType: " + contenType + " and Body:" + requestBody);
		StringBuilder getResponse = new StringBuilder();
		boolean escape = false;
		try {
			final URL url = new URL(host);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", contenType + ";charset=UTF-8");
			connection.setRequestMethod(httpMethod);

			if(!httpMethod.equals("GET")) {
				final OutputStream os = connection.getOutputStream();
				os.write(requestBody.getBytes("UTF-8"));
				os.close();
				escape = true;
			}

			final InputStream content = connection.getInputStream();
			final BufferedReader in = new BufferedReader(new InputStreamReader(content));
			String line;
			while ((line = in.readLine()) != null) {
				getResponse.append(line);
			}
			in.close();
			connection.disconnect();
		}
		catch (final Exception e) {
			e.printStackTrace();
		}
		if(escape) {
			getResponse = new StringBuilder(getResponse.toString().replace("\"", ""));
			LOG.debug("The Response of the Request is:" + getResponse);
			return getResponse.toString();
		} else {
			LOG.debug("The Response of the Request is:" + getResponse);
			return getResponse.toString();
		}
	}

	/**
	 * Making a HTTP GET request, getting an JSON Object back
	 * @param requestURL the url to reques
	 * @param authStringEnc the credentials for the basic http auth
	 * @return a json object containing the result, or null
	 */
	JSONObject getHTTPRequestResponse(String requestURL, String authStringEnc) {
		JSONObject response = null;
		LOG.debug("Requesting URL: " + requestURL);
		try {
			URL url = new URL(requestURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
			connection.setRequestMethod("GET");

			InputStream content = connection.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(content));
			StringBuilder getResponse = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				getResponse.append(line);
			}
			if(getResponse.toString().startsWith("{")){
				response = new JSONObject(getResponse.toString());
			}
			connection.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
			return response;
		}
		return response;
	}

	/**
	 * Requests all registered instances in OpenTOSCA container and searches the TOSCA-Conainer ID for a given property value
	 * @param host the url to the OpenTOSCA container
	 * @param propertyName the name of the instance we want to update
	 * @param propertyTag the tag of the property we want to search for
	 * @return the OpenTOSCA container ID we want to update
	 */
	String getInstanceIDbyProperty(String host, String propertyName, String propertyTag){
		JSONArray instances = getAllInstancesOfOneNodeTemplate(host);
		String id = null;

		for(int i = 0; i < instances.length(); i++){
			String url = instances.getJSONObject(i).getJSONObject("_links").getJSONObject("self").getString("href");
			url = url + "/properties/";
			String property = httpRequests(url, "", "GET", "application/xml");
			Document xmlProperties = loadXMLFromString(property);
			try {
				String deviceID = xmlProperties.getElementsByTagName(propertyTag).item(0).getFirstChild().getNodeValue();
				if (deviceID.equals(propertyName)) {
					id = Integer.toString(instances.getJSONObject(i).getInt("id"));
				}
			} catch (NullPointerException e){
				e.printStackTrace();
			}
		}
		return id;
	}

	/**
	 * Request all instances of a given NodeTemplate
	 * @param host the url to OpenTOSCA
	 * @return an array with the values
	 */
	JSONArray getAllInstancesOfOneNodeTemplate(String host){
		JSONObject instanceList = getHTTPRequestResponse(host, "user:password");
		JSONArray instances = instanceList.getJSONArray("node_template_instances");
		return instances;
	}

	/**
	 * XML Document creator from a given xml string
	 * @param xml the xml as string to create a DOM-document
	 * @return a DOM-document created by the xml in the string
	 */
	Document loadXMLFromString(String xml){
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(xml));
			return builder.parse(is);
		}catch (ParserConfigurationException | IOException | SAXException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 *
	 * Detelets an OpenTOSCA instance of a nodetemplate for the given parameters (
	 * @param csar the csar of the nodetemplate instance
	 * @param servicetemplate the servicetemplate of the nodetemplate instance
	 * @param nodetemplates the nodetemplate where we want to delete an instance
	 * @param instanceID the ID of the instance we want to delete

	 */
	void deteInstance(String csar, String servicetemplate, String nodetemplates, String instanceID){
		String baseHost = "http://" + host + ":1337/csars/" + csar + "/servicetemplates/" + servicetemplate
				+ "/nodetemplates/" + nodetemplates + "/instances/" + instanceID;
		LOG.debug("Using " + baseHost + " as URL for deleting Instance " + instanceID);

		//create Instance
		LOG.debug("Deleting Instances");
		String deletedInstance = httpRequests(baseHost, "", "DELETE", "application/json");
		LOG.debug("Deleting response is " + deletedInstance);
	}

	/**
	 * Getter for the host
	 * @return the host
	 */
	String getHost() {
		return host;
	}
}
