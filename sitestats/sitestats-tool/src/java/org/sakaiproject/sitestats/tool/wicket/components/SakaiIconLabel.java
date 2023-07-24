package org.sakaiproject.sitestats.tool.wicket.components;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * A label prefixed with an inline HTML element decorated by a Sakai icon
 * @author plukasew
 */
public class SakaiIconLabel extends Panel
{
	public SakaiIconLabel(String id, IModel<String> cssClass, IModel<String> text)
	{
		super(id);
		Label icon = new Label("icon", Model.of(""));
		icon.add(AttributeModifier.append("class", cssClass.getObject()));
		add(icon);
		add(new Label("label", text).setRenderBodyOnly(true));
	}
}
