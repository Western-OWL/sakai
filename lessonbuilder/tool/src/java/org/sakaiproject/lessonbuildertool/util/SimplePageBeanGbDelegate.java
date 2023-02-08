package org.sakaiproject.lessonbuildertool.util;

import java.util.Date;
import lombok.Builder;
import org.sakaiproject.lessonbuildertool.service.GradebookIfc;
import org.sakaiproject.service.gradebook.shared.ConflictingAssignmentNameException;
import org.sakaiproject.service.gradebook.shared.InvalidGradeItemNameException;
import org.sakaiproject.lessonbuildertool.tool.beans.SimplePageBean;

/**
 * Handles common GB integration code for SimplePageBean and provides a consistent result for GB operations.
 * 
 * @author plukasew
 */
public class SimplePageBeanGbDelegate
{
	public enum GB_RESULT { SUCCESS, INVALID_CHARS, DUP_TITLE, NO_GB };

	@Builder
	public static final class ExternalAttributes
	{
		public final String gbUid;
		public final String extId;
		public final String extUrl;
		public final String title;
		public final double points;
		public final Date due;
	}

	public static GB_RESULT addExternalAssessment(GradebookIfc gbIfc, ExternalAttributes attr)
	{
		return addOrUpdateExternalAssessment(true, gbIfc, attr);
	}

	public static GB_RESULT updateExternalAssessment(GradebookIfc gbIfc, ExternalAttributes attr)
	{
		return addOrUpdateExternalAssessment(false, gbIfc, attr);
	}

	private static GB_RESULT addOrUpdateExternalAssessment(boolean add, GradebookIfc gbIfc, ExternalAttributes attr)
	{
		GB_RESULT result = GB_RESULT.NO_GB;
		try
		{
			gbIfc.validateExternalTitle(attr.title, attr.gbUid, attr.extId);
			boolean success = add ? gbIfc.addExternalAssessment(attr.gbUid, attr.extId, attr.extUrl, attr.title, attr.points, attr.due, SimplePageBean.LESSONBUILDER_ID)
					: gbIfc.updateExternalAssessment(attr.gbUid, attr.extId, attr.extUrl, attr.title, attr.points, attr.due);
			if (success)
			{
				result = GB_RESULT.SUCCESS;
			}
		}
		catch (ConflictingAssignmentNameException cane)
		{
			result = GB_RESULT.DUP_TITLE;
		}
		catch (InvalidGradeItemNameException igine)
		{
			result = GB_RESULT.INVALID_CHARS;
		}

		return result;
	}
}
