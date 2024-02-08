package org.sakaiproject.sitemanage.api.owl;

import java.util.Date;
import java.util.Optional;

import lombok.Data;

/**
 * Represents the migration selection and status of a site
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

	/** The selected migration option **/
	private String selectionKey;
	private boolean isSelectionEditable;
	private Optional<String> selectionModifiedEid;
	private Optional<Date> selectionModifiedDate;

	/** The migration status **/
	// TODO: not optional - just use "" when not present
	private String statusKey;
	private Optional<String> statusModifiedEid;
	private Optional<Date> statusModifiedDate;

	private String resourcesSize;
	private ResourcesSizeCategory resourcesSizeCategory;

	// Not user facing, but useful for ordering sites within groups
	private String academicSessionEid;
}
