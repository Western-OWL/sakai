package org.sakaiproject.sitemanage.api.owl;

import java.util.Date;
import java.util.Optional;

import lombok.Data;

/**
 * Represents the migration selections (type and action) and status of a site
 */
@Data
public class SiteMigrationItem {

	public enum ResourcesSizeCategory {
		NONE,
		WARN,
		BRIGHTSPACE_LIMIT_EXCEEDED
	}

	private String siteId;
	private String siteTitle;
	private String siteUrl;

	/** The selected type option **/
	private String typeKey;
	private boolean isTypeEditable;
	private Optional<String> typeModifiedEid;
	private Optional<Date> typeModifiedDate;

	/** The selected action option **/
	private String actionKey;
	private boolean isActionEditable;
	private Optional<String> actionModifiedEid;
	private Optional<Date> actionModifiedDate;

	/** The migration status **/
	private String statusKey;
	private Optional<String> statusModifiedEid;
	private Optional<Date> statusModifiedDate;

	private String resourcesSize;
	private ResourcesSizeCategory resourcesSizeCategory;
}
