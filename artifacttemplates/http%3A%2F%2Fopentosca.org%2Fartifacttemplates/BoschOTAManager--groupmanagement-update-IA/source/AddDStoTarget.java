package org.opentosca.nodetypeimplementations;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Class which will add a distribution set to a given device
 */
class AddDStoTarget {

	private int result;
	private final IALogger LOG = new IALogger(AddDStoTarget.class);

	/**
	 * Constructor which will update the give device with the given distribution set
	 * @param controllerID the ID of the device we want to Update
	 * @param distributionSetID the ID of the Distribution set with the Update
	 * @param credentials the credentials to Rollout/HawkBit
	 * @param host the URL of Rollout/HawkBit
	 */
	AddDStoTarget(String controllerID, String distributionSetID, String credentials, String host){
		LOG.debug("Adding DS: " + distributionSetID + " for device: " + controllerID);
		addDStoTarget(controllerID, Integer.parseInt(distributionSetID), credentials, host);
		LOG.debug("Added DS: " + distributionSetID + " for device: " + controllerID + " with response: " + getResult());
	}

	/**
	 * Method which will update the give device with the given distribution set
	 * @param controllerID the ID of the device we want to Update
	 * @param distributionSetID the ID of the Distribution set with the Update
	 * @param credentials the credentials to Rollout/HawkBit
	 * @param host the URL of Rollout/HawkBit
	 */
	private void addDStoTarget(String controllerID, int distributionSetID, String credentials, String host){
		try {
			LOG.debug("Requesting: " + host + "/rest/v1/targets/" + controllerID + "/assignedDS");
			URL url = new URL(host + "/rest/v1/targets/" + controllerID + "/assignedDS");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("Authorization", "Basic " + credentials);
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
			connection.setRequestMethod("POST");

			//adding the id of the distribution set as body for the POST request on the device
			JSONObject inputValues = new JSONObject();
			inputValues.put("id", distributionSetID);
			String message = inputValues.toString();

			OutputStream os = connection.getOutputStream();
			os.write(message.getBytes("UTF-8"));
			os.close();

			result = connection.getResponseCode();
			connection.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		LOG.debug("The ResponseCode is: " + result);
	}

	/**
	 * Getter for the result code
	 * @return the result code of the update operation
	 */
	private int getResult() {
		return result;
	}
}
