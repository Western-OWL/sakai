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

	/** The selected migration option **/
	public Optional<String> selectionKey;
	public Optional<String> selectionModifiedEid;
	public Optional<Date> selectionModifiedDate;

	/** The migration status **/
	public Optional<String> statusKey;
	public Optional<String> statusModifiedEid;
	public Optional<Date> statusModifiedDate;

}
