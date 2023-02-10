package org.sakaiproject.gradebookng.tool.owl.panels;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.util.string.ComponentRenderer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.model.GbCourseGrade;
import org.sakaiproject.gradebookng.business.owl.OwlGbStopWatch;
import org.sakaiproject.gradebookng.business.owl.OwlGbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.owl.finalgrades.FinalGradeFormatter;
import org.sakaiproject.gradebookng.business.owl.finalgrades.OwlGbCourseGrade;
import org.sakaiproject.gradebookng.tool.component.GbAjaxLink;
import org.sakaiproject.gradebookng.tool.model.GbModalWindow;
import org.sakaiproject.gradebookng.tool.owl.behavior.RevertScoreBehavior;
import org.sakaiproject.gradebookng.tool.owl.behavior.ScoreChangeBehavior;
import org.sakaiproject.gradebookng.tool.owl.component.OwlGbUtils;
import org.sakaiproject.gradebookng.tool.owl.component.table.columns.FinalGradeColumn;
import org.sakaiproject.gradebookng.tool.owl.component.table.columns.FinalGradeColumn.LiveGradingMessage;
import org.sakaiproject.gradebookng.tool.owl.pages.FinalGradesPage;
import org.sakaiproject.gradebookng.tool.owl.pages.IGradesPage;
import org.sakaiproject.gradebookng.tool.owl.panels.FinalGradeItemPopoverPanel.FinalGradePopoverData;
import org.sakaiproject.gradebookng.tool.panels.CourseGradeOverrideLogPanel;
import org.sakaiproject.service.gradebook.shared.GradebookInformation;
/**
 *
 * @author plukasew
 */
public class FinalGradeItemCellPanel extends GenericPanel<OwlGbStudentGradeInfo>
{
	@SpringBean(name = "org.sakaiproject.gradebookng.business.GradebookNgBusinessService")
	protected GradebookNgBusinessService businessService;
	
	private static final String PARENT_ID = "cells";
	
	TextField<String> gradeField;
	private String originalGrade;
	
	String baseGradeStyle = FinalGradeColumn.CSS_CLASS;
	GradeCellSaveStyle gradeSaveStyle;
	
	final List<FinalGradeCellNotification> notifications = new ArrayList<>();
	
	FinalGradesPage page;
	
	public enum FinalGradeCellNotification implements Serializable
	{
		ERROR("grade.notifications.haserror"),
		INVALID("finalgrades.notifications.invalid");
		
		private String key;
		
		FinalGradeCellNotification(final String messageKey)
		{
			key = messageKey;
		}
		
		public String getMessageKey()
		{
			return key;
		}
	}

	/**
	 * Enum for encapsulating the grade cell save css class that is to be applied
	 *
	 */
	enum GradeCellSaveStyle {

		SUCCESS("grade-save-success"),
		ERROR("grade-save-error"),
		WARNING("grade-save-warning"),
		OVER_LIMIT("grade-save-over-limit"),
		OVER_LIMIT_AND_SUCCESS("grade-save-over-limit grade-save-success");

		private String css;

		GradeCellSaveStyle(final String css) {
			this.css = css;
		}

		public String getCss() {
			return this.css;
		}
	}
	
	public FinalGradeItemCellPanel(final String id, final IModel<OwlGbStudentGradeInfo> model)
	{
		super(id, model);
	}
	
	@Override
	public void onInitialize()
	{
		super.onInitialize();
		
		page = (FinalGradesPage) getPage();
		
		final String studentUuid = getModelObject().info.getStudentUuid();
		final String displayGrade = formatGrade(getModelObject().info.getCourseGrade());
		
		gradeField = new TextField<String>("editableFinalGrade", Model.of(displayGrade))
		{
			@Override
			protected void onInitialize()
			{
				super.onInitialize();
				
				OwlGbUtils.getParentCellFor(this, PARENT_ID)
						.ifPresent(p -> p.add(new AttributeModifier("data-studentuuid", studentUuid)).setOutputMarkupId(true));
			}
		};
		gradeField.setOutputMarkupId(true);
		add(gradeField);
		
		String cellMarkupId = OwlGbUtils.getParentCellFor(this, PARENT_ID).map(p -> p.getMarkupId(true)).orElse("");
		gradeField.add(new ScoreChangeBehavior(studentUuid, cellMarkupId)
		{
			@Override
			public void onBind()
			{
				super.onBind();
				originalGrade = gradeField.getDefaultModelObjectAsString();
			}
			
			@Override
			protected void onUpdate(final AjaxRequestTarget target)
			{
				OwlGbStopWatch stopwatch = new OwlGbStopWatch("FinalGradeItemCellPanel onUpdate granular");
				OwlGbStopWatch sw2 = new OwlGbStopWatch("FinalGradeItemCellPanel onUpdate coarse");
				
				String newGrade = StringUtils.trimToNull(gradeField.getValue().toUpperCase());
				
				notifications.clear();
				
				// validate the grade entered is a valid one for the selected grading schema
				// though we allow blank grades so the override is removed
				boolean valid = true;
				if (newGrade != null)
				{
					final GradebookInformation gbInfo = businessService.getGradebookSettings();
					final Map<String, Double> schema = gbInfo.getSelectedGradingScaleBottomPercents();
					final List<String> unmapped = gbInfo.getSelectedGradingScaleUnmappedGrades();
					valid = schema.containsKey(newGrade) || unmapped.contains(newGrade);
				}
				stopwatch.time("validation complete");
				
				if (!valid)
				{
					markWarning(gradeField);
					target.add(page.updateLiveGradingMessage(LiveGradingMessage.ERROR));
				}
				else
				{
					String saveGrade = newGrade;
					if (StringUtils.isNotBlank(newGrade))
					{
						// does the entered grade match the rounded final grade anyway? If so, remove the override
						GbCourseGrade originalCourseGrade = new GbCourseGrade(businessService.getCourseGrade(studentUuid));
						String formatted = OwlGbCourseGrade.getCalculatedGrade(originalCourseGrade)
								.map(c -> String.valueOf(FinalGradeFormatter.round(c))).orElse("");
						if (newGrade.equals(formatted))
						{
							saveGrade = null;
						}
					}
					stopwatch.time("new grade formatted");

					// save
					final boolean success = businessService.updateCourseGrade(studentUuid, null, saveGrade);
					stopwatch.time("new grade saved");

					gradeField.setDefaultModelObject(newGrade); // OWLTODO: only set this on success?
					
					if (success)
					{
						markSuccessful(gradeField);
						originalGrade = newGrade;
						GbCourseGrade newCourseGrade = new GbCourseGrade(businessService.getCourseGrade(studentUuid));
						getModelObject().info.setCourseGrade(newCourseGrade);
						String displayGrade = formatGrade(newCourseGrade);
						gradeField.setModelObject(displayGrade);
						target.add(page.updateLiveGradingMessage(LiveGradingMessage.SAVED));
						stopwatch.time("prepped for redraw");
						((FinalGradesPage) page).redrawForGradeChange(target);
						stopwatch.time("redraw for grade change complete");
					}
					else
					{
						markError(gradeField);
						originalGrade = newGrade;
						target.add(page.updateLiveGradingMessage(LiveGradingMessage.ERROR));
					}
				}
				
				refreshNotifications();
				styleGradeCell(gradeField);
				
				// refresh the components we need
				target.addChildren(getPage(), FeedbackPanel.class);
				OwlGbUtils.getParentCellFor(getComponent(), PARENT_ID).ifPresent(target::add);
				target.add(gradeField);
				
				stopwatch.time("refresh components");
				sw2.time("complete");
			}
		});
		
		gradeField.add(new RevertScoreBehavior(studentUuid, cellMarkupId)
		{
			@Override
			protected void onEvent(final AjaxRequestTarget target)
			{	
				// reset score
				gradeField.setDefaultModelObject(originalGrade);
				
				// reset styles and flags
				baseGradeStyle = FinalGradeColumn.CSS_CLASS;
				gradeSaveStyle = null;
				styleGradeCell(gradeField);
				notifications.clear();

				// apply any applicable flags
				refreshNotifications();

				// tell the javascript to refresh the cell
				OwlGbUtils.getParentCellFor(getComponent(), PARENT_ID).ifPresent(target::add);
				target.add(page.updateLiveGradingMessage(LiveGradingMessage.SAVED));
			}
		});
		
		refreshNotifications();
		styleGradeCell(this);
		
		// menu
		final WebMarkupContainer menu = new WebMarkupContainer("menu");
		menu.add(new GbAjaxLink<String>("finalGradeOverrideLog", Model.of(studentUuid)) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(final AjaxRequestTarget target) {
				final GbModalWindow window = page.getGradeOverrideLogWindow();
				window.setComponentToReturnFocusTo(OwlGbUtils.getParentCellFor(this, PARENT_ID).orElse(null));
				window.setContent(new CourseGradeOverrideLogPanel(window.getContentId(), getModel(), window));
				window.showUnloadConfirmation(false);
				window.show(target);
			}
		});
		add(menu);
	}
	
	protected String formatGrade(GbCourseGrade gbcg)
	{
		return FinalGradeFormatter.format(gbcg);
	}
	
	/**
	 * Builder for styling the cell. Aware of the cell 'save style' as well as if it has overrides and adds styles accordingly
	 *
	 * @param gradeField the cell to style
	 */
	private void styleGradeCell(final Component gradeField) {

		final List<String> cssClasses = new ArrayList<>();
		cssClasses.add(baseGradeStyle); // always
		OwlGbCourseGrade.getOverride(getModelObject().info.getCourseGrade()).ifPresent(o -> cssClasses.add("gb-cg-override"));
		if (gradeSaveStyle != null)
		{
			cssClasses.add(gradeSaveStyle.getCss()); // the particular style for this cell that has been computed previously
		}

		// replace the cell styles with the new set
		OwlGbUtils.getParentCellFor(gradeField, PARENT_ID)
				.ifPresent(p -> p.add(AttributeModifier.replace("class", StringUtils.join(cssClasses, " "))));
	}
	
	private void markSuccessful(final Component gradeField)
	{
		gradeSaveStyle = GradeCellSaveStyle.SUCCESS;
		styleGradeCell(gradeField);
	}

	private void markError(final Component gradeField)
	{
		gradeSaveStyle = GradeCellSaveStyle.ERROR;
		styleGradeCell(gradeField);
		notifications.add(FinalGradeCellNotification.ERROR);
	}

	private void markWarning(final Component gradeField)
	{
		gradeSaveStyle = GradeCellSaveStyle.WARNING;
		styleGradeCell(gradeField);
		notifications.add(FinalGradeCellNotification.INVALID);
	}
	
	private void refreshNotifications()
	{
		final WebMarkupContainer warningNotification = new WebMarkupContainer("warningNotification");
		final WebMarkupContainer errorNotification = new WebMarkupContainer("errorNotification");

		warningNotification.setVisible(false);
		errorNotification.setVisible(false);

		if (!notifications.isEmpty())
		{
			if (notifications.contains(FinalGradeCellNotification.ERROR))
			{
				errorNotification.setVisible(true);
				addPopover(errorNotification, notifications);
			}
			else if (notifications.contains(FinalGradeCellNotification.INVALID))
			{
				warningNotification.setVisible(true);
				addPopover(warningNotification, notifications);
			}
		}

		addOrReplace(warningNotification);
		addOrReplace(errorNotification);
	}
	
	private void addPopover(final Component component, final List<FinalGradeCellNotification> notifications)
	{
		FinalGradePopoverData data = new FinalGradePopoverData();
		data.notifications = notifications;
		data.studentUuid = getModelObject().info.getStudentUuid();
		FinalGradeItemPopoverPanel popover = new FinalGradeItemPopoverPanel("popover", Model.of(data));
		final String popoverString = ComponentRenderer.renderComponent(popover).toString();

		component.add(new AttributeModifier("data-toggle", "popover"));
		component.add(new AttributeModifier("data-trigger", "manual"));
		component.add(new AttributeModifier("data-placement", "bottom"));
		component.add(new AttributeModifier("data-html", "true"));
		component.add(new AttributeModifier("data-container", "#gradebookGrades"));
		component.add(new AttributeModifier("data-content", popoverString));
		component.add(new AttributeModifier("tabindex", "0"));
	}
}
