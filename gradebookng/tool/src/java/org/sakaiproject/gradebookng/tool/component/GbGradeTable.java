/**
 * Copyright (c) 2003-2017 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.gradebookng.tool.component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.gradebookng.tool.actions.Action;
import org.sakaiproject.gradebookng.tool.actions.ActionResponse;
import org.sakaiproject.gradebookng.tool.actions.DeleteAssignmentAction;
import org.sakaiproject.gradebookng.tool.actions.EditAssignmentAction;
import org.sakaiproject.gradebookng.tool.actions.EditCommentAction;
import org.sakaiproject.gradebookng.tool.actions.EditSettingsAction;
import org.sakaiproject.gradebookng.tool.actions.ExcuseGradeAction;
import org.sakaiproject.gradebookng.tool.actions.GradeUpdateAction;
import org.sakaiproject.gradebookng.tool.actions.MoveAssignmentLeftAction;
import org.sakaiproject.gradebookng.tool.actions.MoveAssignmentRightAction;
import org.sakaiproject.gradebookng.tool.actions.OverrideCourseGradeAction;
import org.sakaiproject.gradebookng.tool.actions.SetScoreForUngradedAction;
import org.sakaiproject.gradebookng.tool.actions.SetStudentNameOrderAction;
import org.sakaiproject.gradebookng.tool.actions.SetZeroScoreAction;
import org.sakaiproject.gradebookng.tool.actions.ToggleCourseGradePoints;
import org.sakaiproject.gradebookng.tool.actions.ViewAssignmentStatisticsAction;
import org.sakaiproject.gradebookng.tool.actions.ViewCourseGradeLogAction;
import org.sakaiproject.gradebookng.tool.actions.ViewCourseGradeStatisticsAction;
import org.sakaiproject.gradebookng.tool.actions.ViewGradeLogAction;
import org.sakaiproject.gradebookng.tool.actions.ViewGradeSummaryAction;
import org.sakaiproject.gradebookng.tool.actions.ViewRubricGradeAction;
import org.sakaiproject.gradebookng.tool.actions.owl.OwlEmptyAction;
import org.sakaiproject.gradebookng.tool.model.GbGradeTableData;
import org.sakaiproject.gradebookng.tool.model.GbGradebookData;
import org.sakaiproject.gradebookng.tool.pages.GradebookPage;

public class GbGradeTable extends GenericPanel<GbGradeTableData> {

	@SpringBean(name = "org.sakaiproject.component.api.ServerConfigurationService")
	protected ServerConfigurationService serverConfigService;

	private Component component;

	/*
	 * - Students: id, first name, last name, netid - Course grades column: is released?, course grade - course grade value for each student
	 * (letter, percentage, points) - assignment header: id, points, due date, category {id, name, color}, included in course grade?,
	 * external? - categories: enabled? weighted categories? normal categories? handle uncategorized - scores: number, has comments?, extra
	 * credit? (> total points), read only?
	 */

	private Map<String, Action> listeners = new HashMap<String, Action>();

	public void addEventListener(final String event, final Action listener) {
		listeners.put(event, listener);
	}

	public ActionResponse handleEvent(final String event, final JsonNode params, final AjaxRequestTarget target) {
		if (!listeners.containsKey(event)) {
			throw new RuntimeException("Missing AJAX handler");
		}

		return listeners.get(event).handleEvent(params, target);
	}

	public GbGradeTable(final String id, final IModel<GbGradeTableData> model) {
		super(id);

		setDefaultModel(model);

		component = new WebMarkupContainer("gradeTable").setOutputMarkupId(true);

		component.add(new AjaxEventBehavior("gbgradetable.action") {
			@Override
			protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				attributes.getDynamicExtraParameters()
						.add("return [{\"name\": \"ajaxParams\", \"value\": JSON.stringify(attrs.event.extraData)}]");
			}

			@Override
			protected void onEvent(final AjaxRequestTarget target) {
				try {
					final ObjectMapper mapper = new ObjectMapper();
					final JsonNode params = mapper.readTree(getRequest().getRequestParameters().getParameterValue("ajaxParams").toString());

					final ActionResponse response = handleEvent(params.get("action").asText(), params, target);

					target.appendJavaScript(String.format("GbGradeTable.ajaxComplete(%d, '%s', %s);",
							params.get("_requestId").intValue(), response.getStatus(), response.toJson()));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});

		add(component);
	}

	// OWL
	@Override
	public void onInitialize()
	{
		super.onInitialize();

		// hack out the student messager webcomponent for anon
		String msg = "<a href=\"javascript:void(0);\" class=\"gb-message-students\" role=\"menuitem\" data-assignment-id=\"${assignmentId}\">%s</a>";
		String title = new ResourceModel("label.submission-messager.title").getObject();
		add(new Label("messageStudents", String.format(msg, title)).setEscapeModelStrings(false).setVisible(false));
		add(new WebMarkupContainer("subMsg").setVisible(false));

		// also remove the course grade statistics menu item
		boolean showStats = serverConfigService.getBoolean("gradebookng.showCourseGradeStatistics", true);
		WebMarkupContainer cgStats = new WebMarkupContainer("cgStats");
		cgStats.add(new Label("cgStatsMsg", new ResourceModel("coursegrade.option.viewcoursegradestatistics")).setRenderBodyOnly(true));
		add(cgStats.setVisible(showStats));

		// OWL - attach event listeners, replacing with empty action as required for anon grading support
		// in anonymous gradebooks the student and/or course grade columns can be hidden using CSS
		// to prevent anyone from tampering with the CSS and exposing these actions, we disable them on the server side
		boolean anon = ((GradebookPage) getPage()).getOwlUiSettings().isContextAnonymous();
		boolean hideCourseGrade = ((GradebookPage) getPage()).getOwlUiSettings().isCourseGradeHiddenInCurrentContext();
		addEventListener("setScore", new GradeUpdateAction());
		addEventListener("gradeRubric", new ViewRubricGradeAction());
		addEventListener("viewLog", new ViewGradeLogAction());
		addEventListener("editAssignment", new EditAssignmentAction());
		addEventListener("viewStatistics", new ViewAssignmentStatisticsAction());
		addEventListener("overrideCourseGrade", hideCourseGrade ? new OwlEmptyAction() : new OverrideCourseGradeAction());
		addEventListener("editComment", new EditCommentAction());
		addEventListener("viewGradeSummary", anon ? new OwlEmptyAction() : new ViewGradeSummaryAction());
		addEventListener("setZeroScore", hideCourseGrade ? new OwlEmptyAction() : new SetZeroScoreAction());
		addEventListener("viewCourseGradeLog", hideCourseGrade ? new OwlEmptyAction() : new ViewCourseGradeLogAction());
		addEventListener("deleteAssignment", new DeleteAssignmentAction());
		addEventListener("setUngraded", new SetScoreForUngradedAction());
		addEventListener("setStudentNameOrder", anon ? new OwlEmptyAction() : new SetStudentNameOrderAction());
		addEventListener("toggleCourseGradePoints", hideCourseGrade ? new OwlEmptyAction() : new ToggleCourseGradePoints());
		addEventListener("editSettings", new EditSettingsAction());
		addEventListener("moveAssignmentLeft", new MoveAssignmentLeftAction());
		addEventListener("moveAssignmentRight", new MoveAssignmentRightAction());
		addEventListener("viewCourseGradeStatistics", new ViewCourseGradeStatisticsAction());
		addEventListener("excuseGrade", new ExcuseGradeAction());
	}

	// OWL
	public String getLoadTableDataJs() {
		final GbGradebookData gradebookData = new GbGradebookData(getModelObject(), this);

		return String.format("GbGradeTable.loadTemplates(); var tableData = %s; GbGradeTable.renderTable('%s', tableData);", gradebookData.toScript(), component.getMarkupId());
	}
}
