package org.sakaiproject.gradebookng.tool.owl.behavior;

import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;

/**
 *
 * @author plukasew
 */
public abstract class ScoreChangeBehavior extends AjaxFormComponentUpdatingBehavior
{
	public static final String SCORE_CHANGE_EVENT = "scorechange.sakai";
	
	final String studentUuid;
	final String cellMarkupId;
	
	public ScoreChangeBehavior(String studentUuid, String cellMarkupId)
	{
		super(SCORE_CHANGE_EVENT);
		this.studentUuid = studentUuid;
		this.cellMarkupId = cellMarkupId;
	}
	
	@Override
	protected void updateAjaxAttributes(final AjaxRequestAttributes attributes)
	{
		super.updateAjaxAttributes(attributes);

		final Map<String, Object> extraParameters = attributes.getExtraParameters();
		extraParameters.put("studentUuid", studentUuid);

		final AjaxCallListener myAjaxCallListener = new AjaxCallListener()
		{
			@Override
			public CharSequence getPrecondition(final Component component) {
				return "return GradebookWicketEventProxy.updateGradeItem.handlePrecondition('"
						+ cellMarkupId + "', attrs);";
			}

			@Override
			public CharSequence getBeforeSendHandler(final Component component) {
				return "GradebookWicketEventProxy.updateGradeItem.handleBeforeSend('"
						+ cellMarkupId + "', attrs, jqXHR, settings);";
			}

			@Override
			public CharSequence getSuccessHandler(final Component component) {
				return "GradebookWicketEventProxy.updateGradeItem.handleSuccess('" + cellMarkupId
						+ "', attrs, jqXHR, data, textStatus);";
			}

			@Override
			public CharSequence getFailureHandler(final Component component) {
				return "GradebookWicketEventProxy.updateGradeItem.handleFailure('" + cellMarkupId
						+ "', attrs, jqXHR, errorMessage, textStatus);";
			}

			@Override
			public CharSequence getCompleteHandler(final Component component) {
				return "GradebookWicketEventProxy.updateGradeItem.handleComplete('" + cellMarkupId
						+ "', attrs, jqXHR, textStatus);";
			}
		};
		attributes.getAjaxCallListeners().add(myAjaxCallListener);
	}
}
