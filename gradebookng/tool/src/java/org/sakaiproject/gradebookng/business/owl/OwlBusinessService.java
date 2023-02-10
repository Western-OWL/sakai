package org.sakaiproject.gradebookng.business.owl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.coursemanagement.api.Membership;
import org.sakaiproject.coursemanagement.api.Section;
import org.sakaiproject.coursemanagement.api.exception.IdNotFoundException;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.SortDirection;
import org.sakaiproject.gradebookng.business.model.GbGroup;
import org.sakaiproject.gradebookng.business.model.GbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.model.GbUser;
import org.sakaiproject.gradebookng.business.owl.anon.OwlAnonGradingService;
import org.sakaiproject.gradebookng.business.owl.anon.OwlAnonGradingService.AnonIDComparator;
import org.sakaiproject.gradebookng.business.owl.finalgrades.OwlFinalGradesService;
import org.sakaiproject.section.api.coursemanagement.CourseSection;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.sakaiproject.tool.gradebook.facades.owl.OwlAuthz;
import org.sakaiproject.user.api.CandidateDetailProvider;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

/**
 * Entry point for the OWL business services (final grades and anon grading). Also contains methods that are generally useful.
 * @author plukasew
 */
@Slf4j
public class OwlBusinessService
{
	private static final GbGroup ALL_GROUP = new GbGroup("", "", "", GbGroup.Type.ALL); // not localized, do not use for presentation layer

	public final OwlFinalGradesService fg;
	public final OwlAnonGradingService anon;

	private final GradebookNgBusinessService bus;
	private final GradebookService gbs;
	private final CourseManagementService cms;
	private final UserDirectoryService uds;
	private final CandidateDetailProvider cdp;

	public OwlBusinessService(GradebookNgBusinessService bus, GradebookService gbs, CourseManagementService cms,
			UserDirectoryService uds, CandidateDetailProvider cdp)
	{
		this.bus = bus;
		this.gbs = gbs;
		this.cms = cms;
		this.uds = uds;
		this.cdp = cdp;
		fg = new OwlFinalGradesService(bus, gbs, cms);
		anon = new OwlAnonGradingService(bus, gbs);
	}

	public OwlAuthz owlAuthz()
	{
		return gbs.owlAuthz();
	}

	public List<OwlGbStudentGradeInfo> buildGradeMatrixForImportExport(final List<Assignment> assignments, GbGroup groupFilter, boolean isContextAnonymous)
	{
		List<GbStudentGradeInfo> matrix = bus.buildGradeMatrixForImportExport(assignments, isContextAnonymous ? ALL_GROUP : groupFilter);
		List<OwlGbStudentGradeInfo> items = anon.owlify(matrix, isContextAnonymous);

		// OWLTODO: what about pure anon course grades? those would be filtered out at the UI layer anyway BUT
		// the underlying buildGradeMatrixForImportExport should have a flag to include/exclude course grades anyway
		// because they would only appear in a custom export

		if (isContextAnonymous)
		{
			sort(items, new AnonIDComparator(), SortDirection.ASCENDING);
		}

		return items;
	}

	public void sort(List<OwlGbStudentGradeInfo> items, Comparator<OwlGbStudentGradeInfo> comp, SortDirection sort)
	{
		Collections.sort(items, sort == SortDirection.ASCENDING ? comp : Collections.reverseOrder(comp));
	}

	public List<GbGroup> getSiteSections()
	{
		Optional<Site> site = bus.getCurrentSite();
		if (!site.isPresent())
		{
			return Collections.emptyList();
		}

		return getSiteSections(site.get());
	}

	public List<GbGroup> getSiteSections(Site site)
	{
		List<GbGroup> groups = bus.getSiteSectionsAndGroups();
		List<GbGroup> providedGroups = new ArrayList<>();
		List<String> secTitles = new ArrayList<>();
		for (GbGroup g : groups)
		{
			Optional<String> secEid = bus.owl().findSectionEid(g);
			if (secEid.isPresent())
			{
				try
				{
					Section sec = cms.getSection(secEid.get());
					providedGroups.add(g);
					secTitles.add(sec.getTitle());
				}
				catch (IdNotFoundException e)
				{
					// crosslist or invalid eid, ignore and continue
				}
			}
		}

		// filter out the non-section provided groups based on a simple title comparison
		return providedGroups.stream().filter(g -> secTitles.contains(g.getTitle())).collect(Collectors.toList());
	}

	/**
	 * Finds the section eid of a group
	 * @param group the group representing the section, can be null
	 * @return the section eid, if found
	 */
	public Optional<String> findSectionEid(final GbGroup group)
	{
		if (group == null)
		{
			return Optional.empty();
		}

		return bus.getCurrentSite().flatMap(s -> Optional.ofNullable(s.getGroup(group.getId())))
				.flatMap(g -> Optional.ofNullable(g.getProviderGroupId()));
	}

	public List<CourseSection> getViewableSections(final Gradebook gradebook)
	{
		return gbs.getViewableSections(gradebook);
	}

	public List<CourseSection> getViewableSections()
	{
		return gbs.getViewableSections(bus.getGradebook().getUid());
	}

	public Set<String> getViewableSectionEids()
	{
		return getViewableSectionEids(bus.getGradebook());
	}

	public Set<String> getViewableSectionEids(final Gradebook gradebook)
	{
		return getViewableSections(gradebook).stream().filter(Objects::nonNull).map(s -> s.getEid())
				.filter(e -> StringUtils.isNotBlank(e)).collect(Collectors.toSet());
	}

	/**
	 * Get the user given an eid. Does not attempt to acquire student number or anon id.
	 *
	 * @param eid
	 * @return Optional<GbUser>, empty if not found
	 */
	public Optional<OwlGbUser> getNonStudentUserByEid(final String eid)
	{
		try
		{
			final User u = uds.getUserByEid(eid);
			return Optional.of(OwlGbUser.fromUser(u));
		}
		catch (final UserNotDefinedException e)
		{
			return Optional.empty();
		}
	}

	/**
	 * Return true if the current user has the Instructor section role and/or the gradebook.editAssessment permission.
	 *
	 * @param gradebookID the ID of the gradebook in question (site ID)
	 * @return true if the user has the ability, false otherwise
	 */
	public boolean currentUserHasEditPermission(final String gradebookID)
	{
		if (StringUtils.isBlank(gradebookID))
		{
			return false;
		}

		return gbs.currentUserHasEditPerm(gradebookID);
	}

	/**
	 * Returns map of student number -> GbUser. Used by the import process to identify students by number.
	 * @return
	 */
	public Map<String, GbUser> getUserStudentNumMap()
	{
		return bus.getUserEidMap().entrySet().stream()
				.filter(e -> StringUtils.isNotBlank(e.getValue().getStudentNumber()))
				.collect(Collectors.toMap(e -> e.getValue().getStudentNumber(), e -> e.getValue(),
				(user1, user2) -> { log.error("User {} already mapped to this student number, ignoring user {}", user1.getUserUuid(), user2.getUserUuid()); return user1; }));
	}

	/**
	 * Gets the user given a uuid. Acquires student number but not anon id. Student numbers
	 * are revealed even if the account type doesn't have permission, as long as the student is in a roster.
	 * Assumes that the current user is allowed to see student numbers.
	 * @param userUuid the uuid of the user to get
	 * @param site the site the user belongs to
	 * @return OwlGbUser populated with the revealed student number
	 */
	public Optional<OwlGbUser> getUserRevealingNumber(final String userUuid, Site site)
	{
		try
		{
			return Optional.of(OwlGbUser.fromUserAcquiringRevealedStudentNumber(uds.getUser(userUuid), site, bus));
		}
		catch (final UserNotDefinedException e)
		{
			return Optional.empty();
		}
	}

	/**
	 * Gets student number, revealing it regardless of account type if the student is in a roster.
	 * Use only if you have already checked isStudentNumberVisible().
	 * @param u the user
	 * @param site cannot be null
	 * @return the student number
	 */
	public String getRevealedStudentNumber(User u, Site site)
	{
		return cdp.getInstitutionalNumericId(u, site)
				.orElseGet(() ->
				{
					String num = revealStudentNumber(u, site);
					if (!num.isEmpty() && isStudentInARoster(u, bus.owl().getSiteSections(site))) // check for presence of number before hitting CM tables
					{
						return num; // always reveal student number if they are in the roster as a student
					}

					return "";
				});
	}

	/**
	 * Retrieves the student number for this student, regardless of the student's number visibility permissions
	 * @param user the student
	 * @return the student number, or empty string if not found
	 */
	public String revealStudentNumber(OwlGbUser user)
	{
		Optional<Site> site = bus.getCurrentSite();
		if (!site.isPresent())
		{
			return "";
		}

		try
		{
			User u = uds.getUser(user.gbUser.getUserUuid());
			return revealStudentNumber(u, site.get());
		}
		catch (UserNotDefinedException e)
		{
			return "";
		}
	}

	private String revealStudentNumber(User user, Site site)
	{
		return cdp.getInstitutionalNumericIdIgnoringCandidatePermissions(user, site).orElse("");
	}

	private boolean isStudentInARoster(User user, List<GbGroup> sections)
	{
		for (GbGroup sec : sections)
		{
			Set<Membership> members = cms.getSectionMemberships(bus.owl().findSectionEid(sec).orElse(""));
			if (members.stream().anyMatch(m -> m.getUserId().equals(user.getEid()) && "S".equals(m.getRole())))
			{
				return true;
			}
		}

		return false;
	}
}
