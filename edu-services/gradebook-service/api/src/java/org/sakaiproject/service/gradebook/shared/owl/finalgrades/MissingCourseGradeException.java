// 2012.07.30, plukasew, New
// Exception for case when valid student has not course grade

package org.sakaiproject.service.gradebook.shared.owl.finalgrades;

/**
 * Course grade could not be found for official student
 *
 * @author plukasew
 */
public class MissingCourseGradeException extends RuntimeException
{
	public MissingCourseGradeException(String message)
	{
		super(message);
	}
}
