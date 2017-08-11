package org.sakaiproject.gradebookng.tool.panels;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.sakaiproject.gradebookng.business.SortDirection;
import org.sakaiproject.gradebookng.tool.component.table.columns.GbColumnSortToggleLink;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;
import org.sakaiproject.gradebookng.tool.pages.IGradesPage;
import org.sakaiproject.gradebookng.tool.util.GbUtils;

/**
 *
 * @author plukasew
 */
public class FinalGradeColumnHeaderPanel extends Panel
{
	private static final String PARENT_ID = "header";

	public FinalGradeColumnHeaderPanel(final String id)
	{
		super(id);
	}
	
	@Override
	public void onInitialize()
	{
		super.onInitialize();
		
		final IGradesPage gradebookPage = (IGradesPage) getPage();
		
		GbUtils.getParentCellFor(this, PARENT_ID).ifPresent(p -> p.setOutputMarkupId(true));
		
		final GbColumnSortToggleLink title = new GbColumnSortToggleLink("title")
		{
			@Override
			public SortDirection getSort(GradebookUiSettings settings)
			{
				return FinalGradeColumnHeaderPanel.this.getSortOrder(settings);
			}
			
			@Override
			public void setSort(GradebookUiSettings settings, SortDirection value)
			{
				FinalGradeColumnHeaderPanel.this.setSortOrder(value, settings);
			}
		};
		
		final GradebookUiSettings settings = gradebookPage.getUiSettings();
		ResourceModel rm = new ResourceModel(getTitleMsgKey());
		title.add(new AttributeModifier("title", rm));
		title.add(new Label("label", rm));
		if (settings != null && getSortOrder(settings) != null)
		{
			title.add(new AttributeModifier("class", "gb-sort-" + getSortOrder(settings).toString().toLowerCase()));
		}
		add(title);
	}
	
	protected String getTitleMsgKey()
	{
		return "column.header.finalgrade";
	}
	
	protected SortDirection getSortOrder(GradebookUiSettings settings)
	{
		return settings.getFinalGradeSortOrder();
	}
	
	protected void setSortOrder(SortDirection value, GradebookUiSettings settings)
	{
		settings.setFinalGradeSortOrder(value);
	}
}
