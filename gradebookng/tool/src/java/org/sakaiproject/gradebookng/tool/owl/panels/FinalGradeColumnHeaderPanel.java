package org.sakaiproject.gradebookng.tool.owl.panels;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.sakaiproject.gradebookng.business.SortDirection;
import org.sakaiproject.gradebookng.tool.owl.component.OwlGbUtils;
import org.sakaiproject.gradebookng.tool.owl.component.table.columns.FinalGradeColumn;
import org.sakaiproject.gradebookng.tool.owl.component.table.columns.GbColumnSortToggleLink;
import org.sakaiproject.gradebookng.tool.owl.model.UiSettings;
import org.sakaiproject.gradebookng.tool.owl.pages.IGradesPage;


/**
 *
 * @author plukasew
 */
public class FinalGradeColumnHeaderPanel extends Panel
{
	private static final String PARENT_ID = "header";

	private Label liveGradingFeedback;

	private static final AttributeModifier DISPLAY_NONE = new AttributeModifier("style", "display: none");

	public FinalGradeColumnHeaderPanel(final String id)
	{
		super(id);
	}
	
	@Override
	public void onInitialize()
	{
		super.onInitialize();
		
		final IGradesPage gradebookPage = (IGradesPage) getPage();
		
		OwlGbUtils.getParentCellFor(this, PARENT_ID).ifPresent(p -> p.setOutputMarkupId(true));
		
		final GbColumnSortToggleLink title = new GbColumnSortToggleLink("title")
		{
			@Override
			public SortDirection getSort(UiSettings settings)
			{
				return FinalGradeColumnHeaderPanel.this.getSortOrder(settings);
			}
			
			@Override
			public void setSort(UiSettings settings, SortDirection value)
			{
				FinalGradeColumnHeaderPanel.this.setSortOrder(value, settings);
			}
		};
		
		final UiSettings settings = gradebookPage.getGbUiSettings();
		ResourceModel rm = new ResourceModel(getTitleMsgKey());
		title.add(new AttributeModifier("title", rm));
		title.add(new Label("label", rm));
		if (settings != null && getSortOrder(settings) != null)
		{
			title.add(new AttributeModifier("class", "gb-sort-" + getSortOrder(settings).toString().toLowerCase()));
		}
		add(title);

		liveGradingFeedback = new Label("liveGradingFeedback", getString(FinalGradeColumn.LiveGradingMessage.SAVED.key));
		liveGradingFeedback.add(new AttributeModifier("class", FinalGradeColumn.LiveGradingMessage.SAVED.cssClass));
		liveGradingFeedback.setOutputMarkupId(true);
		liveGradingFeedback.add(DISPLAY_NONE);

		// add the 'saving...' message to the DOM as the JavaScript will
		// need to be the one that displays this message (Wicket will handle
		// the 'saved' and 'error' messages when a grade is changed
		liveGradingFeedback.add(new AttributeModifier("data-saving-message", getString(FinalGradeColumn.LiveGradingMessage.SAVING.key)));
		addOrReplace(liveGradingFeedback);

	}
	
	protected String getTitleMsgKey()
	{
		return "column.header.finalgrade";
	}
	
	protected SortDirection getSortOrder(UiSettings settings)
	{
		return settings.owl.getFinalGradeSortOrder();
	}
	
	protected void setSortOrder(SortDirection value, UiSettings settings)
	{
		settings.setSort(UiSettings.GbSortColumn.FINAL_GRADE, value);
	}

	public Component updateLiveGradingMessage(final FinalGradeColumn.LiveGradingMessage message)
	{
		liveGradingFeedback.setDefaultModel(Model.of(getString(message.key)));
		liveGradingFeedback.add(new AttributeModifier("class", message.cssClass));
		if (liveGradingFeedback.getBehaviors().contains(DISPLAY_NONE)) {
			liveGradingFeedback.remove(DISPLAY_NONE);
		}

		return liveGradingFeedback;
	}
}
