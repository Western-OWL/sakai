package org.sakaiproject.gradebookng.business.owl.finalgrades;

import org.sakaiproject.gradebookng.business.GbRole;
import org.sakaiproject.gradebookng.business.util.MessageHelper;
import org.sakaiproject.tool.gradebook.Gradebook;

/**
 * A course grade formatter suitable for student views. Shows percentage/points based on gradebook settings.
 * @author plukasew
 */
public class OwlCourseGradeStudentFormatter extends OwlCourseGradeFormatter
{
	public OwlCourseGradeStudentFormatter(Gradebook gb)
	{
		// student display is based on gradebook settings (and students never see overrides)
		super(gb, GbRole.STUDENT, gb.isCourseGradeDisplayed(), gb.isCoursePointsDisplayed());
		showPercentage = gb.isCourseAverageDisplayed();
		showOverride = false;
	}

	@Override
	protected String notVisibleMsg()
	{
		return MessageHelper.getString("label.coursegrade.studentnotreleased");
	}
}
