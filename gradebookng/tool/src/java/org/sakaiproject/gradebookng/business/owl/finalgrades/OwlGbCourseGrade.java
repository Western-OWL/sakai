package org.sakaiproject.gradebookng.business.owl.finalgrades;

import java.util.Optional;
import org.apache.commons.lang3.math.NumberUtils;
import org.sakaiproject.gradebookng.business.model.GbCourseGrade;
import org.sakaiproject.service.gradebook.shared.CourseGrade;

/**
 *
 * @author plukasew
 */
public class OwlGbCourseGrade
{
	public static Optional<String> getOverride(GbCourseGrade gbcg)
	{
		CourseGrade cg = gbcg.getCourseGrade();
		return cg == null ? Optional.empty() : Optional.ofNullable(cg.getEnteredGrade());
	}

	public static Optional<Double> getCalculatedGrade(GbCourseGrade gbcg)
	{
		CourseGrade cg = gbcg.getCourseGrade();
		if (cg == null)
		{
			return Optional.empty();
		}

		double grade = NumberUtils.toDouble(cg.getCalculatedGrade(), Double.MIN_VALUE);
		return grade == Double.MIN_VALUE ? Optional.empty() : Optional.of(grade);
	}
}
