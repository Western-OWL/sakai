package org.sakaiproject.gradebookng.tool.owl.component;

import org.apache.wicket.ajax.attributes.AjaxCallListener;

/**
 *
 * @author plukasew
 */
public class SakaiSpinnerAjaxCallListener extends AjaxCallListener
{
	private static final String DISABLED = "$('#%s').prop('disabled', true);";
	private static final String ENABLED = "$('#%s').prop('disabled', false);";
	private static final String SPIN = "$('#%s').addClass('spinButton');";
	private static final String STOP = "$('#%s').removeClass('spinButton');";
	private static final String DISABLE_AND_SPIN = DISABLED + SPIN;
	private static final String ENABLE_AND_STOP = ENABLED + STOP;
	
	protected boolean willRender;
	protected String id;
	
	public SakaiSpinnerAjaxCallListener(String componentMarkupId)
	{
		this(componentMarkupId, false);
	}
	
	public SakaiSpinnerAjaxCallListener(String componentMarkupId, boolean componentWillRender)
	{
		id = componentMarkupId;
		willRender = componentWillRender;
		
		// on the client side, disable the control and show the spinner after click
		onBefore(String.format(DISABLE_AND_SPIN, id, id));
		
		// if the control is re-rendered the disabled property will be set by wicket and the spinner
		// class will not be on the button as wicket doesn't know about it
		if (!willRender)
		{
			onComplete(String.format(ENABLE_AND_STOP, id, id));
		}
	}
	
	
}
