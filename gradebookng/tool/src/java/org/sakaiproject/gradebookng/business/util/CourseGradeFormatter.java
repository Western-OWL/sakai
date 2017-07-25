
package org.sakaiproject.gradebookng.business.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.gradebookng.business.GbCategoryType;
import org.sakaiproject.gradebookng.business.GbRole;
import org.sakaiproject.service.gradebook.shared.CourseGrade;
import org.sakaiproject.tool.gradebook.Gradebook;

/**
 * Helper class to handle the formatting of the course grade display string
 *
 * @author Steve Seinsburg (steve.swinsburg@gmail.com)
 */
public class CourseGradeFormatter {

	private final Gradebook gradebook;
	private final GbRole currentUserRole;
	private final boolean isCourseGradeVisible;
	private final boolean showPoints;
	private final boolean showOverride;
	private final boolean showLetterGrade;

	/**
	 * Constructor to initialise the data
	 *
	 * All of this gets passed in ONCE, then reused for every format call
	 *
	 * @param gradebook the gradebook settings
	 * @param currentUserRole role of the current user
	 * @param isCourseGradeVisible if the course grade is visible to the user
	 * @param showPoints if we are to show points
	 * @param showOverride if we are to show the override
	 * @param showLetterGrade if we are to show the letter grade
	 * @return
	 */
	public CourseGradeFormatter(final Gradebook gradebook, final GbRole currentUserRole, final FormatterConfig config)
	{
		this.gradebook = gradebook;
		this.currentUserRole = currentUserRole;
		this.isCourseGradeVisible = config.isCourseGradeVisible;
		this.showPoints = config.showPoints;
		this.showOverride = config.showOverride;
		this.showLetterGrade = config.showLetterGrade;
	}

	/**
	 * Format the passed in course grade
	 *
	 * @param courseGrade the raw course grade for the student
	 *
	 * @return the formatted display string
	 */
	public String format(final CourseGrade courseGrade) {

		String rval = null;

		// something has gone wrong and there's no course grade!
		if (courseGrade == null) {
			rval = MessageHelper.getString("coursegrade.display.none");
			// instructor, can view
		} else if (this.currentUserRole == GbRole.INSTRUCTOR) {
			rval = build(courseGrade);
			// TA, permission check
		} else if (this.currentUserRole == GbRole.TA) {
			if (!this.isCourseGradeVisible) {
				rval = MessageHelper.getString("label.coursegrade.nopermission");
			} else {
				rval = build(courseGrade);
			}
			// student, check if course grade released, and permission check
		} else {
			if (this.gradebook.isCourseGradeDisplayed()) {
				if (!this.isCourseGradeVisible) {
					rval = MessageHelper.getString("label.coursegrade.nopermission");
				} else {
					rval = build(courseGrade);
				}
			} else {
				rval = MessageHelper.getString("label.coursegrade.studentnotreleased");
			}
		}

		return rval;

	}

	/**
	 * Takes care of checking the values and configured settings to format the course grade into an applicable display format
	 *
	 * Format:
	 *
	 * Instructor always gets lettergrade + percentage but may also get points depending on setting. TA, same as instructor unless disabled
	 * Student gets whatever is configured
	 *
	 * @return formatted string ready for display
	 */
	private String build(final CourseGrade courseGrade) {
		final List<String> parts = new ArrayList<>();

		// letter grade
		String letter = getLetterGrade(courseGrade);
		if (!letter.isEmpty())
		{
			parts.add(letter);
		}

		// percentage
		buildPercentage(courseGrade, parts);

		// requested points
		buildPoints(courseGrade, parts);

		// if parts is empty, there are no grades, display a -
		if (parts.isEmpty()) {
			parts.add(MessageHelper.getString("coursegrade.display.none"));
		}

		return String.join(" ", parts);
	}
	
	private String getLetterGrade(final CourseGrade courseGrade)
	{
		if (!showLetterGrade)
		{
			return "";
		}
		
		String letterGrade;
		if (this.showOverride && StringUtils.isNotBlank(courseGrade.getEnteredGrade())) {
			letterGrade = courseGrade.getEnteredGrade();
		} else {
			letterGrade = courseGrade.getMappedGrade();
		}

		if (StringUtils.isNotBlank(letterGrade)
				&& (this.gradebook.isCourseLetterGradeDisplayed() || this.currentUserRole == GbRole.INSTRUCTOR)) {
			return letterGrade;
		}
		
		return "";
	}
	
	private void buildPercentage(final CourseGrade courseGrade, List<String> parts)
	{
		final String calculatedGrade;
		if (this.showOverride && StringUtils.isNotBlank(courseGrade.getEnteredGrade())) {

			// if mapping doesn't exist for this grade override (mapping may have been changed!), map it to 0.
			// TODO this should probably inform the instructor
			/*Double mappedGrade = this.gradebook.getSelectedGradeMapping().getGradeMap().get(courseGrade.getEnteredGrade());
			if(mappedGrade == null) {
				mappedGrade = new Double(0);
			}
			calculatedGrade = FormatHelper.formatDoubleAsPercentage(mappedGrade);*/
			
			// OWL NOTE: we going to show the override and not the percentage
			String override = courseGrade.getEnteredGrade();
			if (override.matches("^\\d+$"))
			{
				calculatedGrade = FormatHelper.formatStringAsPercentage(override);
			}
			else
			{
				calculatedGrade = override;
			}

		} else {
			calculatedGrade = FormatHelper.formatStringAsPercentage(courseGrade.getCalculatedGrade());
		}

		if (StringUtils.isNotBlank(calculatedGrade)
				&& (this.gradebook.isCourseAverageDisplayed() || this.currentUserRole == GbRole.INSTRUCTOR))
		{
			String key = parts.isEmpty() ? "coursegrade.display.percentage-first" : "coursegrade.display.percentage-second";
			parts.add(new StringResourceModel(key, null, new Object[] { calculatedGrade }).getString());
		}
	}
	
	private void buildPoints(final CourseGrade courseGrade, List<String> parts)
	{
		if (this.showPoints) {

			// don't display points for weighted category type
			final GbCategoryType categoryType = GbCategoryType.valueOf(this.gradebook.getCategory_type());
			if (categoryType != GbCategoryType.WEIGHTED_CATEGORY) {

				Double pointsEarned = courseGrade.getPointsEarned();
				Double totalPointsPossible = courseGrade.getTotalPointsPossible();

				// handle the special case in the gradebook service where totalPointsPossible = -1
				if(totalPointsPossible != null && totalPointsPossible == -1) {
					pointsEarned = null;
					totalPointsPossible = null;
				}

				// if instructor, show the points if requested
				// otherwise check the settings
				if (this.currentUserRole == GbRole.INSTRUCTOR || this.gradebook.isCoursePointsDisplayed()) {
					if(pointsEarned != null && totalPointsPossible != null)
					{
						String key = parts.isEmpty() ? "coursegrade.display.points-first" : "coursegrade.display.points-second";
						parts.add(MessageHelper.getString(key, pointsEarned, totalPointsPossible));
					}
				}
			}
		}
	}
	
	public static class FormatterConfig implements Serializable
	{
		public boolean isCourseGradeVisible = false;
		public boolean showPoints = false;
		public boolean showOverride = false;
		public boolean showLetterGrade = false;
	}
}
