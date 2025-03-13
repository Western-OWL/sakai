package org.sakaiproject.sitemanage.api.owl;

import java.time.Instant;
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
	// OWLTODO: temp values
	private String actionKey = "fake1";
	private boolean isActionEditable = true;
	private Optional<String> actionModifiedEid = Optional.of("owlinstructor01");
	private Optional<Date> actionModifiedDate = Optional.of(Date.from(Instant.EPOCH));

	/** The migration status **/
	private String statusKey;
	private Optional<String> statusModifiedEid;
	private Optional<Date> statusModifiedDate;

	private String resourcesSize;
	private ResourcesSizeCategory resourcesSizeCategory;
}
