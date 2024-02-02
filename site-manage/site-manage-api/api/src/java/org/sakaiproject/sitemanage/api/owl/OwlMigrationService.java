package org.sakaiproject.sitemanage.api.owl;

import java.util.List;
import java.util.Map;

/**
 * Service to handle business logic related to identifying courses that are to be migrated to Brightspace
 */
public interface OwlMigrationService {

	/**
	 * The global configuration permitting the migration tab to appear
	 */
	public boolean isMigrationTabEnabled();

	/**
	 * TODO: rip this out
	 * List of common terms (E.g. "Fall/Winter 2024"), including project sites. This is useful to provide an ordering in the UI
	 * @Derecated - I don't think this is needed as the keys of getSiteMigrationItems() are ordered. 
	 */
	@Deprecated
	public List<String> getCommonTerms();

	/**
	 * Gets a map of groups to SiteMigrationItems.
	 * Groups can be common term names like "Fall/Winter 2024" for eligible academic sessions, or "Project Sites" if eligible.
	 * Returns an empty map if the user is not authorized to specify migration selections for any eligible sites.
	 */
	public Map<String, List<SiteMigrationItem>> getSiteMigrationItems();


	/**
	 * Maps migration option keys to display values
	 * Iteration order is preserved: implementation is LinkedHashMap
	 */
	public Map<String, String> getMigrationOptions();

	/**
	 * If the site doesn't already have a selection, persists the siteMigrationItem with the specified selection.
	 * Updates the selection's associated user, date, and sets an initial status if appropriate
	 */
	public void saveSelection(String siteId, String selectionKey);

	/**
	 * Gets the display value associated with a status key.
	 * @param selectionKey
	 * @param statusKey
	 *
	 * Statuses are visible only if both:
	 *    The selectionKey is in the list of selections with visible status.
	 *    The statusKey is in the list of visible statuses.
	 * @return empty String if the status is not visible
	 */
	public String getStatusDisplay(String selectionKey, String statusKey);

	/**
	 * Gets a UI message unsuitable to be managed by MBM
	 */
	public String getUIMessage(String messageKey);
}
