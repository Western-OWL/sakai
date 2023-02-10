package org.sakaiproject.gradebookng.business.owl.finalgrades;

import java.io.Serializable;
import lombok.Getter;
import org.sakaiproject.gradebookng.business.model.GbCourseGrade;
import org.sakaiproject.gradebookng.business.owl.OwlGbUser;

/**
 *
 * @author plukasew
 */
public class OwlGbStudentCourseGradeInfo implements Serializable
{
	public final OwlGbUser student;
	
	@Getter
	private GbCourseGrade courseGrade;

	private OwlGbStudentCourseGradeInfo()
	{
		student = null;
	}
	
	public OwlGbStudentCourseGradeInfo(final OwlGbUser user)
	{
		student = user;
	}

	public OwlGbStudentCourseGradeInfo(final OwlGbUser user, final GbCourseGrade grade)
	{
		student = user;
		courseGrade = grade;
	}
}
