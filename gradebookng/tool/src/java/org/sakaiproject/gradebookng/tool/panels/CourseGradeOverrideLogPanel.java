package org.sakaiproject.gradebookng.tool.panels;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.model.GbGradeLog;
import org.sakaiproject.gradebookng.business.model.GbUser;
import org.sakaiproject.gradebookng.business.util.FormatHelper;
import org.sakaiproject.gradebookng.tool.component.GbAjaxLink;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;
import org.sakaiproject.gradebookng.tool.pages.GradebookPage;
import org.sakaiproject.gradebookng.tool.pages.IGradesPage;
import org.sakaiproject.service.gradebook.shared.CourseGrade;

/**
 * Panel for the course grade override log window
 *
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 */
public class CourseGradeOverrideLogPanel extends Panel {

	private static final long serialVersionUID = 1L;

	private final ModalWindow window;

	@SpringBean(name = "org.sakaiproject.gradebookng.business.GradebookNgBusinessService")
	protected GradebookNgBusinessService businessService;

	public CourseGradeOverrideLogPanel(final String id, final IModel<ModelData> model, final ModalWindow window) {
		super(id, model);
		this.window = window;
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		// unpack model
		ModelData data = (ModelData) getDefaultModelObject();
		final String studentUuid = data.studentUuid;

		// heading
		CourseGradeOverrideLogPanel.this.window.setTitle(getTitleModel().getString());

		// get the course grade
		final CourseGrade courseGrade = this.businessService.getCourseGrade(studentUuid);

		// get the events
		List<GbGradeLog> gradeLog;

		// if course grade is null we don't have any override events to show
		if (courseGrade.getId() == null) {
			gradeLog = Collections.emptyList();
		} else {
			gradeLog = this.businessService.getGradeLog(studentUuid, courseGrade.getId());
		}

		// render list
		final ListView<GbGradeLog> listView = new ListView<GbGradeLog>("log", gradeLog) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(final ListItem<GbGradeLog> item) {

				final GbGradeLog gradeLog = item.getModelObject();

				// add the entry
				item.add(new Label("entry", formatLogEntry(gradeLog)).setEscapeModelStrings(false));
			}
		};
		add(listView);

		// no entries
		String noEntriesKey = data.forFinalGrades ? "finalgrades.log.none" : "coursegrade.log.none";
		final Label emptyLabel = new Label("empty", new ResourceModel(noEntriesKey));
		emptyLabel.setVisible(gradeLog.isEmpty());
		add(emptyLabel);

		// done button
		add(new GbAjaxLink("done") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(final AjaxRequestTarget target) {
				CourseGradeOverrideLogPanel.this.window.close(target);
			}
		});
	}

	/**
	 * Helper to format a grade log entry
	 *
	 * @param gradeLog
	 * @return
	 */
	private String formatLogEntry(final GbGradeLog gradeLog) {

		final String logDate = FormatHelper.formatDateTime(gradeLog.getDateGraded());
		final String grade = gradeLog.getGrade();

		final GbUser grader = CourseGradeOverrideLogPanel.this.businessService.getUser(gradeLog.getGraderUuid());
		final String graderDisplayId = (grader != null) ? grader.getDisplayId() : getString("unknown.user.id");

		String rval;
		
		ModelData data = (ModelData) getDefaultModelObject();
		String setKey = data.forFinalGrades ? "finalgrades.log.entry.set" : "coursegrade.log.entry.set";
		String unsetKey = data.forFinalGrades ? "finalgrades.log.entry.unset" : "coursegrade.log.entry.unset";

		// if no grade, it is a reset
		if (StringUtils.isNotBlank(grade)) {
			rval = new StringResourceModel(setKey, null, new Object[] { logDate, grade, graderDisplayId }).getString();
		} else {
			rval = new StringResourceModel(unsetKey, null, new Object[] { logDate, graderDisplayId }).getString();
		}

		return rval;

	}
	
	private StringResourceModel getTitleModel()
	{
		ModelData data = (ModelData) getDefaultModelObject();
		String key = data.forFinalGrades ? "finalgrades.heading.gradelog" : "heading.coursegradelog";
		String anonKey = data.forFinalGrades ? "finalgrades.heading.gradelog.anonymous" : "heading.coursegradelog.anonymous";
		
		GradebookUiSettings settings = ((IGradesPage)getPage()).getUiSettings();
		GbUser user = businessService.getUser(data.studentUuid);
		if (user == null)
		{
			user = GbUser.forDisplayOnly("", "");
		}
		if (settings.isContextAnonymous())
		{
			return new StringResourceModel(anonKey, null, new Object[] { user.getAnonId(settings) });
		}

		return new StringResourceModel(key, null, new Object[] { user.getDisplayName(), user.getDisplayId() });
	}
	
	public static class ModelData implements Serializable
	{
		public String studentUuid = "";
		public boolean forFinalGrades = false;
	}
}
