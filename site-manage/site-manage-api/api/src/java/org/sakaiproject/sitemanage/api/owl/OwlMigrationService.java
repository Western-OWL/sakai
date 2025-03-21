package org.sakaiproject.sitemanage.api.owl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service to handle business logic related to identifying sites that are to be migrated to Brightspace
 */
public interface OwlMigrationService {

	public static final String EVENT_OWL_MIGRATION_SELECTION_SAVED = "owl.migration.selection.save";

	/**
	 * The global configuration permitting the migration tab to appear
	 * @return true if the migration tab is enabled; false otherwise
	 */
	public boolean isMigrationTabEnabled();

	/**
	 * Gets a list of SiteMigrationItems.
	 * @return list of eligible {@link org.sakaiproject.sitemanage.api.owl.SiteMigrationItem SiteMigrationItems}, or an empty list if the user is not authorized to specify migration selections for any eligible sites.
	 */
	public List<SiteMigrationItem> getSiteMigrationItems();

	/**
	 * Gets a map of type keys to list of appropriate migration actions for that type.
	 * @return the map of type keys to {@link org.sakaiproject.sitemanage.api.owl.MigAction MigActions}
	 */
	public Map<String, List<MigAction>> getTypeActionMap();

	/**
	 * Maps migration type keys to display values. May contain types that are no longer active.
	 * @see getActiveTypes()
	 * @return a map of typeKey to UI value
	 */
	public Map<String, String> getMigrationTypes();

	/**
	 * Map active migration type keys to display values. If empty, UI should be read only
	 * @return a map of typeKey to UI value
	 */
	public Map<String, String> getActiveTypes();

	/**
	 * Maps migration action keys to display values. May contain actions that are no longer active.
	 * @see #getActiveActions()
	 * @return a map of actionKey to UI value
	 */
	public Map<String, String> getMigrationActions();

	/**
	 * Map active migration action keys to display values. If empty, UI should be read only
	 * @return a map of actionKey to UI value
	 */
	public Map<String, String> getActiveActions();

	/**
	 * If there are no active migration types, this value will be displayed for sites that are undecided in the read only UI
	 * @return optional wrapping the default migration type, or empty optional if the corresponding property couldn't be found
	 */
	public Optional<String> getNoActiveTypesDisplay();

	/**
	 * If there are no active migration actions, this value will be displayed for sites that are undecided in the read only UI
	 * @return optional wrapping the default migration action, or empty optional if the corresponding property couldn't be found
	 */
	public Optional<String> getNoActiveActionsDisplay();

	/**
	 * For sites that are eligible for migration and have changeable selections, persist their specified selection.
	 * Updates each site's selections, associated user, date, and sets an initial status if appropriate
	 * @param siteSelections a map of siteIds to {@link org.sakaiproject.sitemanage.api.owl.UserSelection UserSelection} objects
	 * @see org.sakaiproject.sitemanage.api.owl.UserSelection
	 * @return a list of siteIds whose migration selections could not be persisted
	 * Possible reasons a site's selection can't be persisted:
	 *     the current user is not a maintainer in the site,
	 *     the site's existing typeKey AND actionKey are not changeable nor active
	 * Any of these could result from bad data, or users working concurrently
	 * The nature of the error will be logged
	 */
	public List<String> saveSelections(Map<String, UserSelection> siteSelections);

	/**
	 * Get the list of action keys that are tied to resource size checks
	 * @return list of action keys that have resource size checks associated with it
	 */
	public List<String> getActionKeysWithResourcesSizeWarnings();

	/**
	 * Gets the display value associated with a status key.
	 * @param actionKey
	 * @param statusKey
	 *
	 * Statuses are visible only if both:
	 *    The actionKey is in the list of actions with visible status.
	 *    The statusKey is in the list of visible statuses.
	 * @return the display value corresponding to the statusKey, or empty String if the status is not visible
	 */
	public String getStatusDisplay(String actionKey, String statusKey);

	/**
	 * Get the default UI display name for Admin users
	 * @return optional wrapping the default display name, or empty optional if the corresponding property could not be found
	 */
	public Optional<String> getAdminDisplayName();

	/**
	 * Gets a UI message unsuitable to be managed by MBM
	 * @param messageKey
	 * @return the UI message corresponding to the given message key, or empty string if the message could not be found
	 */
	public String getUIMessage(String messageKey);
}
