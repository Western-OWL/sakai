package org.sakaiproject.gradebookng.tool.owl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.model.GbCourseGrade;
import org.sakaiproject.gradebookng.business.model.GbGradeInfo;
import org.sakaiproject.gradebookng.business.model.GbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.model.GbUser;
import org.sakaiproject.gradebookng.business.owl.anon.OwlAnonTypes;
import org.sakaiproject.gradebookng.tool.model.GbGradeTableData;
import org.sakaiproject.service.gradebook.shared.CourseGrade;
import org.sakaiproject.service.gradebook.shared.owl.anongrading.OwlAnonGradingID;

/**
 * Subclass of GbGradeTableData that sanitizes the table data if in an anonymous scenario
 *
 * @author plukasew
 */
public class OwlGbGradeTableData extends GbGradeTableData
{
	public boolean isAnonContext = false;

	public OwlGbGradeTableData(final GradebookNgBusinessService bus, final UiSettings settings)
	{
		super(bus, settings.gb);

		List<OwlAnonGradingID> anonIds = bus.owl().anon.getAnonGradingIDsForCurrentSite();
		if (anonIds.isEmpty())
		{
			return; // nothing futher to do
		}

		isAnonContext = settings.owl.isContextAnonymous();

		// now we can owlify the data through the magic of collection manipulation

		// filter out the course grade
		boolean hideCourseGrade = isAnonContext != bus.owl().anon.isCourseGradePureAnonForAllAssignments(getAssignments());
		if (hideCourseGrade)
		{
			getCourseGradeMap().clear();
		}

		// get the assignments, filter them
		Set<Long> itemsToKeep = settings.owl.getAnonAwareAssignmentIDsForContext();
		getAssignments().removeIf(a -> !itemsToKeep.contains(a.getId()));

		// get the categories, filter them
		Set<Long> catsToKeep = settings.owl.getAnonAwareCategoryIDsForContext();
		getCategories().removeIf(c -> !catsToKeep.contains(c.getId()));

		Map<String, Integer> anonIdMap = bus.owl().anon.getStudentAnonIdMap(anonIds);

		List<GbStudentGradeInfo> grades = getGrades();
		List<GbStudentGradeInfo> orig = new ArrayList<>(grades);
		grades.clear();
		grades.addAll(orig.stream()
				.filter(g -> !isAnonContext || anonIdMap.containsKey(g.getStudentEid()))
				.map(g -> anonymize(g, itemsToKeep, catsToKeep, anonIdMap.getOrDefault(g.getStudentEid(), OwlAnonTypes.NO_ID), hideCourseGrade))
				.collect(Collectors.toList()));

		
		if (isAnonContext) // resort the grades by anon id (student number) if we're in an anonymous context
		{
			Collections.sort(grades, (GbStudentGradeInfo g1, GbStudentGradeInfo g2) -> g1.getStudentNumber().compareTo(g2.getStudentNumber()));
		}
	}

	private GbStudentGradeInfo anonymize(GbStudentGradeInfo info, Set<Long> items, Set<Long> cats, int anonId, boolean hideCourseGrade)
	{
		// anonymize the user (if in anonymous context)
		GbUser user;
		if (isAnonContext)
		{
			user = new GbUser(info.getStudentUuid(), "", "", "", "", "", OwlAnonTypes.forDisplay(anonId)); // swap anonid for student number
		}
		else
		{
			user = new GbUser(info.getStudentUuid(), info.getStudentDisplayId(), info.getStudentDisplayName(), info.getStudentFirstName(),
					info.getStudentLastName(), "", info.getStudentNumber()); // missing param is sortName, it is not stored in GbStudentGradeInfo
		}
		GbStudentGradeInfo anon = new GbStudentGradeInfo(user);

		// copy or hide course grade
		GbCourseGrade gbcg = info.getCourseGrade();
		if (hideCourseGrade)
		{
			gbcg = new GbCourseGrade(new CourseGrade());
			gbcg.setDisplayString("");
		}
		anon.setCourseGrade(gbcg);

		// filter the grades and categories
		for (Entry<Long, GbGradeInfo> entry : info.getGrades().entrySet())
		{
			if (items.contains(entry.getKey()))
			{
				anon.addGrade(entry.getKey(), entry.getValue());
			}
		}
		for (Entry<Long, Double> entry : info.getCategoryAverages().entrySet())
		{
			if (cats.contains(entry.getKey()))
			{
				anon.addCategoryAverage(entry.getKey(), entry.getValue());
			}
		}

		return anon;
	}
}
