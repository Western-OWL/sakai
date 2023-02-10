package org.sakaiproject.gradebookng.tool.owl.panels.finalgrades;

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.sakaiproject.gradebookng.tool.component.GbFeedbackPanel;
import org.sakaiproject.gradebookng.tool.owl.component.SakaiAjaxButton;
import org.sakaiproject.gradebookng.tool.owl.pages.FinalGradesPage;
import org.sakaiproject.gradebookng.tool.owl.panels.finalgrades.CourseGradeSubmissionPanel.CourseGradeSubmissionData;


/**
 *
 * @author plukasew
 */
public class SubmitAndApprovePanel extends GenericPanel<CourseGradeSubmissionData>
{	
	private Label statusLabel;
	private GbFeedbackPanel feedback;
	private SakaiAjaxButton submitButton, approveButton;
	
	public SubmitAndApprovePanel(String id, IModel<CourseGradeSubmissionData> model)
	{
		super(id, model);
	}
	
	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		
		add(statusLabel = new Label("currentStatus", Model.of("")));
		
		submitButton = new SakaiAjaxButton("submitFinalGrades")
		{
			@Override
			public void onSubmit(final AjaxRequestTarget target, final Form form)
			{
				FinalGradesPage page = (FinalGradesPage) getPage();
				page.submitGrades(target);
			}
			
			@Override
			public boolean isEnabled()
			{
				return getStatus().isSubmitReady();
			}
		};
		submitButton.setWillRenderOnClick(true);
		add(submitButton);
		
		approveButton = new SakaiAjaxButton("approveFinalGrades")
		{
			@Override
			public void onSubmit(final AjaxRequestTarget target, final Form form)
			{
				FinalGradesPage page = (FinalGradesPage) getPage();
				page.approveGrades(target);
			}
			
			@Override
			public boolean isEnabled()
			{
				return getStatus().isApproveReady();
			}
		};
		approveButton.setWillRenderOnClick(true);
		add(approveButton);
		
		add(feedback = new GbFeedbackPanel("submitAndApproveFeedback"));
		feedback.setOutputMarkupId(true);
		feedback.setFilter(new ComponentFeedbackMessageFilter(getParent()));
	}
	
	public void redrawFeedback(AjaxRequestTarget target)
	{
		target.add(feedback);
	}
	
	@Override
	protected void onBeforeRender()
	{	
		super.onBeforeRender();
		
		SubmitAndApproveStatus status = getStatus();
		
		statusLabel.setDefaultModelObject(status.getStatusMsg());
		
		// switching sections can change button visibility, so re-check on each render
		submitButton.setVisible(status.isCanSubmit());
		approveButton.setVisible(status.isCanApprove());
	}
	
	private SubmitAndApproveStatus getStatus()
	{
		return getModelObject().getButtonStatus();
	}
	
	@Builder
	public static class SubmitAndApproveStatus implements Serializable
	{
		@Getter
		private final boolean canSubmit, canApprove, submitReady, approveReady;
		@Getter
		private final String statusMsg;
	}
}
