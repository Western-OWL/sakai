package org.sakaiproject.gradebookng.tool.owl.behavior;

import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;

/**
 *
 * @author plukasew
 */
public abstract class RevertScoreBehavior extends AjaxEventBehavior
{
	public static final String REVERT_SCORE_EVENT = "revertscore.sakai";
	
	final String cellMarkupId;
	final String studentUuid;
	
	public RevertScoreBehavior(String studentUuid, String cellMarkupId)
	{
		super(REVERT_SCORE_EVENT);
		this.cellMarkupId = cellMarkupId;
		this.studentUuid = studentUuid;
	}
	
	@Override
	protected void updateAjaxAttributes(final AjaxRequestAttributes attributes)
	{
		super.updateAjaxAttributes(attributes);

		final Map<String, Object> extraParameters = attributes.getExtraParameters();
		extraParameters.put("studentUuid", studentUuid);

		final AjaxCallListener myAjaxCallListener = new AjaxCallListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public CharSequence getCompleteHandler(final Component component) {
				return "GradebookWicketEventProxy.revertGradeItem.handleComplete('" + cellMarkupId
						+ "', attrs, jqXHR, textStatus);";
			}
		};
		attributes.getAjaxCallListeners().add(myAjaxCallListener);
	}
}
