package org.sakaiproject.tool.gradebook.facades.owl;

import java.util.Map;
import java.util.Set;

import org.sakaiproject.authz.api.Role;

/**
 *
 * @author plukasew
 */
public interface OwlAuthz
{
	public boolean isUserAbleToSubmitCourseGrades(String userEid, String sectionEID, String siteID);

	public boolean isUserAbleToSubmitCourseGrades(String userEid, Set<Role> siteRoles, Map<String, String> userRoleMap);

	public boolean isUserAbleToApproveCourseGrades(String userEid, String sectionEID, String siteID);

	public boolean isUserAbleToApproveCourseGrades(String userEid, Set<Role> siteRoles, Map<String, String> userRoleMap);

	/**
	 * Simple check to determine if the user has basic submit or approve permissions in a site. Does not take into
	 * account sections or roster roles.
	 * @param userId the user uuid
	 * @param siteId the site id
	 * @return true if the user any submit or approve permissions for the given site
	 */
	public boolean isUserAbleToSubmitOrApproveInSite(String userId, String siteId);

	public Map<String, String> getSectionUserRoleMap(String sectionEid);

	public Set<Role> getSiteRolesForGradebook(String gradebookUid);
}
