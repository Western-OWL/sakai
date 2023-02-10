package org.sakaiproject.gradebookng.tool.owl.pages;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.gradebookng.business.GbRole;
import org.sakaiproject.gradebookng.business.SortDirection;
import org.sakaiproject.gradebookng.business.owl.finalgrades.CourseGradeStatistics;
import org.sakaiproject.gradebookng.business.owl.finalgrades.CourseGradeSubmissionPresenter;
import org.sakaiproject.gradebookng.business.owl.finalgrades.CourseGradeSubmitter;
import org.sakaiproject.gradebookng.business.owl.finalgrades.CourseGradeSubmitter.GradeChangeReport;
import org.sakaiproject.gradebookng.business.owl.finalgrades.CourseGradeSubmitter.SubmissionHistoryRow;
import org.sakaiproject.gradebookng.business.model.GbGroup;
import org.sakaiproject.gradebookng.business.owl.OwlBusinessService;
import org.sakaiproject.gradebookng.business.owl.OwlGbStopWatch;
import org.sakaiproject.gradebookng.business.owl.OwlGbStudentGradeInfo;
import org.sakaiproject.gradebookng.tool.owl.component.table.GbBaseHeadersToolbar;
import org.sakaiproject.gradebookng.tool.model.GbModalWindow;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;
import org.sakaiproject.gradebookng.tool.owl.model.OwlGbUiSettings;
import org.sakaiproject.gradebookng.tool.owl.component.GbBaseGradesDisplayToolbar;
import org.sakaiproject.gradebookng.tool.owl.component.table.GbGradesDataProvider;
import org.sakaiproject.gradebookng.tool.owl.component.table.FinalGradesDataTable;
import org.sakaiproject.gradebookng.tool.owl.component.table.columns.CourseGradeColumn;
import org.sakaiproject.gradebookng.tool.owl.component.table.columns.FinalGradeColumn;
import org.sakaiproject.gradebookng.tool.owl.component.table.columns.FinalGradeColumn.LiveGradingMessage;
import org.sakaiproject.gradebookng.tool.owl.component.table.columns.StudentNameColumn;
import org.sakaiproject.gradebookng.tool.owl.component.table.columns.StudentNumberColumn;
import org.sakaiproject.gradebookng.tool.owl.component.table.columns.UserIdColumn;
import org.sakaiproject.gradebookng.tool.owl.model.UiSettings;
import org.sakaiproject.gradebookng.tool.owl.panels.finalgrades.CourseGradeSubmissionPanel;
import org.sakaiproject.gradebookng.tool.owl.panels.finalgrades.CourseGradeSubmissionPanel.CourseGradeSubmissionData;
import org.sakaiproject.gradebookng.tool.owl.panels.finalgrades.SectionStatisticsPanel.SectionStats;
import org.sakaiproject.gradebookng.tool.owl.panels.finalgrades.SubmissionHistoryPanel.SubmissionHistory;
import org.sakaiproject.gradebookng.tool.owl.panels.finalgrades.SubmitAndApprovePanel.SubmitAndApproveStatus;
import org.sakaiproject.gradebookng.tool.pages.BasePage;
import org.sakaiproject.gradebookng.tool.pages.GradebookPage;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeSubmissionGrades;
import org.sakaiproject.tool.gradebook.Gradebook;

/**
 *
 * @author plukasew
 */
public class FinalGradesPage extends BasePage implements IGradesPage
{
	private static final String GB_UI_SETTINGS_KEY = "OWL_FG_GB_UI_SETTINGS";
	private static final String OWL_FG_UI_SETTINGS_KEY = "OWL_FG_UI_SETTINGS";

	private static final String OWL_FG_STYLES_PATH = "/gradebookng-tool/styles/owl";

	private Form<Void> form;
	
	private WebMarkupContainer spreadsheet;
	
	private FinalGradesDataTable table;
	private FinalGradeColumn finalGradeCol;
	private CourseGradeSubmissionPanel submissionPanel;
	
	private GbModalWindow gradeOverrideLogWindow;
	
	private transient CourseGradeSubmitter submitter;
	private List<GbGroup> sections;

	private Gradebook gradebook;

	public FinalGradesPage()
	{
		// students and TAs cannot access this page, redirect them to Grades page
		if (role != GbRole.INSTRUCTOR)
		{
			throw new RestartResponseException(GradebookPage.class);
		}
	}
	
	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		
		OwlGbStopWatch stopwatch = new OwlGbStopWatch("FinalGradesPage");
		stopwatch.time("Starting init");

		gradebook = businessService.getGradebook();
		
		disableLink(finalGradesPageLink);
		
		form = new Form<>("form");
		form.setOutputMarkupId(true);
		add(form);
				
		sections = businessService.owl().getSiteSections();
		CourseGradeSubmissionData data = new CourseGradeSubmissionData();
		updateStatsAndHistoryModel(data);
		form.add(submissionPanel = new CourseGradeSubmissionPanel("courseGradeSubmissionPanel", Model.of(data)));
		submissionPanel.setOutputMarkupId(true);
		
		gradeOverrideLogWindow = new GbModalWindow("gradeOverrideLogWindow");
		form.add(gradeOverrideLogWindow);
		
		final GradebookUiSettings settings = getUiSettings();
		final OwlGbUiSettings owlSettings = getOwlUiSettings();
		owlSettings.setContextAnonymous(businessService.owl().anon.isCourseGradePureAnon());
		if (owlSettings.isContextAnonymous())
		{
			if (owlSettings.getAnonIdSortOrder() == null && owlSettings.getCalculatedSortOrder() == null
					&& owlSettings.getFinalGradeSortOrder() == null)
			{
				// there is no existing sort on the anon-aware columns, so default to anon id to maintain anonymity
				owlSettings.setAnonIdSortOrder(SortDirection.ASCENDING);
			}
		}
		
		if (settings.getGroupFilter() == null)
		{
			settings.setGroupFilter(sections.isEmpty() ? null : sections.get(0));
		}

		final List<OwlGbStudentGradeInfo> grades = businessService.owl().fg.buildGradeMatrixForFinalGrades(new UiSettings(settings, owlSettings));

		// mark the current timestamp so we can use this date to check for any changes since now
		final Date gradesTimestamp = new Date();
		
		final GbGradesDataProvider studentGradeMatrix = new GbGradesDataProvider(grades, this);
		final List<IColumn> cols = new ArrayList<>();
		
		// student name column
		cols.add(new StudentNameColumn(this));

		// user id (eid) column
		if (!owlSettings.isContextAnonymous())
		{
			cols.add(new UserIdColumn());
		}

		// student number column
		if (!owlSettings.isContextAnonymous() && businessService.isStudentNumberVisible())
		{
			cols.add(new StudentNumberColumn());
		}
		
		// course grade column
		cols.add(new CourseGradeColumn(gradebook, getCurrentUserRole(), businessService.isCourseGradeVisible(currentUserUuid)));
		
		cols.add(finalGradeCol = new FinalGradeColumn());
		
		int pageSize = owlSettings.getPageSize();
		table = new FinalGradesDataTable("table", cols, studentGradeMatrix, true, pageSize)
		{
			@Override
			protected Item newCellItem(final String id, final int index, final IModel model) {
				return new Item(id, index, model) {
					@Override
					protected void onComponentTag(final ComponentTag tag) {
						super.onComponentTag(tag);

						final Object modelObject = model.getObject();

						if (modelObject instanceof AbstractColumn && "studentColumn"
								.equals(((AbstractColumn) modelObject).getDisplayModel().getObject())) {
							tag.setName("th");
							tag.getAttributes().put("role", "rowheader");
							tag.getAttributes().put("scope", "row");
						} else {
							tag.getAttributes().put("role", "gridcell");
						}
						tag.getAttributes().put("tabindex", "0");
					}
				};
			}

			@Override
			protected Item newRowItem(final String id, final int index, final IModel model) {
				return new Item(id, index, model) {
					@Override
					protected void onComponentTag(final ComponentTag tag) {
						super.onComponentTag(tag);

						tag.getAttributes().put("role", "row");
					}
				};
			}

			@Override
			protected IModel<String> getCaptionModel() {
				return new StringResourceModel("finalgrades.caption", this, null);
			}
		};

		table.addTopToolbar(new GbBaseHeadersToolbar(table, null));
		table.add(new AttributeModifier("data-siteid", this.businessService.getCurrentSiteId()));
		table.add(new AttributeModifier("data-gradestimestamp", gradesTimestamp.getTime()));
		
		spreadsheet = new WebMarkupContainer("spreadsheet");
		spreadsheet.setOutputMarkupId(true);
		spreadsheet.add(table);

		spreadsheet.add(new GbBaseGradesDisplayToolbar("toolbar", table, sections, !grades.isEmpty()));
		
		form.add(spreadsheet);
		
		stopwatch.time("FinalGradesPage initialized");
	}
	
	public List<GbGroup> getSections()
	{
		return sections;
	}

	public OwlBusinessService getOwlBus()
	{
		return businessService.owl();
	}
	
	@Override
	public void renderHead(final IHeaderResponse response)
	{
		super.renderHead(response);

		final String version = ServerConfigurationService.getString("portal.cdn.version", "");

		response.render(CssHeaderItem.forUrl(String.format("%s/gradebook-finalgrades-vars.css?version=%s", OWL_FG_STYLES_PATH, version)));
		response.render(CssHeaderItem.forUrl(String.format("%s/gradebook-finalgrades.css?version=%s", OWL_FG_STYLES_PATH, version)));

		response.render(JavaScriptHeaderItem.forUrl(String.format("/gradebookng-tool/scripts/owl/gradebook-finalgrades.js?version=%s", version)));
	}

	public Component updateLiveGradingMessage(final LiveGradingMessage message)
	{
		return finalGradeCol.updateLiveGradingMessage(message);
	}
	
	/**
	 * Getter for the GradebookUiSettings. Used to store a few UI related settings for the current session only.
	 *
	 * TODO move this to a helper
	 */
	@Override
	public GradebookUiSettings getUiSettings()
	{
		GradebookUiSettings settings = (GradebookUiSettings) Session.get().getAttribute(GB_UI_SETTINGS_KEY);

		if (settings == null) {
			settings = new GradebookUiSettings();
			setUiSettings(settings);
		}

		return settings;
	}

	@Override
	public GradebookUiSettings getUiSettings(final Gradebook gradebook)
	{
		return getUiSettings();
	}

	@Override
	public void setUiSettings(final GradebookUiSettings settings)
	{
		Session.get().setAttribute(GB_UI_SETTINGS_KEY, settings);
	}

	@Override
	public OwlGbUiSettings getOwlUiSettings()
	{
		return getOwlUiSettings(OWL_FG_UI_SETTINGS_KEY);
	}

	@Override
	public void setOwlUiSettings(final OwlGbUiSettings value)
	{
		setOwlUiSettings(OWL_FG_UI_SETTINGS_KEY, value);
	}
	
	@Override
	public List<OwlGbStudentGradeInfo> refreshStudentGradeInfo()
	{
		final GradebookUiSettings settings = getUiSettings();
		final OwlGbUiSettings owlSettings = getOwlUiSettings();
		final List<OwlGbStudentGradeInfo> grades = businessService.owl().fg.buildGradeMatrixForFinalGrades(new UiSettings(settings, owlSettings));
		
		// there is a timestamp put on the table that is used to check for concurrent modifications,
		// update it now that we've refreshed the grade matrix
		// See constructor lines around table.add(new AttributeModifier("data-gradestimestamp", gradesTimestamp.getTime()));
		table.add(AttributeModifier.replace("data-gradestimestamp", new Date().getTime()));
		
		return grades;
	}
	
	public void redrawSpreadsheet(AjaxRequestTarget target)
	{
		if (target != null)
		{
			target.add(spreadsheet);
			
			// any input errors would not have been allowed to save, so we can clear this
			updateLiveGradingMessage(LiveGradingMessage.SAVED);
			
			target.appendJavaScript("reinitSpreadsheet();");
		}
	}
	
	// OWLTODO: probably move all this logic into CourseGradeSubmissionPanel or into CourseGradeSubmitter itself?
	private void updateStatsAndHistoryModel(CourseGradeSubmissionData data)
	{	
		// 1. refresh the provided members
		// 2. get the course grades
		// 3. get the stats, passing in above
		// 4. get the history, passing in above
		// 5. get the submission/approval status, passing in above
		// 6. update the dependant models
		
		// 1 and 2 (get course grades calls refresh)
		CourseGradeSubmitter submitter = getSubmitter();
		Set<OwlGradeSubmissionGrades> courseGrades = submitter.getCurrentCourseGrades();
		
		// 3
		CourseGradeStatistics stats = new CourseGradeStatistics(courseGrades);
		submitter.setStats(stats);
		int missing = submitter.getMissingGradeCount();  // requires a refresh but this was done above
		data.setStats(new SectionStats(stats, missing));
		
		// 4
		List<SubmissionHistoryRow> history = submitter.getSubmissionHistoryRowsForSelectedSection();
		GradeChangeReport report = submitter.getGradeChangeReport(courseGrades);
		String status = history.isEmpty() ? report.getFirstSubmissionMessage() : report.getCurrentStatusMessage();
		data.setHistory(new SubmissionHistory(history, status));
		
		// 5
		// button visible checks
		boolean isSubmitter = submitter.isCurrentUserAbleToSubmit();
		boolean isApprover = submitter.isCurrentUserAbleToApprove();
		// button enabled checks
		boolean canSubmit = isSubmitter && submitter.hasSubmittableGrades(courseGrades, new StringBuilder());
		boolean canApprove = isApprover && submitter.isSectionReadyForApprovalByCurrentUser();
		SubmitAndApproveStatus sas = SubmitAndApproveStatus.builder().canSubmit(isSubmitter).canApprove(isApprover)
				.submitReady(canSubmit).approveReady(canApprove).statusMsg(status).build();
		data.setButtonStatus(sas);
		
		data.setSectionExcluded(submitter.isSectionInExcludePrefixList());
	}
	
	public void submitGrades(AjaxRequestTarget target)
	{
		// OWLTODO: submit() and updateStatsAndHistoryModel() do a lot of the same expensive work, so refactor the common functionality
		// out to improve performance
		getSubmitter().submit();
		redrawForSubmission(target);
	}
	
	public void approveGrades(AjaxRequestTarget target)
	{
		getSubmitter().approve();
		redrawForSubmission(target);
	}
	
	public void redrawForSubmission(AjaxRequestTarget target)
	{
		updateStatsAndHistoryModel(submissionPanel.getData());
		submissionPanel.redrawHistory(target);
		submissionPanel.redrawButtons(target);
		submissionPanel.redrawFeedback(target);
	}
	
	public void redrawForGradeChange(AjaxRequestTarget target)
	{
		updateStatsAndHistoryModel(submissionPanel.getData());
		submissionPanel.redrawStats(target);
		submissionPanel.redrawButtons(target);
	}
	
	public void submitAndApproveError(String msg)
	{
		submissionPanel.error(msg);
	}
	
	public void submitAndApproveMsg(String msg)
	{
		submissionPanel.success(msg);
	}

	public void redrawForGroupChange(AjaxRequestTarget target)
	{
		redrawSpreadsheet(target);
		updateStatsAndHistoryModel(submissionPanel.getData());
		submissionPanel.redrawForSectionChange(target);
	}
	
	public GbModalWindow getGradeOverrideLogWindow()
	{
		return gradeOverrideLogWindow;
	}
	
	public String getCurrentUserUuid()
	{
		return currentUserUuid;
	}
	
	public GbRole getCurrentUserRole()
	{
		return role;
	}
	
	public Gradebook getGradebook()
	{
		return gradebook;
	}

	public void updatePageSize(int pageSize, AjaxRequestTarget target)
	{
		if (pageSize < 0)
		{
			return;
		}
		
		getOwlUiSettings().setPageSize(pageSize);
		table.setItemsPerPage(pageSize);
		table.setCurrentPage(0);
		
		redrawSpreadsheet(target);
	}

	public void resetPaging()
	{
		table.setCurrentPage(0);
	}
	
	public CourseGradeSubmitter getSubmitter()
	{
		if (submitter == null)
		{
			submitter = new CourseGradeSubmitter(businessService, new CourseGradeSubmissionPresenter(this));
		}
		
		return submitter;
	}
}
