package org.sakaiproject.site.tool.owl.migration;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sakaiproject.cheftool.Context;
import org.sakaiproject.cheftool.RunData;
import org.sakaiproject.cheftool.VelocityPortlet;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.event.api.SessionState;
import org.sakaiproject.sitemanage.api.owl.DisplayConstants;
import org.sakaiproject.sitemanage.api.owl.OwlMigrationService;
import org.sakaiproject.sitemanage.api.owl.SiteMigrationItem;
import org.sakaiproject.util.ResourceLoader;

/**
 * Helper class (delegate) for MembershipAction to handle the OWL Migration Tracking feature.
 * @author plukasew
 */
public class OwlMigrationHelper
{
	public static final String STATE_OWL_MIG_SUCCESS_DATE = "owl_mig_success_date";

	private static final OwlMigrationService OWL_MIG_SERV = ComponentManager.get(OwlMigrationService.class);

	private OwlMigrationHelper()
	{
		// utility class
	}

	/**
	 * Builds the Velocity context for the OWL Migration Tracking tab in the Membership tool.
	 * @param portlet the portlet
	 * @param context the Velocity context
	 * @param runData the rundata
	 * @param state the session state
	 * @param template the name of the vm template
	 * @param rb the message bundle
	 * @param termMap a map of "common term" (ie. Summer 2023) to sites belonging to that term that are eligible for migration
	 * @return
	 */
	public static String buildMigrationContext(VelocityPortlet portlet, Context context, RunData runData, SessionState state, String template,
			ResourceLoader rb, Map<String, List<SiteMigrationItem>> termMap)
	{
		context.put("termMap", termMap);
		boolean editableOptions = termMap.values().stream().flatMap(Collection::stream).anyMatch(smi -> smi.isSelectionEditable());
		context.put("hasEditableSites", editableOptions); // true if any of the sites are in an editable state (ie. "undecided")

		context.put("optionsDisplayMap", OWL_MIG_SERV.getMigrationOptions()); // map of all selections key -> display value
		Map<String, String> activeOptions = OWL_MIG_SERV.getActiveOptions(); // map of currently active selections keys -> display value
		context.put("options", activeOptions); // the possible selections in the migration options dropdown
		context.put("readOnlyMode", activeOptions.isEmpty()); // shorthand for no active options (tab is effectively in a read-only mode)
		context.put("readOnlyNoSelectionDisplay", OWL_MIG_SERV.getNoActiveOptionsDisplay()); // value to display in options column when in read-only mode and no user selection has been made

		context.put("tlang", rb);

		context.put("MigTool", MigTool.class);

		// the optional message blurbs that are sourced from the service instead of directly from message bundles
		// the "banner" blurbs are at the top of the page
		// the "confirm" blurbs are in the confirmation panel that appears after clicking "Continue"
		// "confirm2" is a special blurb that relates to site size warnings
		context.put("banner1", OWL_MIG_SERV.getUIMessage(DisplayConstants.BANNER_TOP_1));
		context.put("banner2", OWL_MIG_SERV.getUIMessage(DisplayConstants.BANNER_TOP_2));
		context.put("banner3", OWL_MIG_SERV.getUIMessage(DisplayConstants.BANNER_TOP_3));
		context.put("confirm1", OWL_MIG_SERV.getUIMessage(DisplayConstants.SAVE_CONFIRM_1));
		context.put("confirm2", OWL_MIG_SERV.getUIMessage(DisplayConstants.SAVE_CONFIRM_2));
		context.put("confirm3", OWL_MIG_SERV.getUIMessage(DisplayConstants.SAVE_CONFIRM_3));

		// a "success date" is stored in the state when migration options are saved successfully
		// this date, if it exists, is displayed in a success banner
		// this has to be tracked separately from alerts because there is support for only
		// one alert type, which we are using already for errors
		Instant successDate = (Instant) state.getAttribute(STATE_OWL_MIG_SUCCESS_DATE);
		if (successDate != null)
		{
			context.put("successDate", successDate);
		}

		// build string for JS array literal used to help determine when to display site size warnings
		String js = OWL_MIG_SERV.getSelectionKeysWithResourcesSizeWarnings().stream().collect(Collectors.joining("','"));
		context.put("sizeCheckOptions", js.isBlank() ? "" : "'" + js + "'");
		context.put("showSizeCol", !js.isBlank()); // if there are no selections that trigger size check, hide the size column

		return template + "_migration"; // the actual name of the vm file for this tab
	}

	/**
	 * Determines whether the migration tracking feature is enabled globally
	 * @return true if migration feature enabled
	 */
	public static boolean migrationEnabled()
	{
		return OWL_MIG_SERV.isMigrationTabEnabled();
	}

	public static Map<String, List<SiteMigrationItem>> getSiteMigrationItems()
	{
		return OWL_MIG_SERV.getSiteMigrationItems();
	}

	/**
	 * Sends the updated migration options for the eligible sites to the service for processing/persisting.
	 * @param data the rundata
	 * @param state the session state
	 * @return a list of site titles that could not be updated (for display in the error message)
	 */
	public static List<String> updateMigrations(RunData data, SessionState state)
	{
		// "userSelection" is the name of the <select> form inputs
		List<String> selections = List.of(data.getParameters().getStrings("userSelection"));

		// these are siteId::selectionKey Strings, so we have to split on :: and then make the appropriate service method call to save them
		Map<String, String> selMap = selections.stream().map(s -> s.split("::")).filter(a -> a.length == 2).collect(Collectors.toMap(a -> a[0], a -> a[1]));
		List<String> errorSites = OWL_MIG_SERV.saveSelections(selMap);
		if (errorSites.isEmpty())
		{
			// no errors, set the success date state attribute which is our substitute for a success alert
			// (alerts can only be of one type, and we are using them for errors)
			state.setAttribute(STATE_OWL_MIG_SUCCESS_DATE, Instant.now());
		}
		else
		{
			cleanMigrationState(state); // clear the success date from the state, if there was one
		}

		return errorSites;
	}

	public static void cleanMigrationState(SessionState state)
	{
		state.removeAttribute(STATE_OWL_MIG_SUCCESS_DATE);
	}
}
