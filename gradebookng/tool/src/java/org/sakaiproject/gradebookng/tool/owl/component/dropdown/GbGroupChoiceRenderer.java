package org.sakaiproject.gradebookng.tool.owl.component.dropdown;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.sakaiproject.gradebookng.business.model.GbGroup;

/**
 *
 * @author plukasew
 */
public class GbGroupChoiceRenderer extends ChoiceRenderer<GbGroup>
{
	@Override
	public Object getDisplayValue(final GbGroup g)
	{
		return g.getTitle();
	}

	@Override
	public String getIdValue(final GbGroup g, final int index)
	{
		return g.getId();
	}
}
