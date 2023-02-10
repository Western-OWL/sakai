package org.sakaiproject.gradebookng.tool.owl.component.dropdown;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.StringResourceModel;

/**
 *
 * @author plukasew
 */
public class SakaiStringResourceChoiceRenderer implements IChoiceRenderer<String>
{
	private final String msgKey;
	private final Component component;
	
	public SakaiStringResourceChoiceRenderer(String msgKey, Component component)
	{
		this.msgKey = msgKey;
		this.component = component;
	}
	
	@Override
	public Object getDisplayValue(String object)
	{
		return new StringResourceModel(msgKey, component, null, new Object[] { object }).getString();
	}

	@Override
	public String getIdValue(String object, int index)
	{
		return object;
	}
}
