package org.sakaiproject.sitemanage.api.owl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service to handle business logic related to identifying courses that are to be migrated to Brightspace
 */
public interface OwlMigrationService {

	public static final String EVENT_OWL_MIGRATION_SELECTION_SAVED = "owl.migration.selection.save";

	/**
	 * The global configuration permitting the migration tab to appear
	 */
	public boolean isMigrationTabEnabled();

	/**
	 * Gets a list of SiteMigrationItems.
	 * Returns an empty list if the user is not authorized to specify migration selections for any eligible sites.
	 */
	public List<SiteMigrationItem> getSiteMigrationItems();

	/**
	 * Gets a map of type keys to list of appropriate migration actions for that type.
	 * @return the map
	 */
	public Map<String, List<MigAction>> getTypeActionMap();

	/**
	 * Maps migration type keys to display values.
	 * Iteration order is preserved: implementation is LinkedHashMap.
	 * May contain types that are no longer active.
	 * @see getActiveTypes()
	 */
	public Map<String, String> getMigrationTypes();

	/**
	 * Map active migration type keys to display values
	 * If empty, UI should be read only
	 */
	public Map<String, String> getActiveTypes();

	/**
	 * Maps migration action keys to display values.
	 * Iteration order is preserved: implementation is LinkedHashMap.
	 * May contain actions that are no longer active.
	 * @see getActiveActionss()
	 */
	public Map<String, String> getMigrationActions();

	/**
	 * Map active migration action keys to display values
	 * If empty, UI should be read only
	 */
	public Map<String, String> getActiveActions();

	/**
	 * If there are no active migration types, this value will be displayed for sites that are undecided in the read only UI
	 */
	public Optional<String> getNoActiveTypesDisplay();

	/**
	 * For sites that are eligible for migration and have changeable selections, persist their specified selection.
	 * Updates each site's selection, associated user, date, and sets an initial status if appropriate
	 * @param siteSelections a map of siteIds to selectionKeys
	 * @return a list of siteIds whose migration selections could not be persisted
	 * Possible reasons a site's selection can't be persisted:
	 *     the current user does not have an instructor enrollment in the site,
	 *     the site's existing selectionKey is not changeable,
	 *     the site's term is not eligible for migration,
	 *     the site's creation date is before the cutoff date, etc.
	 * Any of these could result from bad data, or users working concurrently
	 * The nature of the error will be logged
	 */
	public List<String> saveSelections(Map<String, UserSelection> siteSelections);

	public List<String> getActionKeysWithResourcesSizeWarnings();

	/**
	 * Gets the display value associated with a status key.
	 * @param actionKey
	 * @param statusKey
	 *
	 * Statuses are visible only if both:
	 *    The actionKey is in the list of actions with visible status.
	 *    The statusKey is in the list of visible statuses.
	 * @return empty String if the status is not visible
	 */
	public String getStatusDisplay(String actionKey, String statusKey);

	public Optional<String> getAdminDisplayName();

	/**
	 * Gets a UI message unsuitable to be managed by MBM
	 */
	public String getUIMessage(String messageKey);
}
