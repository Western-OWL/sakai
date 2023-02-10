package org.sakaiproject.gradebookng.tool.owl.panels;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.sakaiproject.gradebookng.business.model.GbCourseGrade;
import org.sakaiproject.gradebookng.business.owl.finalgrades.OwlCourseGradeFormatter;
import org.sakaiproject.gradebookng.tool.owl.component.OwlGbUtils;

/**
 * Panel that is rendered for each student's calculated course grade
 */
public class CourseGradeItemCellPanel extends GenericPanel<GbCourseGrade>
{
	private static final long serialVersionUID = 1L;
	
	private static final String PARENT_ID = "cells";
	
	private final OwlCourseGradeFormatter courseGradeFormatter;

	public CourseGradeItemCellPanel(final String id, final IModel<GbCourseGrade> model, OwlCourseGradeFormatter courseGradeFormatter)
	{
		super(id, model);
		this.courseGradeFormatter = courseGradeFormatter;
	}

	@Override
	public void onInitialize()
	{
		super.onInitialize();

		OwlGbUtils.getParentCellFor(this, PARENT_ID).ifPresent(p -> p.setOutputMarkupId(true));

		final String courseGradeDisplay = courseGradeFormatter.format(getModelObject().getCourseGrade());
		final Label courseGradeLabel = new Label("courseGrade", Model.of(courseGradeDisplay));
		courseGradeLabel.setEscapeModelStrings(false);
		add(courseGradeLabel);
	}
}