package org.sakaiproject.gradebookng.business.owl.finalgrades;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.gradebookng.business.GbCategoryType;
import org.sakaiproject.gradebookng.business.GbRole;
import org.sakaiproject.gradebookng.business.util.FormatHelper;
import org.sakaiproject.gradebookng.business.util.MessageHelper;
import org.sakaiproject.service.gradebook.shared.CourseGrade;
import org.sakaiproject.tool.gradebook.Gradebook;

/**
 * A replacement for the standard CourseGradeFormatter, which is too inflexible for OWL.
 *
 * This formatter is suitable for Instructor/TA views.
 *
 * There is some duplication here but it can't really be helped without a large refactor of the standard formatter (which is unused now
 * and may get removed anyway).
 * 
 * @author plukasew
 */
public class OwlCourseGradeFormatter implements Serializable
{
	protected final int categoryType;
	protected boolean showCourseGrade, showPercentage, showOverride, showPoints;

	public OwlCourseGradeFormatter(Gradebook gb, GbRole currentUserRole, boolean isCourseGradeVisibleToUser, boolean showPoints)
	{
		this(gb, currentUserRole, isCourseGradeVisibleToUser, showPoints, true);
	}

	public OwlCourseGradeFormatter(Gradebook gb, GbRole currentUserRole, boolean isCourseGradeVisibleToUser, boolean showPoints, boolean showOverride)
	{
		categoryType = gb.getCategory_type();
		showCourseGrade = currentUserRole == GbRole.INSTRUCTOR || isCourseGradeVisibleToUser;
		showPercentage = true;
		this.showOverride = showOverride;
		this.showPoints = showPoints;
	}

	public String format(CourseGrade cg)
	{
		return validate(cg).orElseGet(() -> build(cg));
	}

	/**
	 * the message to show if the course grade is not visible to this user
	 * @return the message
	 */
	protected String notVisibleMsg()
	{
		return MessageHelper.getString("label.coursegrade.nopermission");
	}

	private Optional<String> validate(CourseGrade cg)
	{
		if (cg == null)
		{
			return Optional.of(MessageHelper.getString("coursegrade.display.none"));
		}

		if (!showCourseGrade)
		{
			return Optional.of(notVisibleMsg());
		}

		return Optional.empty();
	}

	/**
	 * Always shows percentage, may show points and/or override
	 * @param cg the course grade to format
	 * @return a formatted grade string
	 */
	private String build(CourseGrade cg)
	{
		List<String> parts = new ArrayList<>(2);

		// percentage / override
		if (showPercentage)
		{
			buildPercentage(parts, cg);
		}
		
		// points
		if (showPoints)
		{
			buildPoints(parts, cg);
		}

		if (parts.isEmpty())
		{
			parts.add(MessageHelper.getString("coursegrade.display.none"));
		}

		return String.join(" ", parts);
	}

	private void buildPercentage(List<String> parts, CourseGrade cg)
	{
		final String calculatedGrade;
		final String override = cg.getEnteredGrade();
		if (showOverride && StringUtils.isNotBlank(override))
		{
			// show the override and not the percentage
			calculatedGrade = override.matches("^\\d+$") ? FormatHelper.formatStringAsPercentage(override) : override;
		}
		else
		{
			calculatedGrade = FormatHelper.formatStringAsPercentage(cg.getCalculatedGrade());
		}

		if (StringUtils.isNotBlank(calculatedGrade))
		{
			parts.add(new StringResourceModel("coursegrade.display.percentage-first", null,	new Object[] { calculatedGrade }).getString());
		}
	}

	private void buildPoints(List<String> parts, CourseGrade cg)
	{
		if (GbCategoryType.valueOf(categoryType) != GbCategoryType.WEIGHTED_CATEGORY)
		{
			Double pointsEarned = cg.getPointsEarned();
			Double totalPossible = cg.getTotalPointsPossible();

			// handle the special case in the gradebook service where totalPointsPossible = -1
			if (Double.valueOf(-1).equals(totalPossible))
			{
				pointsEarned = null;
				totalPossible = null;
			}

			if (pointsEarned != null && totalPossible != null)
			{
				String pointsEarnedDisplay = FormatHelper.formatGradeForDisplay(cg.getPointsEarned());
				String totalPossibleDisplay = FormatHelper.formatGradeForDisplay(cg.getTotalPointsPossible());
				String key = parts.isEmpty() ? "coursegrade.display.points-first" : "coursegrade.display.points-second";
				parts.add(MessageHelper.getString(key, pointsEarnedDisplay, totalPossibleDisplay));
			}
		}
	}
}
