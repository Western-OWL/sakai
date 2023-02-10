package org.sakaiproject.gradebookng.tool.owl.panels.anon;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.Model;
import org.sakaiproject.gradebookng.business.SortDirection;
import org.sakaiproject.gradebookng.tool.owl.model.UiSettings;
import org.sakaiproject.gradebookng.tool.pages.GradebookPage;

/**
 *
 * @author plukasew
 * @author bbailla2
 */
public class AnonTogglePanel extends GenericPanel<Boolean>
{
	private RadioGroup anonymousToggle;
	private GradebookPage page;

	public AnonTogglePanel(String id)
	{
		super(id);
		setRenderBodyOnly(true);
		setDefaultModel(Model.of(Boolean.FALSE));
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();

		page = (GradebookPage) getPage();

		setDefaultModelObject(page.getOwlUiSettings().isContextAnonymous());

		// Toggles the context between normal / anonymous items. Model is boolean: False->normal, True->anonymous  --bbailla2
		anonymousToggle = new RadioGroup("toggleAnonymous", getDefaultModel())
		{
			@Override
			public boolean isInputNullable()
			{
				return false;
			}
		};

		Radio anonToggle_normal = new Radio("anonToggle_normal", Model.of(Boolean.FALSE));
		Radio anonToggle_anonymous = new Radio("anonToggle_anonymous", Model.of(Boolean.TRUE));
		anonymousToggle.add(anonToggle_normal.add(new AjaxEventBehavior("onchange")
		{
			@Override
			protected void onEvent(AjaxRequestTarget target)
			{
				UiSettings settings = page.getGbUiSettings();
				// Flip the context to normal
				settings.owl.setContextAnonymous(false);
				anonymousToggle.setModelObject(Boolean.FALSE);
				// Use default sort order. Maintaining any sort order would violate anonymity constraints
				settings.setSort(UiSettings.GbSortColumn.STUDENT, SortDirection.ASCENDING);
				// Clear the group filter for single user group violation of anonymity constraint
				settings.gb.setGroupFilter(page.getAllGroup());
				// GBNG- style full page refresh
				setResponsePage(page.getPageClass());

			}
		}));
		anonymousToggle.add(anonToggle_anonymous.add(new AjaxEventBehavior("onchange")
		{
			@Override
			protected void onEvent(AjaxRequestTarget target)
			{
				UiSettings settings = page.getGbUiSettings();
				// Flip the context to anonymous
				settings.owl.setContextAnonymous(true);
				anonymousToggle.setModelObject(Boolean.TRUE);
				// Clear filters and use the AnonIdSortOrder to eliminate any anonymity constraint violations
				settings.owl.setStudentFilter("");
				settings.setSort(UiSettings.GbSortColumn.ANON_ID, SortDirection.ASCENDING);
				// Clear the group filter for single user group violation of anonymity constraint
				settings.gb.setGroupFilter(page.getAllGroup());
				// GBNG- style full page refresh
				setResponsePage(page.getPageClass());
			}
		}));

		add(anonymousToggle);
	}
}
