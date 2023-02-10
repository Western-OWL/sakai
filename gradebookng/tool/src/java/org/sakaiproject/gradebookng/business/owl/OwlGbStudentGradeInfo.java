package org.sakaiproject.gradebookng.business.owl;

import java.io.Serializable;
import java.util.Optional;
import org.sakaiproject.gradebookng.business.model.GbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.owl.finalgrades.OwlGbCourseGrade;

/**
 *
 * @author plukasew
 */
public class OwlGbStudentGradeInfo implements Serializable
{
	public final GbStudentGradeInfo info;
	public final int anonId;

	public OwlGbStudentGradeInfo(GbStudentGradeInfo info, int anonId)
	{
		this.info = info;
		this.anonId = anonId;
	}

	public Optional<String> getGradeOverride()
	{
		return OwlGbCourseGrade.getOverride(info.getCourseGrade());
	}
}
