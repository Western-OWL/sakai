package org.sakaiproject.gradebookng.tool.owl.component.dropdown;

import org.apache.wicket.ajax.attributes.AjaxCallListener;

/**
 *
 * @author plukasew
 */
public class SakaiSpinningSelectAjaxCallListener extends AjaxCallListener
{
	// OWLTODO: fix all this duplication
	
	private static final String DISABLED = "$('#%s').addClass('fakeDisabledSelect');";
	private static final String ENABLED = "$('#%s').removeClass('fakeDisabledSelect');";
	private static final String SPIN = "$('#%s').parent().addClass('spinButton');";
	private static final String STOP = "$('#%s').parent().removeClass('spinButton');";
	private static final String DISABLE_AND_SPIN = DISABLED + SPIN;
	private static final String ENABLE_AND_STOP = ENABLED + STOP;
	
	protected boolean willRender;
	protected String id;
	
	public SakaiSpinningSelectAjaxCallListener(String componentMarkupId)
	{
		this(componentMarkupId, false);
	}
	
	public SakaiSpinningSelectAjaxCallListener(String componentMarkupId, boolean componentWillRender)
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
