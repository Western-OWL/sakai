package org.sakaiproject.gradebookng.tool.owl.component;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.PropertyModel;
import org.sakaiproject.gradebookng.business.GbRole;
import org.sakaiproject.gradebookng.business.model.GbGroup;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;
import org.sakaiproject.gradebookng.tool.owl.component.dropdown.GbGroupChoiceRenderer;
import org.sakaiproject.gradebookng.tool.owl.component.dropdown.SakaiSpinnerDropDownChoice;
import org.sakaiproject.gradebookng.tool.owl.component.dropdown.SakaiSpinningSelectOnChangeBehavior;
import org.sakaiproject.gradebookng.tool.owl.component.table.GbSakaiPagerContainer;
import org.sakaiproject.gradebookng.tool.owl.component.table.FinalGradesDataTable;
import org.sakaiproject.gradebookng.tool.owl.model.OwlGbUiSettings;
import org.sakaiproject.gradebookng.tool.owl.model.UiSettings;
import org.sakaiproject.gradebookng.tool.owl.pages.FinalGradesPage;
import org.sakaiproject.service.gradebook.shared.PermissionDefinition;

/**
 *
 * @author plukasew
 */
public class GbBaseGradesDisplayToolbar extends Panel
{
	protected final FinalGradesDataTable table;
	protected boolean showGroupFilter;
	protected List<GbGroup> groups;
	protected GbRole role;
	protected List<PermissionDefinition> permissions;
	protected final boolean hasAssignmentsAndGrades;
	private SakaiAjaxButton clearButton;
	
	public GbBaseGradesDisplayToolbar(String id, FinalGradesDataTable table, List<GbGroup> groups, final boolean hasAssignmentsAndGrades)
	{
		super(id);
		this.table = table;
		showGroupFilter = true;
		this.groups = groups;
		this.hasAssignmentsAndGrades = hasAssignmentsAndGrades;
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		
		FinalGradesPage page = (FinalGradesPage) getPage();
		final UiSettings settings = page.getGbUiSettings();
		
		// section and group dropdown
		showGroupFilter = showGroupFilter();
		if (showGroupFilter)
		{
			handleShowGroupFilter();
		}
		else
		{
			add(new Label("groupFilterOnlyOne", groups.isEmpty() ? Model.of("") : Model.of(groups.get(0).getTitle())));
		}

		final SakaiSpinnerDropDownChoice<GbGroup> groupFilter = new SakaiSpinnerDropDownChoice<GbGroup>("groupFilter",
				new Model<GbGroup>(), groups, new GbGroupChoiceRenderer(), new StringResourceModel("filter.groups", this, null),
				new SakaiSpinningSelectOnChangeBehavior()
				{
					@Override
					protected void onUpdate(final AjaxRequestTarget target) {

						final GbGroup selected = (GbGroup) getFormComponent().getDefaultModelObject();

						// store selected group (null ok)
						final GradebookUiSettings settings = page.getUiSettings();
						settings.setGroupFilter(selected);
						page.setUiSettings(settings);

						// refresh
						page.redrawForGroupChange(target);
					}

				});
				
		configureGroupFilter(groupFilter, settings.gb);
		groupFilter.setMarkupId("groupFilterSelect");
		add(groupFilter);

		final Form<String> form = new Form<>("studentFilterForm", new PropertyModel<>(settings.owl, "studentFilter"));
		add(form);
		final TextField<String> filterTextField = new TextField<>("studentFilter", form.getModel());
		form.add(filterTextField);
		final SakaiAjaxButton submit = new SakaiAjaxButton("studentFilterButton")
		{
			@Override
			public void onSubmit(final AjaxRequestTarget target, final Form<?> form)
			{
				String filterText = StringUtils.trimToEmpty((String) form.getModelObject());
				if (!filterText.isEmpty())
				{
					clearButton.setVisible(true);
				}
				final OwlGbUiSettings settings = page.getOwlUiSettings();
				settings.setStudentFilter(filterText);

				page.setOwlUiSettings(settings); // save settings

				page.resetPaging();
				page.redrawSpreadsheet(target);
			}
		};
		form.add(submit);
		form.setDefaultButton(submit); // prevents Enter key in search filter from submitting grades
		clearButton = new SakaiAjaxButton("studentFilterClear")
		{
			@Override
			public void onSubmit(final AjaxRequestTarget target, final Form<?> form)
			{
				clearButton.setVisible(false);

				final OwlGbUiSettings settings = page.getOwlUiSettings();
				settings.setStudentFilter("");

				page.setOwlUiSettings(settings); // save settings

				page.resetPaging();
				page.redrawSpreadsheet(target);
			}
		};
		clearButton.setVisible(!page.getOwlUiSettings().getStudentFilter().isEmpty());
		form.add(clearButton);
		
		add(pagerContainer());
	}
	
	protected GbSakaiPagerContainer pagerContainer()
	{
		return new GbSakaiPagerContainer("gradebookPager", table);
	}
	
	protected void configureGroupFilter(SakaiSpinnerDropDownChoice<GbGroup> groupFilter, GradebookUiSettings settings)
	{
		if (!groups.isEmpty())
		{
			// set selected group, or first item in list
			groupFilter.setModelObject((settings.getGroupFilter() != null) ? settings.getGroupFilter() : groups.get(0));
		}
		groupFilter.select.setNullValid(false);
		groupFilter.setVisible(showGroupFilter);
	}
	
	protected boolean showGroupFilter()
	{
		if (groups.size() > 1)
		{
			return true;
		}
		return false;
	}
	
	protected void handleShowGroupFilter()
	{
		add(new EmptyPanel("groupFilterOnlyOne").setVisible(false));
	}
}
