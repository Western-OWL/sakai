package org.sakaiproject.gradebookng.tool.owl.panels;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.sakaiproject.gradebookng.business.SortDirection;
import org.sakaiproject.gradebookng.tool.owl.component.table.columns.GbColumnSortToggleLink;
import org.sakaiproject.gradebookng.tool.owl.model.UiSettings;
import org.sakaiproject.gradebookng.tool.owl.pages.IGradesPage;

/**
 *
 * @author plukasew
 */
public class UserIdColumnHeaderPanel extends Panel
{
	public UserIdColumnHeaderPanel(final String id)
	{
		super(id);
	}

	@Override
	public void onInitialize()
	{
		super.onInitialize();

		final IGradesPage page = (IGradesPage) getPage();

		final GbColumnSortToggleLink title = new GbColumnSortToggleLink("title")
		{
			@Override
			public SortDirection getSort(UiSettings settings)
			{
				return settings.owl.getUserIdSortOrder();
			}

			@Override
			public void setSort(UiSettings settings, SortDirection value)
			{
				settings.setSort(UiSettings.GbSortColumn.USER_ID, value);
			}
		};
		IModel<String> titleModel = new ResourceModel("column.header.userid");
		title.add(new AttributeModifier("title", titleModel));
		title.add(new Label("label", titleModel));
		SortDirection sortDir = page.getOwlUiSettings().getUserIdSortOrder();
		if (sortDir != null)
		{
			title.add(new AttributeModifier("class", "gb-sort-" + sortDir.toString().toLowerCase()));
		}
		add(title);
	}
}
