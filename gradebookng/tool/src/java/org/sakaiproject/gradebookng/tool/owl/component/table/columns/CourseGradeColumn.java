package org.sakaiproject.gradebookng.tool.owl.component.table.columns;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.sakaiproject.gradebookng.business.GbRole;
import org.sakaiproject.gradebookng.business.owl.OwlGbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.owl.finalgrades.OwlCourseGradeFormatter;
import org.sakaiproject.gradebookng.tool.owl.panels.CourseGradeColumnHeaderPanel;
import org.sakaiproject.gradebookng.tool.owl.panels.CourseGradeItemCellPanel;
import org.sakaiproject.tool.gradebook.Gradebook;


/**
 * Column to display the calculated course grade on the Final Grades page
 * @author plukasew
 */
public class CourseGradeColumn<S> extends AbstractColumn<OwlGbStudentGradeInfo, S>
{
	private final OwlCourseGradeFormatter cfg;
	
	public CourseGradeColumn(final Gradebook gb, final GbRole currentUserRole, boolean isCourseGradeVisibleToCurrentUser)
	{
		super(Model.of(""));
		cfg = new OwlCourseGradeFormatter(gb, currentUserRole, isCourseGradeVisibleToCurrentUser, false, false);
	}
	
	@Override
	public Component getHeader(final String componentId)
	{
		return new CourseGradeColumnHeaderPanel(componentId);
	}

	@Override
	public String getCssClass()
	{
		return "gb-course-grade";
	}

	@Override
	public void populateItem(final Item<ICellPopulator<OwlGbStudentGradeInfo>> cellItem, final String componentId, final IModel<OwlGbStudentGradeInfo> rowModel) {
		final OwlGbStudentGradeInfo studentGradeInfo = rowModel.getObject();

		cellItem.add(new AttributeModifier("tabindex", 0));
		cellItem.add(new CourseGradeItemCellPanel(componentId, Model.of(studentGradeInfo.info.getCourseGrade()), cfg));
		cellItem.setOutputMarkupId(true);
	}
}
