package org.opentosca.nodetypeimplementations;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

/**
 * Class for creating a SoftwareModule in Rollout/HawkBit, downloading a File from a FileServer and Uploading that
 * file to the SoftwareModule in Rollout/HawkBit
 */
class CreateSMaddBinary {

	private String softwareModuleID = null;
	private final IALogger LOG = new IALogger(CreateSMaddBinary.class);
	private final static String DESCRIPTION_VALUES = "created_by_OpenTOSCA";


	/**
	 * Constructor, which will automatically create a Software Module, download and upload the give file
	 * @param credentials the credentials to Rollout/HawkBit
	 * @param host the URL of Rollout/HawkBit
	 * @param urlToBinary the URL to the Fileserver containing the file for the Rollout/Hawkbit Software Module
	 * @param name the Name of the new creating SoftwareModule
	 */
	CreateSMaddBinary(String credentials, String host, String urlToBinary, String name){
		LOG.debug("Create Software Module");
		softwareModuleID = createSoftwareModule(name, credentials, host);
		LOG.debug("Created Software Module with id: " + softwareModuleID);
		if(softwareModuleID != null) {
			LOG.debug("Downloading binary");
			File newVersion = downloadBinary(urlToBinary);
			LOG.debug("Downloaded binary: " + newVersion.getName());
			if(newVersion.exists()){
				LOG.debug("Uploading binary");
				int status = uploadBinary(newVersion, host, credentials);
				LOG.debug("Upload ended with: " + status);
				boolean isDeleted = newVersion.delete();
				LOG.debug("The file was deleted: " + isDeleted);
			}
		}
	}

	/**
	 * Method to create a SoftwareModule in a give Rollout/HawkBit with given Name
	 * @param name the name of the new Software Module
	 * @param credentials the credentials to Rollout/HawkBit
	 * @param host the URL of Rollout/HawkBit
	 * @return the ID of the created SoftwareModule
	 */
	private String createSoftwareModule(String name, String credentials, String host){
		String id = null;
		JSONObject inputValues = new JSONObject();
		inputValues.put("type", "os");
		inputValues.put("name", name);
		inputValues.put("version", DESCRIPTION_VALUES);
		inputValues.put("vendor", DESCRIPTION_VALUES);
		inputValues.put("description", DESCRIPTION_VALUES);

		JSONArray input = new JSONArray();
		input.put(inputValues);
		String message = input.toString();

		try {
			LOG.debug("Requesting: " + host + "/rest/v1/softwaremodules");
			URL url = new URL(host + "/rest/v1/softwaremodules");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("Authorization", "Basic " + credentials);
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/hal+json;charset=UTF-8");
			connection.setRequestMethod("POST");

			OutputStream os = connection.getOutputStream();
			os.write(message.getBytes("UTF-8"));
			os.close();

			LOG.debug("Software Module Creation ended with :" + connection.getResponseCode());
			if (connection.getResponseCode() == 201){
				StringBuilder getResponse = new StringBuilder();
				InputStream content = connection.getInputStream();
				BufferedReader in =	new BufferedReader (new InputStreamReader(content));
				String line;
				while ((line = in.readLine()) != null) {
					getResponse.append(line);
				}
				in.close();
				JSONArray response = new JSONArray(getResponse.toString());
				id = Integer.toString(response.getJSONObject(0).getInt("id"));
			}
			connection.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return id;
	}

	/**
	 * Downloads a File from a given URL and return it
	 * @param urlToBinary the URL to the File to download
	 * @return the Downloaded file
	 */
	private File downloadBinary(String urlToBinary){
		File downloadedFile = new File(urlToBinary.split("/")[urlToBinary.split("/").length - 1]);
		LOG.debug("URL to request is: " + urlToBinary);
		try {
			URL website = new URL(urlToBinary);
			ReadableByteChannel rbc = Channels.newChannel(website.openStream());
			FileOutputStream fos = new FileOutputStream(downloadedFile);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return downloadedFile;
	}

	/**
	 * Uploads a File to a given SoftwareModule in Rollout/Hawkbit
	 * @param newVersion the File containing the new Update
	 * @param host the URL of Rollout/HawkBit
	 * @param credentials the credentials to Rollout/HawkBit
	 * @return  the HTTP response code of the upload-POST-request
	 */
	private int uploadBinary(File newVersion, String host, String credentials){
		int result = 0;
		try {
			URL url = new URL(host + "/rest/v1/softwaremodules/" + softwareModuleID +"/artifacts");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("Authorization", "Basic " + credentials);
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", " multipart/form-data; boundary=" + "----SomeRandomText");
			connection.setRequestMethod("POST");

			MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, "----SomeRandomText", Charset.defaultCharset());
			ContentBody contentPart = new FileBody(newVersion);
			reqEntity.addPart("file", contentPart);
			reqEntity.writeTo(connection.getOutputStream());
			connection.disconnect();

			result = connection.getResponseCode();
			LOG.debug("Upload Binary ended with: " + result);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Getter for the SoftwareModuleID
	 * @return the ID of the SoftwareModule
	 */
	String getSoftwareModuleID() {
		return softwareModuleID;
	}
}
