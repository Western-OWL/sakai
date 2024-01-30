package org.sakaiproject.sitemanage.api.owl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service to handle business logic related to identifying courses that are to be migrated to Brightspace
 */
public interface OwlMigrationService {

	public boolean isMigrationTabEnabled();

	/**
	 * Gets a map of common terms (I.e. groupings of eligible academic sessions), to SiteMigrationItems
	 * Returns an empty map if the user is not authorized to specify migration selections for any sites.
	 */
	public Map<String, List<SiteMigrationItem>> getSiteMigrationItems();


	public List<MigrationOption> getMigrationOptions();

	/**
	 * Persists the siteMigrationItem with the specified selection; updates the selection's associated user, date, and sets an initial status
	 */
	public void saveSelection(SiteMigrationItem siteMigrationItem, String selectionKey);

	/**
	 * Gets the display value associated with a status key.
	 * @param selectionKey
	 * @param statusKey
	 *
	 * Statuses are visible only if both:
	 *    The selectionKey is in the list of selections with visible status.
	 *    The statusKey is in the list of visible statuses.
	 * @return empty if the status is not visible
	 */
	public Optional<String> getStatusDisplay(String selectionKey, String statusKey);

	/**
	 * Gets a UI message unsuitable to be managed by MBM
	 */
	public String getUIMessage(String messageKey);

}
