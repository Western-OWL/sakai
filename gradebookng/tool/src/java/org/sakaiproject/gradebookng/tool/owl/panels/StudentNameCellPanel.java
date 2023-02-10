package org.sakaiproject.gradebookng.tool.owl.panels;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.sakaiproject.gradebookng.business.model.GbStudentNameSortOrder;
import org.sakaiproject.gradebookng.tool.owl.component.table.columns.StudentNameColumn;

/**
 *
 * Cell panel for the student name
 *
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 * @author plukasew
 *
 */
public class StudentNameCellPanel extends GenericPanel<StudentNameColumn.StudentNameColumnData>
{
	private static final long serialVersionUID = 1L;

	public StudentNameCellPanel(final String id, final IModel<StudentNameColumn.StudentNameColumnData> model)
	{
		super(id, model);
	}

	@Override
	public void onInitialize()
	{
		super.onInitialize();

		StudentNameColumn.StudentNameColumnData data = getModelObject();
		add(new Label("name", getFormattedStudentName(data.firstName, data.lastName, data.nameOrder)));
	}

	/**
	 * Helper to format a student name based on the sort type.
	 *
	 * Sorted by Last Name = Smith, John (jsmith26) Sorted by First Name = John Smith (jsmith26)
	 *
	 * @param firstName
	 * @param lastName
	 * @param sortOrder
	 * @return
	 */
	private String getFormattedStudentName(final String firstName, final String lastName, final GbStudentNameSortOrder sortOrder) {

		final String msg = "formatter.studentname." + sortOrder.name();
		if (GbStudentNameSortOrder.LAST_NAME == sortOrder) {
			return String.format(getString(msg), lastName, firstName);
		}
		else if (GbStudentNameSortOrder.FIRST_NAME == sortOrder) {
			return String.format(getString(msg), firstName, lastName);
		}
		return firstName;
	}
}
