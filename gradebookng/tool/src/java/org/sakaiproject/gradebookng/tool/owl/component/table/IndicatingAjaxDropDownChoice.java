package org.sakaiproject.gradebookng.tool.owl.component.table;

import java.util.List;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.extensions.ajax.markup.html.AjaxIndicatorAppender;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

/**
 *
 * @author plukasew
 */
public class IndicatingAjaxDropDownChoice<T> extends DropDownChoice<T> implements IAjaxIndicatorAware
{
	private final AjaxIndicatorAppender indicator = new AjaxIndicatorAppender();
	
	public IndicatingAjaxDropDownChoice(String id, List<T> data, IChoiceRenderer<? super T> renderer)
	{
		super(id, data, renderer);
	}
	
	public IndicatingAjaxDropDownChoice(String id, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer)
	{
		super(id, model, choices, renderer);
	}
	
	public IndicatingAjaxDropDownChoice(String id, IModel<T> model, List<? extends T> choices, IChoiceRenderer<? super T> renderer)
	{
		super(id, model, choices, renderer);
	}
	
	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		add(indicator);
	}
	
	@Override
	public String getAjaxIndicatorMarkupId()
	{
		return indicator.getMarkupId();
	}
}
