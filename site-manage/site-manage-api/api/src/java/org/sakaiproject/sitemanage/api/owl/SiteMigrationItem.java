package org.sakaiproject.sitemanage.api.owl;

import java.util.Date;
import java.util.Optional;

import lombok.Data;

/**
 * Represents the migration selection and status of a site
 */
@Data
public class SiteMigrationItem {
	
	public String siteId;
	public String siteTitle;

	/** The selected migration option **/
	public String selectionKey;
	public boolean isSelectionEditable;
	public Optional<String> selectionModifiedEid;
	public Optional<Date> selectionModifiedDate;

	/** The migration status **/
	// TODO: not optional - just use "" when not present
	public String statusKey;
	public Optional<String> statusModifiedEid;
	public Optional<Date> statusModifiedDate;

	public float resourcesSize;
}
