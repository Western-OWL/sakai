package org.sakaiproject.gradebookng.business.util;

import java.text.DecimalFormat;
import org.sakaiproject.gradebookng.business.model.GbCourseGrade;
import org.sakaiproject.service.gradebook.shared.CourseGrade;

/**
 *
 * @author plukasew
 */
public class FinalGradeFormatter
{
	/**
	 * Formats the final grade for use in the Gradebook. Rounds numeric grades.
	 * @param gbcg the course grade
	 * @return the appropriate override or rounded calculated grade
	 */
	public static String format(GbCourseGrade gbcg)
	{
		return format(gbcg, false);
	}
	
	/**
	 * Formats the final grade (override or rounded calculated course grade, as applicable) for sending to the Registrar.
	 * Rounds numeric grades. Pads grade to 3 characters. Assumes grade has already passed validation.
	 * @param gbcg the course grade
	 * @return the appropriate override or rounded calculated grade, padded to 3 characters
	 */
	public static String formatForRegistrar(GbCourseGrade gbcg)
	{
		return format(gbcg, true);
	}
	
	private static String format(GbCourseGrade gbcg, boolean forReg)
	{	
		CourseGrade cg = gbcg.getCourseGrade();
		if (gbcg.hasOverride())
		{
			return forReg ? overrideToRegistrarFinal(cg.getEnteredGrade()) : cg.getEnteredGrade();
		}
		
		long rounded = round(gbcg.getCalculatedGrade());
		return forReg ? padNumeric(rounded) : String.valueOf(rounded);
	}
	
	/**
	 * The official OWL final grade rounding function
	 * @return the final grade rounded to a whole number
	 */
	public static long round(double d)
	{
		return Math.round(d);
	}
	
	public static String padNumeric(long grade)
	{
		DecimalFormat formatNoDecimals = new DecimalFormat("000");
        return formatNoDecimals.format(grade);
	}
	
	public static String overrideToRegistrarFinal(String override)
	{
		String g = override.trim();
		try
		{
			return padNumeric(Long.parseLong(g));
		}
		catch (NumberFormatException nfe)
		{
			while (g.length() < 3)
			{
				g += " "; // pad with trailing spaces to fill 3 characters
			}
			
			return g;
		}
	}
}
