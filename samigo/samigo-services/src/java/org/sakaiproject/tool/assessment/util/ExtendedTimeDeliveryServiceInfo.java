package org.sakaiproject.tool.assessment.util;

import java.util.Collection;
import lombok.Data;

/**
 * Used to reduce the number of repeated queries when constructing ExtendedTimeDeliveryService instances.
 */
@Data
public class ExtendedTimeDeliveryServiceInfo {

	private boolean groupsKnown;
	private String userId;
	private Collection<String> groupIDs;

	/** No benefit for default c'tor **/
	private ExtendedTimeDeliveryServiceInfo() {}

	public ExtendedTimeDeliveryServiceInfo(String userId) {
		this.userId = userId;
		groupsKnown = false;
	}

	public ExtendedTimeDeliveryServiceInfo(String userId, Collection<String> groupIDs) {
		this.userId = userId;
		this.groupIDs = groupIDs;
		groupsKnown = true;
	}

	public void setGroupIDs(Collection<String> groupIDs) {
		this.groupIDs = groupIDs;
		groupsKnown = true;
	}
}
