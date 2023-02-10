package org.sakaiproject.gradebookng.tool.owl.component.table.columns;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.sakaiproject.gradebookng.business.owl.OwlGbStudentGradeInfo;
import org.sakaiproject.gradebookng.tool.owl.panels.UserIdColumnHeaderPanel;

/**
 *
 * @author plukasew
 */
public class UserIdColumn<S> extends AbstractColumn<OwlGbStudentGradeInfo, S>
{
	public UserIdColumn()
	{
		super(Model.of("userIdColumn"));
	}

	@Override
	public Component getHeader(final String componentId)
	{
		return new UserIdColumnHeaderPanel(componentId);
	}

	@Override
	public void populateItem(final Item<ICellPopulator<OwlGbStudentGradeInfo>> cellItem, final String componentId, final IModel<OwlGbStudentGradeInfo> rowModel)
	{
		final OwlGbStudentGradeInfo student = rowModel.getObject();

		// In OWL we do not have a ContextualUserDisplayService or a DisplayAdvisor, so displayId = eid
		Model<String> userIdModel = Model.of(student.info.getStudentDisplayId());
		
		cellItem.add(new Label(componentId, userIdModel));
		cellItem.add(new AttributeModifier("data-studentUuid", student.info.getStudentUuid()));
		cellItem.add(new AttributeModifier("abbr", userIdModel));
		cellItem.add(new AttributeModifier("aria-label", userIdModel));
	}

	@Override
	public String getCssClass()
	{
		return "gb-userid-cell";
	}
}
