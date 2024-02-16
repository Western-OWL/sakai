package org.sakaiproject.site.tool.owl.migration;

import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.sitemanage.api.owl.OwlMigrationService;
import org.sakaiproject.sitemanage.api.owl.SiteMigrationItem;
import org.sakaiproject.time.api.UserTimeService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

/**
 * Utility for working with Velocity
 * @author plukasew
 */
public class MigTool
{
	private static final OwlMigrationService OWL_MIG_SERV = ComponentManager.get(OwlMigrationService.class);
	private static final UserTimeService UTS = ComponentManager.get(UserTimeService.class);
	private static final UserDirectoryService UDS = ComponentManager.get(UserDirectoryService.class);
	private static final SecurityService SS = ComponentManager.get(SecurityService.class);

	private MigTool()
	{
		// utility class
	}

	/**
	 * Gets the display string for the given site's status
	 * @param item the site
	 * @return the display string for the status, or empty string if no status or status should not be displayed
	 */
	public static String getStatusDisplay(SiteMigrationItem item)
	{
		return OWL_MIG_SERV.getStatusDisplay(item.getSelectionKey(), item.getStatusKey());
	}

	/**
	 * Formats the given date (if it exists) for display using standard Sakai formatting, or shows the placeholder value if no date
	 * @param date the date (optional)
	 * @param locale the user's locale
	 * @param noValue the placeholder value
	 * @return the date formatted for display to the current user, or a placeholder
	 */
	public static String formatDate(Optional<Date> date, Locale locale, String noValue)
	{
		return date.map(d -> UTS.shortLocalizedTimestamp(d.toInstant(), locale)).orElse(noValue);
	}

	/**
	 * Formats the given instant for display using standard Sakai formatting
	 * @param instant the instant
	 * @param locale the user's locale
	 * @return the instant formatted for display to the current user
	 */
	public static String formatDate(Instant instant, Locale locale)
	{
		return formatDate(Optional.of(Date.from(instant)), locale, "");
	}

	/**
	 * Formats the given user eid (if it exists) for display, or shows placeholder if no eid
	 * @param eid the user eid
	 * @param noValue placeholder value
	 * @return the user's display name, or a generic name if user eid belong to an OWL admin, or the placeholder if no eid exists
	 */
	public static String formatUser(Optional<String> eid, String noValue)
	{
		if (!eid.isPresent())
		{
			return noValue;
		}
		
		User u;
		try
		{
			u = UDS.getUserByEid(eid.get());
		}
		catch (UserNotDefinedException e)
		{
			return eid.get();
		}
		return SS.isSuperUser(u.getId()) ? OWL_MIG_SERV.getAdminDisplayName().orElse(noValue) : u.getDisplayName();
	}

	/**
	 * Gets the CSS class appropriate for the site's size categorization
	 * @param item the site
	 * @return the CSS class appropriate for the site, or empty string if the site doesn't require it
	 */
	public static String getSizeCssClass(SiteMigrationItem item)
	{
		switch (item.getResourcesSizeCategory())
		{
			case WARN:
				return "owl-mig-tracking-size-label-warn";
			case BRIGHTSPACE_LIMIT_EXCEEDED:
				return "owl-mig-tracking-size-label-error";
			default:
				return "";
		}
	}

	/**
	 * Formats the site size estimate for display
	 * @param item the site
	 * @return the estimated site size with the appropriate unit
	 */
	public static String formatSize(SiteMigrationItem item)
	{
		return item.getResourcesSize() + " GB";
	}

	/**
	 * Gets the display text for the given site's selection
	 * @param item the site
	 * @param displayMap the map of selection keys to display text
	 * @return the display value for the key, or the key itself if not found in the map
	 */
	public static String getSelectionDisplay(SiteMigrationItem item, Map<String, String> displayMap)
	{
		String key = item.getSelectionKey();
		String display = displayMap.get(item.getSelectionKey());
		return display == null ? key : display;
	}
}
