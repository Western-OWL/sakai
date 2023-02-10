package org.sakaiproject.gradebookng.tool.owl.panels;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.sakaiproject.gradebookng.business.SortDirection;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;
import org.sakaiproject.gradebookng.tool.owl.component.table.columns.GbColumnSortToggleLink;
import org.sakaiproject.gradebookng.tool.owl.model.UiSettings;
import org.sakaiproject.gradebookng.tool.owl.pages.IGradesPage;

/**
 *
 * @author plukasew
 */
public class StudentNumberColumnHeaderPanel extends Panel
{	
	public StudentNumberColumnHeaderPanel(final String id)
	{
		super(id);
	}
	
	@Override
	public void onInitialize()
	{
		super.onInitialize();
		
		final IGradesPage gradebookPage = (IGradesPage) getPage();
		
		// title
		final GbColumnSortToggleLink title = new GbColumnSortToggleLink("title")
		{
			@Override
			public SortDirection getSort(UiSettings settings)
			{
				return settings.gb.getStudentNumberSortOrder();
			}
			
			@Override
			public void setSort(UiSettings settings, SortDirection value)
			{
				settings.setSort(UiSettings.GbSortColumn.STUDENT_NUMBER, value);
			}
		};
		
		title.add(new AttributeModifier("title", new ResourceModel("column.header.studentNumber")));
		title.add(new Label("label", new ResourceModel("column.header.studentNumber")));
		final GradebookUiSettings settings = gradebookPage.getUiSettings();
		SortDirection sort = settings.getStudentNumberSortOrder();
		if (sort != null)
		{
			title.add(new AttributeModifier("class", "gb-sort-" + sort.toString().toLowerCase()));
		}
		
		add(title);
	}
}
