package org.sakaiproject.tool.gradebook.facades.sakai2impl.owl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.tool.gradebook.facades.owl.OwlAuthz;
import org.sakaiproject.authz.cover.FunctionManager;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.site.api.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.coursemanagement.api.Membership;
import org.sakaiproject.coursemanagement.api.exception.IdNotFoundException;
import org.sakaiproject.emailtemplateservice.api.EmailTemplateService;

/**
 *
 * @author plukasew
 */
public class OwlAuthzImpl implements OwlAuthz
{
	private static final Logger log = LoggerFactory.getLogger(OwlAuthzImpl.class);

	public static final String PERMISSION_SUBMIT_COURSE_GRADES = "gradebook.submitCourseGrades";
	public static final String PERMISSION_APPROVE_COURSE_GRADES = "gradebook.approveCourseGrades";

	private static final String TMPLT_KEY_SFTP_ERROR	= "gradebook.courseGradeSubmission.sftpError";	// The name (key) of the sftp error template
	private static final String TMPLT_KEY_SUB_NOTICE	= "gradebook.courseGradeSubmission.submissionNotice";	// the name (key) of the submission notice tmeplate
	private static final String TMPLT_KEY_SUB_RECEIPT	= "gradebook.courseGradeSubmission.submissionReceipt";	// The name (key) of the submission receipt template

	// OWL-797 (mweston4) Email instructor(s) when grades are approved for their section
	private static final String TMPLT_KEY_APPROVE_NOTICE = "gradebook.courseGradeSubmission.approvalNotice"; // The name (key) of the approval notice template
	private static final String TMPLT_KEY_APPROVE_NOTICE_NO_CHANGE = "gradebook.courseGradeSubmission.approvalNotice.noChange"; // The name (key) of the approval notice template when grades approval is repeated

	private CourseManagementService cms;
	private SiteService siteServ;
	private SecurityService secServ;

	public void init(Collection registeredPerms)
	{
		cms = (CourseManagementService) ComponentManager.get(CourseManagementService.class);
		siteServ = (SiteService) ComponentManager.get(SiteService.class);
		secServ = (SecurityService) ComponentManager.get(SecurityService.class);

		// register permissions
		if (!registeredPerms.contains(PERMISSION_APPROVE_COURSE_GRADES))
		{
			FunctionManager.registerFunction(PERMISSION_APPROVE_COURSE_GRADES);
		}
		if (!registeredPerms.contains(PERMISSION_SUBMIT_COURSE_GRADES))
		{
			FunctionManager.registerFunction(PERMISSION_SUBMIT_COURSE_GRADES);
		}

		// register email templates
		// bjones86 - course grade submission email templates
		EmailTemplateService ets = (EmailTemplateService) ComponentManager.get(EmailTemplateService.class);
		ClassLoader cl = OwlAuthzImpl.class.getClassLoader();
		ets.importTemplateFromXmlFile(cl.getResourceAsStream("courseGradeSubmission_SftpError.xml"), TMPLT_KEY_SFTP_ERROR);
		ets.importTemplateFromXmlFile(cl.getResourceAsStream("courseGradeSubmission_SubmissionNotice.xml"), TMPLT_KEY_SUB_NOTICE);
		ets.importTemplateFromXmlFile(cl.getResourceAsStream("courseGradeSubmission_SubmissionReceipt.xml"), TMPLT_KEY_SUB_RECEIPT);

		// OWL-697 (mweston4) Email instructor(s) when grades are approved for their section
		ets.importTemplateFromXmlFile(cl.getResourceAsStream("courseGradeSubmission_ApprovalNotice.xml"), TMPLT_KEY_APPROVE_NOTICE);
		ets.importTemplateFromXmlFile(cl.getResourceAsStream("courseGradeSubmission_ApprovalNotice_NoChange.xml"), TMPLT_KEY_APPROVE_NOTICE_NO_CHANGE);
	}

	/**
	 * This method is slow and should not be used in loops!
	 * @param userEID
	 * @param sectionEID
	 * @param siteID
	 * @return
	 */
	@Override
	public boolean isUserAbleToSubmitCourseGrades(String userEID, String sectionEID, String siteID)
	{
		return checkUsersSectionRoleForPermission(userEID, sectionEID, siteID, PERMISSION_SUBMIT_COURSE_GRADES);
	}

	/**
	 * Faster version of the above method, but it requires more pre-requisites
	 * @param userEid
	 * @param siteRoles
	 * @param userRoleMap
	 * @return
	 */
	@Override
	public boolean isUserAbleToSubmitCourseGrades(String userEid, Set<org.sakaiproject.authz.api.Role> siteRoles, Map<String, String> userRoleMap)
	{
		return checkUsersSectionRoleForPermission(userEid, siteRoles, userRoleMap, PERMISSION_SUBMIT_COURSE_GRADES);
	}

	/**
	 * This method is slow and should not be used in loops!
	 * @param userEID
	 * @param sectionEID
	 * @param siteID
	 * @return
	 */
	@Override
	public boolean isUserAbleToApproveCourseGrades(String userEID, String sectionEID, String siteID)
	{
		return checkUsersSectionRoleForPermission(userEID, sectionEID, siteID, PERMISSION_APPROVE_COURSE_GRADES);
	}

	/**
	 * Faster version of the above method, but it requires more pre-requisites
	 * @param userEid
	 * @param siteRoles
	 * @param userRoleMap
	 * @return
	 */
	@Override
	public boolean isUserAbleToApproveCourseGrades(String userEid, Set<org.sakaiproject.authz.api.Role> siteRoles, Map<String, String> userRoleMap)
	{
		return checkUsersSectionRoleForPermission(userEid, siteRoles, userRoleMap, PERMISSION_APPROVE_COURSE_GRADES);
	}

	@Override
	public boolean isUserAbleToSubmitOrApproveInSite(String userId, String siteId)
	{
		try
		{
			String siteRef = siteServ.getSite(siteId).getReference();
			return secServ.unlock(userId, PERMISSION_APPROVE_COURSE_GRADES, siteRef) || secServ.unlock(userId, PERMISSION_SUBMIT_COURSE_GRADES, siteRef);
		}
		catch (IdUnusedException e)
		{
			log.error(e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Utility method to check the given user's section role for the given permission
	 * @author bjones86 - OWL-873
	 * @param userEID the user in question
	 * @param sectionEID the section the user belongs to
	 * @param siteID the site the section belongs to
	 * @param permission the permission to check for
	 * @return true/false
	 */
	@SuppressWarnings("deprecation")
	private boolean checkUsersSectionRoleForPermission(String userEID, String sectionEID, String siteID, String permission)
	{
		boolean allowed = false;
		try
		{
			// Get the section, user-role map for the section, all site roles for the site
			Map<String, String> userRoleMap = cms.getSectionMemberships(sectionEID).stream()
					.collect(Collectors.toMap(Membership::getUserId, Membership::getRole));

			Site site = siteServ.getSite(siteID);
			Set<org.sakaiproject.authz.api.Role> siteRoles = site.getRoles();

			allowed = checkUsersSectionRoleForPermission(userEID, siteRoles, userRoleMap, permission);
		}
		catch (IdNotFoundException ex)
		{
			log.error("OWL: section ID not found: " + sectionEID);
		}
		catch (IdUnusedException ex)
		{
			log.error("OWL: site ID unused: " + siteID);
		}

		return allowed;
	}

	// faster version of the above method that requires objects up front to avoid costly repeated lookups of CM data
	private boolean checkUsersSectionRoleForPermission(String userEID, Set<org.sakaiproject.authz.api.Role> siteRoles, Map<String, String> userRoleMap, String permission)
	{
		boolean allowed = false;

		org.sakaiproject.authz.api.Role sectionRole = null;
		String sectionRoleId = userRoleMap.get(userEID);
		if (sectionRoleId != null)
		{
			sectionRoleId = SectionRole.fromCmsRole(sectionRoleId).map(sr -> sr.siteRole).orElse("");
			sectionRole = findMatchingRole(siteRoles, sectionRoleId);
		}

		// If the section role has the permission, allowed = true
		if (sectionRole != null && sectionRole.getAllowedFunctions().contains(permission))
		{
			allowed = true;
		}

		return allowed;
	}

	/**
	 * Utility method to find the Role object that matches the given roleID from the provided set of Roles
	 * @author bjones86 - OWL-873
	 * @param allRoles set of unique role Objects
	 * @param roleID the ID of the Role object to return (if found in the set)
	 * @return the matching Role object if found, or null
	 */
	private org.sakaiproject.authz.api.Role findMatchingRole(Set<org.sakaiproject.authz.api.Role> allRoles, String roleID)
	{
		org.sakaiproject.authz.api.Role match = null;
		for (org.sakaiproject.authz.api.Role role : allRoles)
		{
			if (role.getId().equals(roleID))
			{
				match = role;
				break;
			}
		}

		return match;
	}

	@Override
	public Map<String, String> getSectionUserRoleMap(String sectionEid)
	{
		return cms.getSectionMemberships(sectionEid).stream().collect(Collectors.toMap(Membership::getUserId, Membership::getRole));
	}

	@Override
	public Set<org.sakaiproject.authz.api.Role> getSiteRolesForGradebook(String gradebookUid)
	{
		try
		{
			return siteServ.getSite(gradebookUid).getRoles();
		}
		catch (IdUnusedException e)
		{
			return Collections.emptySet();
		}
	}

	/**
	 * This maps CMS (Sakora) role ids to their matching site role. It must be kept in sync with our mapping
	 * defined in components.xml in the providers project.
	 *
	 * Rationale: We used to depend on SectionRoleResolver from the providers impl to do this mapping. There
	 * are no available API methods in either the CMS or the SectionManager to get section roles. The purpose
	 * of this enum is to break the dependency on an impl class at the cost of having to maintain parity between
	 * the enum and components.xml in providers. However, the mapping has never changed and is unlikely to change
	 * in the future, so the risk is low.
	 */
	public enum SectionRole
	{
		Instructor("I", "Instructor"), Student("S", "Student"), GradeAdmin("GA", "Grade Admin"),
		CourseCoordinator("CC", "Course Coordinator"), TA("GSI", "Teaching Assistant"), Auditor("A", "Auditor");

		public final String cmsRole, siteRole;
		SectionRole(String cmsRole, String siteRole)
		{
			this.cmsRole = cmsRole;
			this.siteRole = siteRole;
		}

		public static Optional<SectionRole> fromCmsRole(String role)
		{
			return Arrays.stream(SectionRole.values()).filter(r -> r.cmsRole.equals(role)).findFirst();
		}
	}
}
