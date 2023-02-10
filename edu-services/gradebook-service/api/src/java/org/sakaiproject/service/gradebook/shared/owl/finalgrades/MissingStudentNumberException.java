// 2012.07.07, plukasew, New

package org.sakaiproject.service.gradebook.shared.owl.finalgrades;

/**
 * Student number could not be found for official student
 *
 * @author plukasew
 */
public class MissingStudentNumberException extends RuntimeException
{
	public MissingStudentNumberException(String message)
	{
		super(message);
	}
}
