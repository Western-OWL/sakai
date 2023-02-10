package org.sakaiproject.gradebookng.tool.owl.component.table.columns;

import java.io.Serializable;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.sakaiproject.gradebookng.business.model.GbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.model.GbStudentNameSortOrder;
import org.sakaiproject.gradebookng.business.owl.OwlGbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.owl.anon.OwlAnonTypes;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;
import org.sakaiproject.gradebookng.tool.owl.model.OwlGbUiSettings;
import org.sakaiproject.gradebookng.tool.owl.pages.FinalGradesPage;
import org.sakaiproject.gradebookng.tool.owl.panels.StudentNameCellPanel;
import org.sakaiproject.gradebookng.tool.owl.panels.StudentNameColumnHeaderPanel;

/**
 *
 * @author plukasew
 */
public class StudentNameColumn<S> extends AbstractColumn<OwlGbStudentGradeInfo, S>
{
	public static final String STUDENT_COL_CSS_CLASS = "gb-student-cell";
	public static final String STUDENT_COL_CSS_CLASS_ANON = "gb-student-cell-anon";
	
	private final FinalGradesPage page;
	
	public StudentNameColumn(FinalGradesPage page)
	{
		super(Model.of("studentNameColumn"));
		this.page = page;
	}
	
	@Override
	public Component getHeader(final String componentId)
	{
		return new StudentNameColumnHeaderPanel(componentId, Model.of(page.getUiSettings().getNameSortOrder()));
	}

	@Override
	public void populateItem(final Item<ICellPopulator<OwlGbStudentGradeInfo>> cellItem, final String componentId, final IModel<OwlGbStudentGradeInfo> rowModel)
	{
		final OwlGbStudentGradeInfo student = rowModel.getObject();

		GradebookUiSettings settings = page.getUiSettings();
		OwlGbUiSettings owlSettings = page.getOwlUiSettings();

		if (owlSettings.isContextAnonymous())
		{
			Model<String> anonIdModel = Model.of(OwlAnonTypes.forDisplay(student.anonId));
			cellItem.add(new Label(componentId, anonIdModel));
			// consumed by gradebook-grades.js to populate the dropdown tooltips
			cellItem.add(new AttributeModifier("data-studentUuid", student.info.getStudentUuid()));
			cellItem.add(new AttributeModifier("abbr", anonIdModel));
			cellItem.add(new AttributeModifier("aria-label", anonIdModel));
		}
		else
		{
			IModel<StudentNameColumnData> dataModel = Model.of(StudentNameColumnData.from(student.info, settings.getNameSortOrder()));
			cellItem.add(new StudentNameCellPanel(componentId, dataModel));
			cellItem.add(new AttributeModifier("data-studentUuid", student.info.getStudentUuid()));
			cellItem.add(new AttributeModifier("abbr", student.info.getStudentDisplayName()));
			cellItem.add(new AttributeModifier("aria-label", student.info.getStudentDisplayName()));
		}
	}

	@Override
	public String getCssClass()
	{
		return page.getOwlUiSettings().isContextAnonymous() ? STUDENT_COL_CSS_CLASS_ANON : STUDENT_COL_CSS_CLASS;
	}

	public static final class StudentNameColumnData implements Serializable
	{
		public final String firstName, lastName, displayName;
		public final GbStudentNameSortOrder nameOrder;

		private StudentNameColumnData(String firstName, String lastName, String displayName, GbStudentNameSortOrder nameOrder)
		{
			this.firstName = firstName;
			this.lastName = lastName;
			this.displayName = displayName;
			this.nameOrder = nameOrder;
		}

		public static StudentNameColumnData from(GbStudentGradeInfo info, GbStudentNameSortOrder nameOrder)
		{
			return new StudentNameColumnData(info.getStudentFirstName(), info.getStudentLastName(), info.getStudentDisplayName(), nameOrder);
		}
	}
}
