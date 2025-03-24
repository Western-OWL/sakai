package org.sakaiproject.site.tool.owl.migration;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sakaiproject.cheftool.Context;
import org.sakaiproject.cheftool.RunData;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.event.api.SessionState;
import org.sakaiproject.sitemanage.api.owl.DisplayConstants;
import org.sakaiproject.sitemanage.api.owl.MigAction;
import org.sakaiproject.sitemanage.api.owl.OwlMigrationService;
import org.sakaiproject.sitemanage.api.owl.SiteMigrationItem;
import org.sakaiproject.sitemanage.api.owl.UserSelection;
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
	 * @param context the Velocity context
	 * @param state the session state
	 * @param template the name of the vm template
	 * @param rb the message bundle
	 * @param sites list of sites belonging that are eligible for migration
	 * @return
	 */
	public static String buildMigrationContext(Context context, SessionState state, String template, ResourceLoader rb, List<SiteMigrationItem> sites)
	{
		context.put("siteList", sites);
		boolean editableOptions = sites.stream().anyMatch(smi -> smi.isTypeEditable() || smi.isActionEditable());
		context.put("hasEditableSites", editableOptions); // true if any of the sites are in an editable state (ie. "undecided")

		context.put("typesDisplayMap", OWL_MIG_SERV.getMigrationTypes()); // map of all types key -> display value
		var activeTypes = OWL_MIG_SERV.getActiveTypes(); // map of currently active types keys -> display value
		context.put("types", activeTypes); // the possible selections in the migration options dropdown
		context.put("actionsDisplayMap", OWL_MIG_SERV.getMigrationActions());
		var activeActions = OWL_MIG_SERV.getActiveActions();
		context.put("actions", activeActions);
		
		context.put("readOnlyMode", activeTypes.isEmpty() && activeActions.isEmpty()); // shorthand for no active types or actions (tab is effectively in a read-only mode)
		context.put("readOnlyNoTypeSelectionDisplay", OWL_MIG_SERV.getNoActiveTypesDisplay()); // value to display in type column when in read-only mode and no user selection has been made
		context.put("readOnlyNoActionSelectionDisplay", OWL_MIG_SERV.getNoActiveActionsDisplay()); // value to display in action column when in read-only mode and no user selection has been made

		context.put("typeActionMap", OWL_MIG_SERV.getTypeActionMap());

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
		String js = OWL_MIG_SERV.getActionKeysWithResourcesSizeWarnings().stream().collect(Collectors.joining("','"));
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

	public static List<SiteMigrationItem> getSiteMigrationItems()
	{
		return OWL_MIG_SERV.getSiteMigrationItems();
	}

	public static Map<String, List<MigAction>> getMigrationActions()
	{
		return OWL_MIG_SERV.getTypeActionMap();
	}

	/**
	 * Sends the updated migration options for the eligible sites to the service for processing/persisting.
	 * @param data the rundata
	 * @param state the session state
	 * @return a list of site titles that could not be updated (for display in the error message)
	 */
	public static List<String> updateMigrations(RunData data, SessionState state)
	{
		// "userType" and "userAction" are the names of the <select> form inputs
		var types = data.getParameters().getStrings("userType") == null ? List.<String>of() : List.of(data.getParameters().getStrings("userType"));
		var actions = data.getParameters().getStrings("userAction") == null ? List.<String>of() : List.of(data.getParameters().getStrings("userAction"));

		// these are siteId::key Strings
		var typeMap = types.stream().map(s -> s.split("::")).filter(a -> a.length == 2).collect(Collectors.toMap(a -> a[0], a -> a[1]));
		var actMap = actions.stream().map(s -> s.split("::")).filter(a -> a.length == 2).collect(Collectors.toMap(a -> a[0], a -> a[1]));

		var siteIds = Stream.concat(typeMap.keySet().stream(), actMap.keySet().stream()).collect(Collectors.toSet());

		var selMap = siteIds.stream().collect(Collectors.toMap(s -> s, s -> new UserSelection(Optional.ofNullable(typeMap.get(s)), Optional.ofNullable(actMap.get(s)))));

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
