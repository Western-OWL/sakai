package org.sakaiproject.gradebookng.tool.owl.panels;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

import org.sakaiproject.gradebookng.business.SortDirection;
import org.sakaiproject.gradebookng.business.model.GbStudentNameSortOrder;
import org.sakaiproject.gradebookng.tool.component.GbAjaxLink;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;
import org.sakaiproject.gradebookng.tool.owl.component.table.columns.GbColumnSortToggleLink;
import org.sakaiproject.gradebookng.tool.owl.model.UiSettings;
import org.sakaiproject.gradebookng.tool.owl.pages.FinalGradesPage;
import org.sakaiproject.gradebookng.tool.owl.pages.IGradesPage;

/**
 *
 * Header panel for the student name
 *
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 * @author plukasew
 *
 */
public class StudentNameColumnHeaderPanel extends GenericPanel<GbStudentNameSortOrder>
{
	public StudentNameColumnHeaderPanel(final String id, final IModel<GbStudentNameSortOrder> model)
	{
		super(id, model);
	}

	@Override
	public void onInitialize()
	{
		super.onInitialize();

		final IGradesPage gradebookPage = (IGradesPage) getPage();

		// setup model
		final GbStudentNameSortOrder sortType = getModelObject();

		// title
		final GbColumnSortToggleLink title = new GbColumnSortToggleLink("title")
		{	
			@Override
			public SortDirection getSort(UiSettings settings)
			{
				return StudentNameColumnHeaderPanel.this.getSort(settings);
			}
			
			@Override
			public void setSort(UiSettings settings, SortDirection value)
			{
				StudentNameColumnHeaderPanel.this.setSort(value, settings);
			}
		};

		final UiSettings settings = gradebookPage.getGbUiSettings();
		String titleKey = settings.owl.isContextAnonymous() ? "column.header.students.anonymous" : "column.header.students";
		ResourceModel titleModel = new ResourceModel(titleKey);
		title.add(new AttributeModifier("title", titleModel));
		title.add(new Label("label", titleModel));
		if (getSort(settings) != null)
		{
			title.add(new AttributeModifier("class", "gb-sort-" + getSort(settings).toString().toLowerCase()));
		}
		add(title);

		boolean isContextAnonymous = settings.owl.isContextAnonymous();

		// sort by first/last name link
		final GbAjaxLink<GbStudentNameSortOrder> sortByName = new GbAjaxLink<GbStudentNameSortOrder>("sortByName", getModel()) {

			@Override
			public void onClick(final AjaxRequestTarget target) {

				// get current sort
				final GbStudentNameSortOrder currentSort = StudentNameColumnHeaderPanel.this.getModelObject();

				// get next
				final GbStudentNameSortOrder newSort = currentSort.toggle();

				// set the sort
				final FinalGradesPage gradebookPage = (FinalGradesPage) getPage();
				final GradebookUiSettings settings = gradebookPage.getUiSettings();
				settings.setNameSortOrder(newSort);

				// save settings
				gradebookPage.setUiSettings(settings);
				
				// refresh
				gradebookPage.resetPaging();
				gradebookPage.redrawSpreadsheet(target);

			}
		};

		// the label changes depending on the state so we wrap it in a model
		final IModel<String> sortByNameModel = new Model<String>()
		{
			@Override
			public String getObject()
			{
				// shows the label opposite to the current sort type
				if (settings.owl.isContextAnonymous())
				{
					return getString("sortbyname.option.anonymous");
				}
				else if (sortType == GbStudentNameSortOrder.FIRST_NAME)
				{
					return getString("sortbyname.option.last");
				}

				return getString("sortbyname.option.first");
			}
		};

		sortByName.add(new Label("sortByNameLabel", sortByNameModel));

		WebMarkupContainer studentDropdown = new WebMarkupContainer("studentDropdown");
		studentDropdown.add(sortByName);
		studentDropdown.setVisible(!isContextAnonymous);
		add(studentDropdown);
	}
	
	protected SortDirection getSort(UiSettings settings)
	{
		return settings.owl.isContextAnonymous() ? settings.owl.getAnonIdSortOrder() : settings.gb.getStudentSortOrder();
	}
	
	protected void setSort(SortDirection value, UiSettings settings)
	{
		if (settings.owl.isContextAnonymous())
		{
			settings.setSort(UiSettings.GbSortColumn.ANON_ID, value);
		}
		else
		{
			settings.setSort(UiSettings.GbSortColumn.STUDENT, value);
		}
	}
}
