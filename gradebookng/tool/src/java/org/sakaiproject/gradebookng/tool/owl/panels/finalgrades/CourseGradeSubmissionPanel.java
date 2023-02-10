package org.sakaiproject.gradebookng.tool.owl.panels.finalgrades;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.gradebookng.tool.owl.panels.finalgrades.CourseGradeSubmissionPanel.CourseGradeSubmissionData;
import org.sakaiproject.gradebookng.tool.owl.panels.finalgrades.SectionStatisticsPanel.SectionStats;
import org.sakaiproject.gradebookng.tool.owl.panels.finalgrades.SubmissionHistoryPanel.SubmissionHistory;
import org.sakaiproject.gradebookng.tool.owl.panels.finalgrades.SubmitAndApprovePanel.SubmitAndApproveStatus;

/**
 *
 * @author plukasew
 */
public class CourseGradeSubmissionPanel extends GenericPanel<CourseGradeSubmissionData>
{
	private SectionStatisticsPanel statsPanel;
	private SubmissionHistoryPanel historyPanel;
	private SubmitAndApprovePanel submitPanel;
	private Label sectionExcludedMsg;
	
	public CourseGradeSubmissionPanel(String id, IModel<CourseGradeSubmissionData> model)
	{
		super(id, model);
	}
	
	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		
		IModel<CourseGradeSubmissionData> dataModel = getModel();
		add(statsPanel = new SectionStatisticsPanel("sectionStatisticsPanel", dataModel));
		add(historyPanel = new SubmissionHistoryPanel("submissionHistoryPanel", dataModel));
		historyPanel.setOutputMarkupPlaceholderTag(true);
		add(submitPanel = new SubmitAndApprovePanel("submitAndApprovePanel", dataModel));
		submitPanel.setOutputMarkupPlaceholderTag(true);
		add(sectionExcludedMsg = new Label("sectionExcludedMsg",
				new StringResourceModel("finalgrades_submission_disabled", this, null)));
		sectionExcludedMsg.setOutputMarkupPlaceholderTag(true);
		
		refreshVisibility();
	}
	
	private void refreshVisibility()
	{
		boolean excluded = getData().isSectionExcluded();
		historyPanel.setVisible(!excluded);
		submitPanel.setVisible(!excluded);
		sectionExcludedMsg.setVisible(excluded);
	}
	
	
	public CourseGradeSubmissionData getData()
	{
		return getModelObject();
	}
	
	public void redrawButtons(AjaxRequestTarget target)
	{
		submitPanel.setDefaultModelObject(getData());
		target.add(submitPanel);
	}
	
	public void redrawStats(AjaxRequestTarget target)
	{
		statsPanel.setDefaultModelObject(getData());
		statsPanel.redraw(target);
	}
	
	public void redrawHistory(AjaxRequestTarget target)
	{
		historyPanel.setDefaultModelObject(getData());
		target.add(historyPanel);
	}
	
	public void redrawFeedback(AjaxRequestTarget target)
	{
		submitPanel.redrawFeedback(target);
	}
	
	public void redrawForSectionChange(AjaxRequestTarget target)
	{
		refreshVisibility();
		redrawButtons(target);
		redrawStats(target);
		redrawHistory(target);
		target.add(sectionExcludedMsg);
	}
	
	public static class CourseGradeSubmissionData implements Serializable
	{
		@Getter @Setter
		private SectionStats stats;
		
		@Getter @Setter
		private SubmissionHistory history;
		
		@Getter @Setter
		private SubmitAndApproveStatus buttonStatus;
		
		@Getter @Setter
		private boolean sectionExcluded;
	}
}
