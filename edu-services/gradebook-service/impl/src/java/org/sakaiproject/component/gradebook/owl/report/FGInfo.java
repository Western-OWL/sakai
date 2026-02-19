package org.sakaiproject.component.gradebook.owl.report;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import org.sakaiproject.service.gradebook.shared.CourseGrade;

/**
 * This class is a stripped-down substitute for OwlGbStudentCourseGradeInfo in GBNG
 * @author plukasew
 */
@RequiredArgsConstructor
public class FGInfo
{
	public final String userEid, studentNumber;
	private final CourseGrade cg;

	// copied from OwlGbCourseGrade
	public Optional<String> getOverride()
	{
		return cg == null ? Optional.empty() : Optional.ofNullable(cg.getEnteredGrade());
	}

	// copied from OwlGbCourseGrade
	public Optional<Double> getCalculatedGrade()
	{
		if (cg == null)
		{
			return Optional.empty();
		}

		double grade = NumberUtils.toDouble(cg.getCalculatedGrade(), Double.MIN_VALUE);
		return grade == Double.MIN_VALUE ? Optional.empty() : Optional.of(grade);
	}
}
