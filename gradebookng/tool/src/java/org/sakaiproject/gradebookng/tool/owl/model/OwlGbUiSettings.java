package org.sakaiproject.gradebookng.tool.owl.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.gradebookng.business.SortDirection;


/**
 * Used to store OWL-specific gradebook ui settings in the session. Companion class to GradebookUiSettings.
 * @author plukasew
 */
@Data
public class OwlGbUiSettings implements Serializable
{
	public static final int DEFAULT_PAGE_SIZE = 100;

	private int pageSize;
	private boolean anonPossible = false;
	private boolean isContextAnonymous = false;
	private boolean isGradebookMixed = false; // true iff both normal and anonymous items exist, or all items are anonymous but none count toward the course grade so the course grade is normal
	private boolean isCourseGradeAnon = false;
	private Set<Long> anonAwareAssignmentIDsForContext; // Tracks which assignments are visible. Contains all assignments whose isAnon() matches isContextAnonymous
	private Set<Long> anonAwareCategoryIDsForContext; // Tracks which categories' scores are visible. Note: mixed category scores are displayed in normal contexts, but hidden in anonymous contexts. If the context is normal, this contains pure normal and mixed categories; if the context is anonymous, this contains only pure anonymous categories.
	private SortDirection finalGradeSortOrder; // final grade column
	private SortDirection calculatedSortOrder; // calculated course grade column (final grades page)
	private SortDirection anonIdSortOrder; // anonymous id column
	private SortDirection userIdSortOrder; // user id column
	private String studentFilter;

	public OwlGbUiSettings()
	{
		pageSize = DEFAULT_PAGE_SIZE;
		studentFilter = "";
		anonAwareAssignmentIDsForContext = Collections.emptySet();
		anonAwareCategoryIDsForContext = Collections.emptySet();
	}

	/**
	 * Sets the paging size for the grades table on the Final Grades page. Set to 0 for no limit.
	 * @param value number of rows in each page. Set to 0 to indicate no limit. Negative values are ignored.
	 */
	public void setPageSize(int value)
	{
		if (value >= 0)
		{
			pageSize = value;
		}
	}

	public void setStudentFilter(String value)
	{
		studentFilter = StringUtils.trimToEmpty(value);
	}

	public void clearSortOrder()
	{
		finalGradeSortOrder = null;
		calculatedSortOrder = null;
		anonIdSortOrder = null;
		userIdSortOrder = null;
	}

	public void setFinalGradeSortOrder(SortDirection value)
	{
		clearSortOrder();
		finalGradeSortOrder = value;
	}

	public void setCalculatedSortOrder(SortDirection value)
	{
		clearSortOrder();
		calculatedSortOrder = value;
	}

	public void setAnonIdSortOrder(SortDirection value)
	{
		clearSortOrder();
		anonIdSortOrder = value;
	}

	public void setUserIdSortOrder(SortDirection value)
	{
		clearSortOrder();
		userIdSortOrder = value;
	}

	/**
	 * Convenience method to tell if the course grade column should be hidden in the current context
	 * @return true if the course grade column should be hidden
	 */
	public boolean isCourseGradeHiddenInCurrentContext()
	{
		return isContextAnonymous != isCourseGradeAnon;
	}
}
