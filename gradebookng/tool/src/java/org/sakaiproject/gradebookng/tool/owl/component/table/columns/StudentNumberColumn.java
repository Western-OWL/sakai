package org.sakaiproject.gradebookng.tool.owl.component.table.columns;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.sakaiproject.gradebookng.business.owl.OwlGbStudentGradeInfo;
import org.sakaiproject.gradebookng.tool.owl.panels.StudentNumberColumnHeaderPanel;

/**
 *
 * @author plukasew
 */
public class StudentNumberColumn<S> extends AbstractColumn<OwlGbStudentGradeInfo, S>
{
	public static final String STUDENT_NUM_COL_CSS_CLASS = "gb-student-number-cell";
	
	public StudentNumberColumn()
	{
		super(Model.of(""));
	}
	
	@Override
	public Component getHeader(final String componentId)
	{
		return new StudentNumberColumnHeaderPanel(componentId);
	}

	@Override
	public void populateItem(final Item<ICellPopulator<OwlGbStudentGradeInfo>> cellItem, final String componentId, final IModel<OwlGbStudentGradeInfo> rowModel)
	{
		final OwlGbStudentGradeInfo studentGradeInfo = rowModel.getObject();

		String studentNumber = StringUtils.defaultIfBlank(studentGradeInfo.info.getStudentNumber(), "-");

		cellItem.add(new Label(componentId, studentNumber));
		cellItem.add(new AttributeModifier("data-studentUuid", studentGradeInfo.info.getStudentUuid()));
		cellItem.add(new AttributeModifier("abbr", studentNumber));
		cellItem.add(new AttributeModifier("aria-label", studentNumber));
	}

	@Override
	public String getCssClass() {
		return STUDENT_NUM_COL_CSS_CLASS;
	}
}
