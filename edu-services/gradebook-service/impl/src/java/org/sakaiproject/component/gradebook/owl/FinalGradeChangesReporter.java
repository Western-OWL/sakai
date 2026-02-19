package org.sakaiproject.component.gradebook.owl;

import java.util.Set;
import org.sakaiproject.component.gradebook.GradebookServiceHibernateImpl;
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

	FinalGradeChangesReporter(GradebookServiceHibernateImpl gbService)
	{
		gbServ = gbService;
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

	private Set<OwlGradeSubmissionGrades> getPreviousGrades(String siteId, String sectionEid)
	{
		// OWLTODO: impl...see CourseGradeSubmitter.getGradeChangeReport()
		// step 2 and 3

		return Set.of();
	}

	private FGChanges checkForChanges(Set<OwlGradeSubmissionGrades> currentGrades, Set<OwlGradeSubmissionGrades> previousGrades)
	{
		// OWLTODO: impl...see CourseGradeSubmitter.checkForGradeChanges()
		// step 4

		return new FGChanges(0, 0, 0);
	}
}
