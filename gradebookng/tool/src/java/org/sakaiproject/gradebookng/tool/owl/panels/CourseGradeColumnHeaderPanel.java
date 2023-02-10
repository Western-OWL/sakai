package org.sakaiproject.gradebookng.tool.owl.panels;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import org.sakaiproject.gradebookng.business.GbCategoryType;
import org.sakaiproject.gradebookng.business.GbRole;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.SortDirection;
import org.sakaiproject.gradebookng.tool.component.GbAjaxLink;
import org.sakaiproject.gradebookng.tool.model.GbModalWindow;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;
import org.sakaiproject.gradebookng.tool.owl.component.OwlGbUtils;
import org.sakaiproject.gradebookng.tool.owl.component.table.columns.GbColumnSortToggleLink;
import org.sakaiproject.gradebookng.tool.owl.model.UiSettings;
import org.sakaiproject.gradebookng.tool.owl.pages.IGradesPage;
import org.sakaiproject.gradebookng.tool.pages.BasePage;
import org.sakaiproject.gradebookng.tool.panels.ZeroUngradedItemsPanel;
import org.sakaiproject.tool.gradebook.Gradebook;

public class CourseGradeColumnHeaderPanel extends Panel {

	@SpringBean(name = "org.sakaiproject.gradebookng.business.GradebookNgBusinessService")
	protected GradebookNgBusinessService bus;
	
	private static final String PARENT_ID = "header";

	public CourseGradeColumnHeaderPanel(final String id) {
		super(id);
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		final IGradesPage gradebookPage = (IGradesPage) getPage();

		OwlGbUtils.getParentCellFor(this, PARENT_ID).ifPresent(p -> p.setOutputMarkupId(true));

		final GbColumnSortToggleLink title = new GbColumnSortToggleLink("title")
		{
			@Override
			public SortDirection getSort(UiSettings settings)
			{
				return CourseGradeColumnHeaderPanel.this.getSort(settings);
			}
			
			@Override
			public void setSort(UiSettings settings, SortDirection value)
			{
				CourseGradeColumnHeaderPanel.this.setSort(settings, value);
			}
		};

		final UiSettings settings = gradebookPage.getGbUiSettings();
		ResourceModel titleModel = getTitleModel();
		title.add(new AttributeModifier("title", titleModel));
		title.add(new Label("label", titleModel));
		if (settings != null && getSort(settings) != null) {
			title.add(
				new AttributeModifier("class", "gb-sort-" + getSort(settings).toString().toLowerCase()));
		}
		add(title);

		final Gradebook gradebook = bus.getGradebook();
		final GbRole role = bus.getUserRoleOrNone();

		//final GbCategoryType categoryType = GbCategoryType.valueOf(gradebook.getCategory_type());

		// get setting
		//final Boolean showPoints = this.model.getObject();

		// icons
		final Map<String, Object> popoverModel = new HashMap<>();
		popoverModel.put("role", role);
		popoverModel.put("flag", HeaderFlagPopoverPanel.Flag.COURSE_GRADE_RELEASED);
		final BasePage bp = (BasePage) gradebookPage;
		add(bp.buildFlagWithPopover("isReleasedFlag",
				new HeaderFlagPopoverPanel("popover", Model.ofMap(popoverModel)).toPopoverString())
				.setVisible(gradebook.isCourseGradeDisplayed()));
		popoverModel.put("flag", HeaderFlagPopoverPanel.Flag.COURSE_GRADE_NOT_RELEASED);
		add(bp.buildFlagWithPopover("notReleasedFlag",
				new HeaderFlagPopoverPanel("popover", Model.ofMap(popoverModel)).toPopoverString())
				.setVisible(!gradebook.isCourseGradeDisplayed()));

		// menu
		/*final WebMarkupContainer menu = new WebMarkupContainer("menu") {

			@Override
			public boolean isVisible() {
				return role == GbRole.INSTRUCTOR;
			}
		};
		menu.add(new GbAjaxLink("setUngraded") {

			@Override
			public void onClick(final AjaxRequestTarget target) {
				final GbModalWindow window = gradebookPage.getUpdateUngradedItemsWindow();
				window.setTitle(getString("heading.zeroungradeditems"));
				window.setComponentToReturnFocusTo(OwlGbUtils.getParentCellFor(this, PARENT_ID).orElse(null));
				window.setContent(new ZeroUngradedItemsPanel(window.getContentId(), window));
				window.showUnloadConfirmation(false);
				window.show(target);
			}
		});

		final GbAjaxLink<Boolean> showHidePoints = new GbAjaxLink("showHidePoints", this.model) {

			@Override
			public void onClick(final AjaxRequestTarget target) {

				// get current setting
				final Boolean currentSetting = CourseGradeColumnHeaderPanel.this.model.getObject();

				// toggle it
				final Boolean nextSetting = !currentSetting;

				// set it
				final GradebookUiSettings settings = gradebookPage.getUiSettings();
				settings.setShowPoints(nextSetting);

				// save settings
				gradebookPage.setUiSettings(settings);

				// refresh
				gradebookPage.redrawSpreadsheet(target);
			}

			@Override
			public boolean isVisible() {
				return categoryType != GbCategoryType.WEIGHTED_CATEGORY;
			}
		};*/

		// the label changes depending on the state so we wrap it in a model
		/*final IModel<String> showHidePointsModel = new Model<String>() {

			@Override
			public String getObject() {

				// toggles the label to the opposite one
				if (showPoints) {
					return getString("coursegrade.option.hidepoints");
				} else {
					return getString("coursegrade.option.showpoints");
				}
			}
		};
		showHidePoints.add(new Label("showHidePointsLabel", showHidePointsModel));
		menu.add(showHidePoints);

		add(menu);*/
	}

	protected SortDirection getSort(UiSettings settings)
	{
		return settings.owl.getCalculatedSortOrder();
	}

	protected void setSort(UiSettings settings, SortDirection value)
	{
		settings.setSort(UiSettings.GbSortColumn.CALCULATED, value);
	}
	
	protected ResourceModel getTitleModel()
	{
		return new ResourceModel("finalgrades.column.header.coursegrade");
	}
}