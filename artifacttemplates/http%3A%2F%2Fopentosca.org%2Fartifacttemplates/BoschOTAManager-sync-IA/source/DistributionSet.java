package org.opentosca.nodetypeimplementations;

/**
 * Created by Marc Schmid on 20.09.18.
 */
public class DistributionSet {

	private String distributionSetID;

	public DistributionSet(String distributionSetID){
		this.distributionSetID = distributionSetID;
	}


	public String getDistributionSetID() {
		return distributionSetID;
	}

	public void setDistributionSetID(String distributionSetID) {
		this.distributionSetID = distributionSetID;
	}
}
