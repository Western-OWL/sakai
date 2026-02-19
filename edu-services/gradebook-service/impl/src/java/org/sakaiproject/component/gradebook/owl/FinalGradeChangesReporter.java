package org.sakaiproject.component.gradebook.owl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sakaiproject.component.gradebook.GradebookServiceHibernateImpl;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeSubmission;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeSubmissionGrades;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.report.FGChanges;

/**
 * Delegate class to handle retrieving final grade changes (revised/added/removed counts) for the unsubmitted
 * final grades report job.
 *
 * NOTE: this class heavily copies from code in GradebookNG in order to make that logic accessible through
 * GradebookService, introducing code duplication. See OWL-5728 for the rationale behind this decision.
 *
 * @author plukasew
 */
class FinalGradeChangesReporter
{
	private final GradebookServiceHibernateImpl gbServ;
	private final OwlGradebookServiceImpl owlgbServ;

	FinalGradeChangesReporter(GradebookServiceHibernateImpl gbService, OwlGradebookServiceImpl owlgbService)
	{
		gbServ = gbService;
		owlgbServ = owlgbService;
	}

	FGChanges getChanges(String siteId, String sectionEid)
	{
		// overall impl based on CourseGradeSubmitter.getGradeChangeReport()
		// 1. get the current grades
		// 2. get the existing final grade submission
		// 3. get the submitted grades from the existing submission
		// 4. compare the current and previous grades and return result

		return checkForChanges(getCurrentGrades(siteId, sectionEid), getPreviousGrades(siteId, sectionEid));
	}

	private Set<OwlGradeSubmissionGrades> getCurrentGrades(String siteId, String sectionEid)
	{
		// OWLTODO: impl...see CourseGradeSubmitter.getCurrentCourseGrades()
		// step 1

		return Set.of();
	}

	// Compare with CourseGradeSubmitter.getGradeChangeReport()
	private Set<OwlGradeSubmissionGrades> getPreviousGrades(String siteId, String sectionEid)
	{
		// step 2 - get the last submission
		OwlGradeSubmission lastSub = owlgbServ.getMostRecentCourseGradeSubmissionForSectionInSite(sectionEid, siteId);

		// step 3 - get the grades for the last submission
		Set<OwlGradeSubmissionGrades> prevGrades = new HashSet<>();
		if (lastSub != null)
		{
			int submissionStatus = lastSub.getStatusCode();
			if (submissionStatus == OwlGradeSubmission.PENDING_APPROVAL_STATUS || submissionStatus == OwlGradeSubmission.APPROVED_STATUS)
			{
				prevGrades.addAll(lastSub.getGradeData());
			}
		}

		return prevGrades;
	}

	// Compare with CourseGradeSubmitter.checkForGradeChanges()
	private FGChanges checkForChanges(Set<OwlGradeSubmissionGrades> currentGrades, Set<OwlGradeSubmissionGrades> previousGrades)
	{
		// step 4 - compare current grades to previous grades for changes
		Map<String, OwlGradeSubmissionGrades> currentGradeMap = currentGrades.stream().collect(Collectors.toMap(OwlGradeSubmissionGrades::getStudentNumber, Function.identity()));
		Map<String, OwlGradeSubmissionGrades> prevGradeMap = previousGrades.stream().collect(Collectors.toMap(OwlGradeSubmissionGrades::getStudentNumber, Function.identity()));

		Set<String> newStudents = new HashSet<>(currentGradeMap.keySet());
		Set<String> sameStudents = new HashSet<>(currentGradeMap.keySet());
		Set<String> changedStudents = new HashSet<>();
		Set<String> missingStudents = new HashSet<>(prevGradeMap.keySet());

		sameStudents.retainAll(prevGradeMap.keySet());
		newStudents.removeAll(sameStudents);
		missingStudents.removeAll(sameStudents);

		for (String s : sameStudents)
		{
			if (!currentGradeMap.get(s).getGrade().equals(prevGradeMap.get(s).getGrade()))
			{
				changedStudents.add(s);
			}
		}

		return new FGChanges(changedStudents.size(), newStudents.size(), missingStudents.size());
	}
}
