package org.sakaiproject.gradebookng.tool.owl.model;

import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import org.sakaiproject.gradebookng.business.SortDirection;
import org.sakaiproject.gradebookng.business.model.GbAssignmentGradeSortOrder;
import org.sakaiproject.gradebookng.business.model.GbCategoryAverageSortOrder;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;

/**
 *
 * @author plukasew
 */
@RequiredArgsConstructor
public class UiSettings implements Serializable
{
	public enum GbSortColumn { COURSE_GRADE, CATEGORY, ASSIGNMENT, STUDENT, STUDENT_NUMBER, FINAL_GRADE, CALCULATED, ANON_ID, USER_ID };

	public final GradebookUiSettings gb;
	public final OwlGbUiSettings owl;

	public void setCategorySort(GbCategoryAverageSortOrder value)
	{
		gb.setCategorySortOrder(value);
		owl.clearSortOrder();
	}

	public void setAssignmentSort(GbAssignmentGradeSortOrder value)
	{
		gb.setAssignmentSortOrder(value);
		owl.clearSortOrder();
	}

	public void setSort(GbSortColumn col, SortDirection value)
	{
		switch (col)
		{
			case COURSE_GRADE:
				gb.setCourseGradeSortOrder(value);
				owl.clearSortOrder();
				break;
			case CATEGORY:
				throw new UnsupportedOperationException("Use setCategorySort() instead");
			case ASSIGNMENT:
				throw new UnsupportedOperationException("Use setAssignmentSort() instead");
			case STUDENT:
				gb.setStudentSortOrder(value);
				owl.clearSortOrder();
				break;
			case STUDENT_NUMBER:
				gb.setStudentNumberSortOrder(value);
				owl.clearSortOrder();
				break;
			case FINAL_GRADE:
				gb.setStudentSortOrder(null); // this effectively clears the sort on the GradebookUiSettings object
				owl.setFinalGradeSortOrder(value);
				break;
			case CALCULATED:
				gb.setStudentSortOrder(null);
				owl.setCalculatedSortOrder(value);
				break;
			case ANON_ID:
				gb.setStudentSortOrder(null);
				owl.setAnonIdSortOrder(value);
				break;
			case USER_ID:
				gb.setStudentSortOrder(null);
				owl.setUserIdSortOrder(value);
				break;
			default:
				throw new UnsupportedOperationException("Unsupported, requires implementation");
		}
	}
}
