package org.sakaiproject.sitemanage.api.owl;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Models a migration action, including both key and label.
 */
@Data
@AllArgsConstructor
public class MigAction
{
	private String key;
	private String label;
}
