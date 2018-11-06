package org.opentosca.nodetypeimplementations;

/**
 * Created by Marc Schmid on 20.09.18.
 */
public class Device {

	private String deviceID;
	private String assignedDS;

	public Device(String deviceID, String assignedDS){
		this.deviceID = deviceID;
		this.assignedDS = assignedDS;
	}


	public String getDeviceID() {
		return deviceID;
	}

	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
	}

	public String getAssignedDS() {
		return assignedDS;
	}

	public void setAssignedDS(String assignedDS) {
		this.assignedDS = assignedDS;
	}
}
