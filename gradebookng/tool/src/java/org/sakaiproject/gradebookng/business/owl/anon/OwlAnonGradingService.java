package org.sakaiproject.gradebookng.business.owl.anon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.model.GbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.model.GbUser;
import org.sakaiproject.gradebookng.business.owl.OwlGbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.owl.anon.OwlAnonTypes.StudentAnonId;
import org.sakaiproject.gradebookng.tool.owl.model.OwlGbUiSettings;
import org.sakaiproject.gradebookng.tool.owl.model.UiSettings;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.service.gradebook.shared.owl.anongrading.OwlAnonGradingID;
import org.sakaiproject.tool.gradebook.Gradebook;

/**
 *
 * @author plukasew
 */
@RequiredArgsConstructor @Slf4j
public class OwlAnonGradingService
{
	private final GradebookNgBusinessService bus;
	private final GradebookService gbs;

	public enum AnonStatus { NORMAL, ANON, MIXED };

	/**
	 * Convenience method; use isCourseGradePureAnonForAllAssignments if you already have access to the assignment list
	 * @return
	 */
	public boolean isCourseGradePureAnon()
	{
		return isCourseGradePureAnonForAllAssignments(bus.getGradebookAssignments());
	}

	/**
	 * Convenience method; use isCourseGradePureAnonForAllAssignments if you already have access to the assignment list
	 * @return
	 */
	public boolean isCourseGradePureAnon(final Gradebook gradebook)
	{
		return isCourseGradePureAnonForAllAssignments(bus.getGradebookAssignments(gradebook));
	}

	/**
	 * Returns true if all items counting toward the course grade are anonymous in the specified list of assignments. If no assignments count toward the course grade, it is not considered pure anonymous.
	 * @param allAssignments for performance purposes; it is expected to be the complete list of assignments in the course (or at least the entire list of assignments that count toward the course grade).
	 * To guarantee accuracy, pass the complete unfiltered list of assignments in the course
	 * @return
	 */
	public boolean isCourseGradePureAnonForAllAssignments(List<Assignment> allAssignments)
	{
		// Return true if there exists at least one anonymous counting assignment and no normal counting assignments
		boolean normalFound = false;
		boolean anonFound = false;
		for (Assignment assignment : allAssignments)
		{
			if (assignment.isCounted())
			{
				if (assignment.isAnon())
				{
					anonFound = true;
				}
				else
				{
					normalFound = true;
					break;
				}
			}
		}

		// Return true iff there is at least one counting assignment and all counting assignments are anonymous
		return anonFound && !normalFound;
	}

	/**
	 * Gets anonymousIds for the specified sectionEids
	 * @param sectionEids
	 * @return
	 */
	public List<OwlAnonGradingID> getAnonGradingIDsBySectionEIDs(Set<String> sectionEids)
	{
		return gbs.getAnonGradingIDsBySectionEIDs(sectionEids);
	}

	public List<OwlAnonGradingID> getAnonGradingIDsForCurrentSite()
	{
		return gbs.getAnonGradingIDsBySectionEIDs(bus.owl().getViewableSectionEids());
	}

	public List<OwlAnonGradingID> getAnonGradingIDsForCurrentSite(final Gradebook gradebook)
	{
		return gbs.getAnonGradingIDsBySectionEIDs(bus.owl().getViewableSectionEids(gradebook));
	}

	/**
	 * Gets anonymousIds for the group filter provided by GradebookUiSettings
	 * @param settings
	 * @return
	 */
	public List<OwlAnonGradingID> getAnonGradingIDsForUiSettings(UiSettings settings)
	{
		// The sections we'll be looking anonIDs up for
		List<String> sections = new ArrayList<>();
		String section = bus.owl().findSectionEid(settings.gb.getGroupFilter()).orElse("");
		if (StringUtils.isBlank(section))
		{
			sections.addAll(bus.owl().getViewableSectionEids());
		}
		else
		{
			sections.add(section);
		}

		return gbs.getAnonGradingIDsBySectionEIDs(sections);
	}

	/**
	 * For performance, use only in contexts where data is presented for one user
	 * @param userEid
	 * @return
	 */
	public Optional<Integer> getSectionAnonIdForUser(String userEid)
	{
		return Optional.ofNullable(getStudentAnonIdMap(getAnonGradingIDsForCurrentSite()).get(userEid));
	}

	public Map<String, Integer> getStudentAnonIdMap(List<OwlAnonGradingID> anonIds)
	{
		Map<String, Integer> map = new LinkedHashMap<>();
		Collections.sort(anonIds, defaultOwlAnonGradingIDOrdering);
		for (OwlAnonGradingID anonId : anonIds)
		{
			String eid = anonId.getUserEid();
			Integer gradingId = anonId.getAnonGradingID();
			if (StringUtils.isBlank(eid) || gradingId == null)
			{
				log.error("Found OwlAnonGradingID with empty eid ({}) or null gradingId ({})", eid, gradingId);
				continue;
			}

			map.put(eid, map.containsKey(eid) && !gradingId.equals(map.get(eid)) ? OwlAnonTypes.MULTIPLE_IDS : gradingId);
		}

		return map;
	}

	/**
	 * Builds a Map<section, Map<studentUid, anonId>> from the specified OwlAnonGradingID list
	 * @param anonIds
	 * @return
	 */
	public Map<String, List<StudentAnonId>> getSectionStudentAnonIdMap(List<OwlAnonGradingID> anonIds)
	{
		Map<String, List<StudentAnonId>> sectionStudentAnonIdMap = new LinkedHashMap<>();
		Collections.sort(anonIds, defaultOwlAnonGradingIDOrdering);
		for (OwlAnonGradingID anonId : anonIds)
		{
			sectionStudentAnonIdMap.computeIfAbsent(anonId.getSectionEid(), l -> new ArrayList<>())
					.add(new StudentAnonId(anonId.getUserEid(), anonId.getAnonGradingID()));
		}

		return sectionStudentAnonIdMap;
	}

	public List<OwlGbStudentGradeInfo> owlify(List<GbStudentGradeInfo> matrix, boolean isContextAnonymous)
	{
		List<OwlAnonGradingID> anonIDs = isContextAnonymous ? getAnonGradingIDsForCurrentSite() : Collections.emptyList();
		final Map<String, Integer> anonIdMap = isContextAnonymous ? getStudentAnonIdMap(anonIDs) : Collections.emptyMap();
		List<OwlGbStudentGradeInfo> items = matrix.stream()
				.map(i -> new OwlGbStudentGradeInfo(i, anonIdMap.getOrDefault(i.getStudentEid(), OwlAnonTypes.NO_ID)))
				.filter(o -> !isContextAnonymous || o.anonId != OwlAnonTypes.NO_ID).collect(Collectors.toList());

		return items;
	}

	public AnonStatus detectAnonStatus(List<Assignment> assignments)
	{
		// Cases:
		// 1) Assignments all normal / empty
		// 2) Assignments mixed
		// 3) Assignments all anonymous:
		//    a) none count towards course grade?: context is mixed since course grades are normal
		//    b) some count towards course grade?: context = anonymous
		boolean hasNormal = false;
		boolean hasAnon = false;
		boolean hasCountingAnon = false;
		for (Assignment assignment : assignments)
		{
			if (assignment.isAnon())
			{
				hasAnon = true;
				if (assignment.isCounted())
				{
					hasCountingAnon = true;
				}
			}
			else
			{
				hasNormal = true;
			}

			if (hasNormal && hasAnon)
			{
				// Case 2) mixed scenario
				break;
			}
		}

		if (!hasAnon)
		{
			return AnonStatus.NORMAL;
		}
		else if (hasCountingAnon && !hasNormal)
		{
			return AnonStatus.ANON;
		}

		return AnonStatus.MIXED;
	}

	/**
	 * Visits all given assignments and populates the uiSettings.getAnonAwareAssignmentIDsForContext and getAnonAwareCategoryIDsForContext lists, representing which assignments and categories we need to display scores for
	 * @param uiSettings used to determine if the context is anonymous
	 * @param allAssignments the list of all assignments in this gradebook
	 */
	public void setupAnonAwareAssignmentIDsAndCategoryIDsForContext(OwlGbUiSettings uiSettings, Collection<Assignment> allAssignments)
	{
		Set<Long> assignmentIDsToInclude = new HashSet<>();
		Set<Long> categoryIDsToIncludeScores = new HashSet<>();
		uiSettings.setAnonAwareAssignmentIDsForContext(assignmentIDsToInclude);
		uiSettings.setAnonAwareCategoryIDsForContext(categoryIDsToIncludeScores);
		Set<Long> categoriesContainingNormal = new HashSet<>();
		Set<Long> categoriesContainingAnonymous = new HashSet<>();
		for (Assignment assignment : allAssignments)
		{
			Long categoryId = assignment.getCategoryId();
			if (categoryId != null)
			{
				if (assignment.isAnon())
				{
					categoriesContainingAnonymous.add(categoryId);
				}
				else
				{
					categoriesContainingNormal.add(categoryId);
				}
			}
			if (assignment.isAnon() == uiSettings.isContextAnonymous())
			{
				assignmentIDsToInclude.add(assignment.getId());
			}
		}
		if (uiSettings.isContextAnonymous())
		{
			// show grades for pure anonymous categories; if there's one normal item, it's mixed and should be displayed only in the normal context
			categoryIDsToIncludeScores.addAll(categoriesContainingAnonymous);
			categoryIDsToIncludeScores.removeAll(categoriesContainingNormal);
		}
		else
		{
			// If there are any normal items, we display the category score
			categoryIDsToIncludeScores.addAll(categoriesContainingNormal);
		}
	}

	/**
	 * Maps anon id to a GbUser for quick lookup during file imports
	 * @return map of anonID -> GbUser
	 */
	public Map<String, GbUser> getAnonIDUserMap()
	{
		Map<String, Integer> anonIdMap = getStudentAnonIdMap(getAnonGradingIDsForCurrentSite());

		return bus.getUserEidMap().entrySet().stream().filter(e -> anonIdMap.get(e.getKey()) != null)
				.collect(Collectors.toMap(e -> String.valueOf(anonIdMap.get(e.getKey())), e -> e.getValue()));
	}

	public List<Assignment> filterByAnonContext(List<Assignment> items, OwlGbUiSettings settings)
	{
		Set<Long> itemIdsToKeep = settings.getAnonAwareAssignmentIDsForContext();
		return items.stream().filter(i -> itemIdsToKeep.contains(i.getId())).collect(Collectors.toList());
	}

	/**
	 * Orders OwlAnonGradingIDs: primary sort by sections, secondary sort by userEids. This is mainly used to ensure the order in which OwlAnonGradingID data is added into LinkedHashMaps is deterministic.
	 */
	public static Comparator<OwlAnonGradingID> defaultOwlAnonGradingIDOrdering = new Comparator<OwlAnonGradingID>()
	{
		@Override
		public int compare(final OwlAnonGradingID id1, final OwlAnonGradingID id2)
		{
			int sectionComparison = id1.getSectionEid().compareTo(id2.getSectionEid());
			if (sectionComparison == 0)
			{
				int userIdComparison = id1.getUserEid().compareTo(id2.getUserEid());
				// if userIdComparison is 0, there's no need to go further with anonIds; they should be unique per (sectionId, userId) pair
				return userIdComparison;
			}
			return sectionComparison;
		}
	};

	/**
	 * Comparator class for sorting a list of users by anonymous grading IDs
	 */
	public static class AnonIDComparator implements Comparator<OwlGbStudentGradeInfo>
	{
		@Override
		public int compare(final OwlGbStudentGradeInfo s1, final OwlGbStudentGradeInfo s2)
		{
			return Integer.valueOf(s1.anonId).compareTo(s2.anonId);
		}
	}
}
