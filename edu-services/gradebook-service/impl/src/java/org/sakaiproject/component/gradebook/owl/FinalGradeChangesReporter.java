package org.sakaiproject.component.gradebook.owl;

import org.sakaiproject.component.gradebook.GradebookServiceHibernateImpl;
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

	FGChanges getChanges(String siteId, String sectionId)
	{
		// OWLTODO: impl based on CourseGradeSubmitter.getGradeChangeReport()

		return new FGChanges(0, 0, 0);
	}
}
