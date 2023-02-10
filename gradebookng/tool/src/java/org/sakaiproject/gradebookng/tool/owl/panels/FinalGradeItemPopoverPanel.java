package org.sakaiproject.gradebookng.tool.owl.panels;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.sakaiproject.gradebookng.tool.owl.panels.FinalGradeItemCellPanel.FinalGradeCellNotification;
import org.sakaiproject.gradebookng.tool.owl.panels.FinalGradeItemPopoverPanel.FinalGradePopoverData;

/**
 *
 * @author plukasew
 */
public class FinalGradeItemPopoverPanel extends GenericPanel<FinalGradePopoverData>
{	
	public FinalGradeItemPopoverPanel(String id, IModel<FinalGradePopoverData> model)
	{
		super(id, model);
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		
		FinalGradePopoverData data = getModelObject();
		
		final WebMarkupContainer closePopoverLink = new WebMarkupContainer("closePopoverLink");
		//closePopoverLink.add(new AttributeModifier("data-assignmentid", (Long) modelData.get("assignmentId")));
		closePopoverLink.add(new AttributeModifier("data-studentUuid", data.studentUuid));
		add(closePopoverLink);
		
		final WebMarkupContainer saveErrorNotification = new WebMarkupContainer("saveErrorNotification");
		saveErrorNotification.setVisible(data.notifications.contains(FinalGradeCellNotification.ERROR));
		saveErrorNotification.add(new Label("message", new ResourceModel(FinalGradeCellNotification.ERROR.getMessageKey())));
		add(saveErrorNotification);
		
		final WebMarkupContainer isInvalidNotification = new WebMarkupContainer("isInvalidNotification");
		isInvalidNotification.setVisible(data.notifications.contains(FinalGradeCellNotification.INVALID));
		isInvalidNotification.add(new Label("message", new ResourceModel(FinalGradeCellNotification.INVALID.getMessageKey())));
		add(isInvalidNotification);
		
	}
	
	public static class FinalGradePopoverData implements Serializable
	{
		public List<FinalGradeCellNotification> notifications = Collections.emptyList();
		public String studentUuid = "";
	}
}
