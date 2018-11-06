package org.opentosca.nodetypeimplementations;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Class to create a new Distribution Set and add a SoftwareModule to it
 */
class CreateDSaddSM {

	private int distributionSetID;
	private final IALogger LOG = new IALogger(CreateDSaddSM.class);
	private final static String DESCRIPTION_VALUES = "created_by_OpenTOSCA";


	/**
	 * Constructor which will create an Distribution Set and add a SotwareModule to it
	 * @param softwareModuleID the ID of the SoftwareModule, that should be added to a DS
	 * @param credentials the credentials to Rollout/HawkBit
	 * @param host the URL of Rollout/HawkBit
	 * @param DS_name the name of the new Distribution Set
	 */
	CreateDSaddSM(String softwareModuleID, String credentials, String host, String DS_name) {
		LOG.debug("Creating distribution set");
		createDSandAddSM(softwareModuleID, host, credentials, DS_name);
		LOG.debug("Created distribution set: " + distributionSetID);
	}

	/**
	 * Method which will create the Distribution Set and add the Software Module
	 * @param softwareModuleID the ID of the SoftwareModule, that should be added to a DS
	 * @param host the URL of Rollout/HawkBit
	 * @param credentials the credentials to Rollout/HawkBit
	 * @param DS_name the name of the new Distribution Set
	 */
	private void createDSandAddSM(String softwareModuleID, String host, String credentials, String DS_name) {
		JSONObject inputValues = new JSONObject();
		inputValues.put("type", "os");
		inputValues.put("name", DS_name);
		inputValues.put("version", DESCRIPTION_VALUES);
		inputValues.put("requiredMigrationStep", false);
		inputValues.put("description", DESCRIPTION_VALUES);
		JSONObject modules_id = new JSONObject();
		modules_id.put("id", softwareModuleID);
		JSONArray modules = new JSONArray();
		modules.put(modules_id);
		inputValues.put("modules", modules);

		JSONArray input = new JSONArray();
		input.put(inputValues);
		String message = input.toString();

		try {
			LOG.debug("Requesting: " + host + "/rest/v1/distributionsets");
			URL url = new URL(host + "/rest/v1/distributionsets");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("Authorization", "Basic " + credentials);
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/hal+json;charset=UTF-8");
			connection.setRequestMethod("POST");

			OutputStream os = connection.getOutputStream();
			os.write(message.getBytes("UTF-8"));
			os.close();

			LOG.debug("Response was: " + connection.getResponseCode());
			if (connection.getResponseCode() == 201) {
				StringBuilder getResponse = new StringBuilder();
				InputStream content = connection.getInputStream();
				BufferedReader in = new BufferedReader(new InputStreamReader(content));
				String line;
				while ((line = in.readLine()) != null) {
					getResponse.append(line);
				}
				in.close();
				JSONArray response = new JSONArray(getResponse.toString());

				distributionSetID = response.getJSONObject(0).getInt("id");
			}
			connection.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		LOG.debug("distributionSetID is: " + distributionSetID);
	}

	/**
	 * Getter for the distributionSetID
	 * @return the ID of the distribution set
	 */
	public int getDistributionSetID() {
		return distributionSetID;
	}
}
