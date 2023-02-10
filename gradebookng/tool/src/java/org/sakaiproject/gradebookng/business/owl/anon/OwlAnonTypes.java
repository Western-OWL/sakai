package org.sakaiproject.gradebookng.business.owl.anon;

import java.io.Serializable;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author plukasew
 */
public interface OwlAnonTypes
{
	public static final int NO_ID = -1;
	public static final int MULTIPLE_IDS = -2;

	@RequiredArgsConstructor
	public final class StudentAnonId implements Serializable
	{
		public final String studentId;
		public final int anonId;
	}

	public static String forDisplay(int anonId)
	{
		switch(anonId)
		{
			case NO_ID:
				return "";
			case MULTIPLE_IDS:
				return "Error: multiple ids"; // should be presentation layer but no need to localize
			default:
				return String.valueOf(anonId);
		}
	}
}
