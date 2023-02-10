package org.sakaiproject.gradebookng.business.owl.finalgrades;

import org.sakaiproject.gradebookng.business.owl.OwlGbStopWatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.coursemanagement.api.Membership;
import org.sakaiproject.coursemanagement.api.Section;
import org.sakaiproject.gradebookng.business.GbRole;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.model.GbCourseGrade;
import org.sakaiproject.gradebookng.business.model.GbGroup;
import org.sakaiproject.gradebookng.business.model.GbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.model.GbUser;
import org.sakaiproject.gradebookng.business.owl.OwlGbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.owl.OwlGbUser;
import org.sakaiproject.gradebookng.business.owl.anon.OwlAnonGradingService.AnonIDComparator;
import org.sakaiproject.gradebookng.business.owl.anon.OwlAnonTypes;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;
import org.sakaiproject.gradebookng.tool.owl.model.OwlGbUiSettings;
import org.sakaiproject.gradebookng.tool.owl.model.UiSettings;
import org.sakaiproject.service.gradebook.shared.CourseGrade;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.service.gradebook.shared.owl.anongrading.OwlAnonGradingID;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeApproval;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeSubmission;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.tool.gradebook.Gradebook;

/**
 * Delegate class for GradebookNgBusinessService that handles all of our OWL final grades custom code
 * 
 * @author plukasew
 */
@RequiredArgsConstructor
public class OwlFinalGradesService
{
	private final GradebookNgBusinessService bus;
	private final GradebookService gbs;
	private final CourseManagementService cms;

	public List<OwlGradeSubmission> getAllCourseGradeSubmissionsForSection(final String sectionEid) throws IllegalArgumentException
	{
		return gbs.getAllCourseGradeSubmissionsForSectionInSite(sectionEid, bus.getCurrentSiteId());
	}

	public OwlGradeSubmission getMostRecentCourseGradeSubmissionForSection(final String sectionEid) throws IllegalArgumentException
	{
		return gbs.getMostRecentCourseGradeSubmissionForSectionInSite(sectionEid, bus.getCurrentSiteId());
	}

	// OWL-1228  --plukasew
	public boolean isSectionApproved(final String sectionEid) throws IllegalArgumentException
	{
		return gbs.isSectionInSiteApproved(sectionEid, bus.getCurrentSiteId());
	}

	public boolean areAllSectionsApproved(final Set<String> sectionEids) throws IllegalArgumentException
	{
		return gbs.areAllSectionsInSiteApproved(sectionEids, bus.getCurrentSiteId());
	}

	public Long createSubmission(final OwlGradeSubmission sub) throws IllegalArgumentException
	{
		return gbs.createSubmission(sub);
	}

	public void updateSubmission(final OwlGradeSubmission sub) throws IllegalArgumentException
	{
		gbs.updateSubmission(sub);
	}

	public Long createApproval(final OwlGradeApproval approval) throws IllegalArgumentException
	{
		return gbs.createApproval(approval);
	}

	public boolean isOfficialRegistrarGradingSchemeInUse( final Long gradebookID )
	{
		return gbs.isOfficialRegistrarGradingSchemeInUse(gradebookID);
	}

	public Section getSectionByEid(String eid)
	{
		return cms.getSection(eid);
	}

	public Set<Membership> getSectionMemberships(String sectionEid)
	{
		return cms.getSectionMemberships(sectionEid);
	}

	/**
	 * Gets the course grade info objects for the given section. Requires permission to view student numbers.
	 * @param group the section
	 * @return as list of course grade info objects (student data + course grade data)
	 */
	public List<OwlGbStudentCourseGradeInfo> getSectionCourseGrades(GbGroup group)
	{
		if (bus.owl().findSectionEid(group).isPresent())
		{
			OwlGbStopWatch sw = new OwlGbStopWatch("bus.getSectionCourseGrades");
			List<String> users = bus.getGradeableUsers(group);
			sw.time("getGradableUsers");
			Map<String, CourseGrade> courseGrades = bus.getCourseGrades(users);
			sw.time("getCourseGrades");
			List<OwlGbStudentCourseGradeInfo> secCourseGrades = new ArrayList<>(courseGrades.size());
			// before looping, get the current site and ensure the user can see student numbers
			Site site = bus.getCurrentSite().orElse(null);
			boolean canView = bus.isStudentNumberVisible();
			if (site != null && canView)
			{
				for (Map.Entry<String, CourseGrade> entry : courseGrades.entrySet())
				{
					Optional<OwlGbUser> student = bus.owl().getUserRevealingNumber(entry.getKey(), site);
					GbCourseGrade grade = new GbCourseGrade(entry.getValue());
					student.ifPresent(s -> secCourseGrades.add(new OwlGbStudentCourseGradeInfo(s, grade)));
				}			
				sw.time("create course grade infos");

				return secCourseGrades;
			}
		}

		return Collections.emptyList();
	}

	/**
	 * Return true if the current user has the submit or approve permissions. This is NOT complete check
	 * of final submitter/approver status because we can't perform the full check with no section selected.
	 * @param siteId
	 * @return
	 */
	public boolean currentUserCanSeeFinalGradesPage(final String siteId)
	{
		final String userId = bus.getCurrentUser().getId();
		return GbRole.INSTRUCTOR.equals(bus.getUserRoleOrNone()) && gbs.owlAuthz().isUserAbleToSubmitOrApproveInSite(userId, siteId);
	}

	public List<OwlGbStudentGradeInfo> buildGradeMatrixForFinalGrades(final UiSettings uiSettings)
	{
		// OWLTODO: test performance for hits from looking up sections, looking up anon ids, converting to OwlGbUsers, etc.

		final UiSettings settings = uiSettings != null ? uiSettings : new UiSettings(new GradebookUiSettings(), new OwlGbUiSettings());
		final boolean anon = settings.owl.isContextAnonymous();

		final OwlGbStopWatch stopwatch = new OwlGbStopWatch("buildGradeMatrixForFinalGrades");
		stopwatch.time("buildGradeMatrixForFinalGrades start");

		final Gradebook gradebook = bus.getGradebook();
		if (gradebook == null)
		{
			return Collections.emptyList();
		}
		stopwatch.time("getGradebook");

		final GbRole role = bus.getUserRoleOrNone();
		final Site site = bus.getCurrentSite().orElse(null);
		if (role != GbRole.INSTRUCTOR || site == null)
		{
			return Collections.emptyList();
		}

		List<GbUser> gbUsers = bus.getGbUsersForUiSettings(bus.getGradeableUsers(settings.gb.getGroupFilter()), settings.gb, site);

		// We can filter here early if we're not dealing with anonymous ids
		if (!anon)
		{
			gbUsers = gbUsers.stream().filter(u -> filterMatches(u, settings)).collect(Collectors.toList());
		}

		// destructure the filtered GbUsers back into uuids, not ideal but we have to filter them first
		List<String> studentUuids = gbUsers.stream().map(GbUser::getUserUuid).collect(Collectors.toList());
		stopwatch.time("get gbUsers and convert GbUsers to uuids");

		final Map<String, GbStudentGradeInfo> matrix = new LinkedHashMap<>();

		bus.putCourseGradesInMatrix(matrix, gbUsers, studentUuids, gradebook, role, true, settings.gb);
		stopwatch.time("putCourseGradesInMatrix()");

		// this effectively will not sort if the UiSettings have an OWL-specific column as the sort property so we won't end up sorting it twice
		List<GbStudentGradeInfo> sortedGradeInfo = bus.sortGradeMatrix(matrix, settings.gb);

		// now that we have our final matrix, owlify it, transforming it into OwlGbStudentGradeInfo
		List<OwlGbStudentGradeInfo> items = bus.owl().anon.owlify(sortedGradeInfo, anon);
		stopwatch.time("owlify");

		final String filter = settings.owl.getStudentFilter();
		if (anon && !filter.isEmpty()) // apply ui filter
		{
			items = items.stream().filter(i -> anonFilterMatches(filter, i)).collect(Collectors.toList());
		}

		// sort if required
		if (settings.owl.getFinalGradeSortOrder() != null)
		{
			bus.owl().sort(items, new FinalGradeComparator(), settings.owl.getFinalGradeSortOrder());
		}
		else if (settings.owl.getCalculatedSortOrder() != null)
		{
			bus.owl().sort(items, new CalculatedCourseGradeComparator(), settings.owl.getCalculatedSortOrder());
		}
		else if (settings.owl.getAnonIdSortOrder() != null)
		{
			bus.owl().sort(items, new AnonIDComparator(), settings.owl.getAnonIdSortOrder());
		}
		else if (settings.owl.getUserIdSortOrder() != null)
		{
			bus.owl().sort(items, new UserIDComparator(), settings.owl.getUserIdSortOrder());
		}

		return items;
	}

	/**
	 * Omnisearch for filter string against name, eid, and student number. If the filter string contains multiple
	 * tokens, they must all match (AND not OR). This is so that "John Smith" can be entered to return "John Smith" but not
	 * "John Doe" and "Jenny Smith". Matching is case-insensitive on names/eids.
	 * @param gbUser the user
	 * @param settings the settings containing the filter string
	 * @return true if filter string matches
	 */
	private boolean filterMatches(GbUser gbUser, UiSettings settings)
	{
		String[] filters = settings.owl.getStudentFilter().split("\\s+");
		int matches = 0;
		for (String filter : filters)
		{
			if (StringUtils.containsIgnoreCase(gbUser.getFirstName(), filter)
				|| StringUtils.containsIgnoreCase(gbUser.getLastName(), filter)
				|| StringUtils.containsIgnoreCase(gbUser.getDisplayId(), filter) // case-insensitive just in case we get bad data
				|| (StringUtils.contains(gbUser.getStudentNumber(), filter)))
			{
				++matches;
			}
		}

		return matches > 0 && matches == filters.length;
	}

	/**
	 * Filters users on anonymous id (only used on Final Grades page).
	 * @param filter the search string
	 * @param info the user
	 * @return true if filter string matches
	 */
	private boolean anonFilterMatches(String filter, OwlGbStudentGradeInfo info)
	{
		return String.valueOf(info.anonId).contains(filter);
	}

	/**
	 * Comparator class for sorting by OWL final grade (course sites)
	 *
	 */
	class FinalGradeComparator implements Comparator<OwlGbStudentGradeInfo>
	{
		@Override
		public int compare(final OwlGbStudentGradeInfo g1, final OwlGbStudentGradeInfo g2)
		{
			String fg1 = FinalGradeFormatter.formatForRegistrar(g1.info.getCourseGrade());
			String fg2 = FinalGradeFormatter.formatForRegistrar(g2.info.getCourseGrade());

			return fg1.compareTo(fg2);
		}
	}

	/**
	 * Comparator class for sorting by calculated course grade only
	 *
	 */
	class CalculatedCourseGradeComparator implements Comparator<OwlGbStudentGradeInfo>
	{
		@Override
		public int compare(final OwlGbStudentGradeInfo g1, final OwlGbStudentGradeInfo g2)
		{
			Double cg1 = OwlGbCourseGrade.getCalculatedGrade(g1.info.getCourseGrade()).orElse(Double.NEGATIVE_INFINITY);
			Double cg2 = OwlGbCourseGrade.getCalculatedGrade(g2.info.getCourseGrade()).orElse(Double.NEGATIVE_INFINITY);

			return cg1.compareTo(cg2);
		}
	}

	class UserIDComparator implements Comparator<OwlGbStudentGradeInfo>
	{
		@Override
		public int compare(final OwlGbStudentGradeInfo g1, final OwlGbStudentGradeInfo g2)
		{
			return g1.info.getStudentDisplayId().compareTo(g2.info.getStudentDisplayId());
		}
	}
}
