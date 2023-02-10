package org.sakaiproject.gradebookng.tool.owl.component.table.columns;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.sakaiproject.gradebookng.business.owl.OwlGbStudentGradeInfo;
import org.sakaiproject.gradebookng.tool.owl.panels.FinalGradeColumnHeaderPanel;
import org.sakaiproject.gradebookng.tool.owl.panels.FinalGradeItemCellPanel;

/**
 * 
 * @author plukasew
 * @param <S> 
 */
public class FinalGradeColumn<S> extends AbstractColumn<OwlGbStudentGradeInfo, S>
{
	public static final String CSS_CLASS = "gb-final-grade";
	private FinalGradeColumnHeaderPanel header;
	
	public FinalGradeColumn()
	{
		super(Model.of(""));
	}
	
	@Override
	public Component getHeader(final String componentId)
	{
		header = new FinalGradeColumnHeaderPanel(componentId);
		return header;
	}
	
	@Override
	public String getCssClass()
	{
		return CSS_CLASS;
	}
	
	@Override
	public void populateItem(Item<ICellPopulator<OwlGbStudentGradeInfo>> cellItem, final String componentId, IModel<OwlGbStudentGradeInfo> rowModel)
	{	
		cellItem.add(new AttributeModifier("tabindex", 0));
		cellItem.add(new FinalGradeItemCellPanel(componentId, rowModel));
		cellItem.setOutputMarkupId(true);
	}

	public Component updateLiveGradingMessage(final LiveGradingMessage message)
	{
		if (header != null)
		{
			return header.updateLiveGradingMessage(message);
		}

		return null;
	}

	public enum LiveGradingMessage
	{
		SAVING("feedback.saving", ""), SAVED("grades.saved", "gb-live-feedback-saved"), ERROR("grades.saved.error", "gb-live-feedback-error");

		public String key, cssClass;

		private LiveGradingMessage(String key, String cssClass)
		{
			this.key = key;
			this.cssClass = cssClass;
		}
	}
}
