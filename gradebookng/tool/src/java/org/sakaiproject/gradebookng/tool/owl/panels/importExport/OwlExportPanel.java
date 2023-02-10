package org.sakaiproject.gradebookng.tool.owl.panels.importExport;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.time.Duration;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.gradebookng.business.GbCategoryType;
import org.sakaiproject.gradebookng.business.model.GbGroup;
import org.sakaiproject.gradebookng.business.owl.OwlBusinessService;
import org.sakaiproject.gradebookng.tool.owl.component.dropdown.GbGroupChoiceRenderer;
import org.sakaiproject.gradebookng.tool.owl.model.OwlExportConfig;
import org.sakaiproject.gradebookng.tool.owl.model.OwlExportFileBuilder;
import org.sakaiproject.gradebookng.tool.owl.model.OwlExportFileBuilder.OwlExportFormat;
import org.sakaiproject.gradebookng.tool.pages.BasePage;
import org.sakaiproject.gradebookng.tool.panels.BasePanel;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.CategoryDefinition;
import org.sakaiproject.service.gradebook.shared.SortType;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.sakaiproject.util.Validator;
import org.sakaiproject.util.api.FormattedText;



/**
 * An anon-aware export panel customized for OWL.
 *
 * NOTE: This class is designed as a complete replacement for Sakai's ExportPanel.
 * As such, when porting this code, carefully compare it to the current version of ExportPanel.java/.html and pick up relevant
 * bug fixes and/or improvements.
 * @author plukasew
 */
public class OwlExportPanel extends BasePanel
{
	private OwlExportConfig config;
	private OwlExportFileBuilder fileBuilder;

	private List<Assignment> allAssignments;
	private SortType sortBy;
	private List<CategoryDefinition> cats;
	private GbCategoryType catType;

	// Model for file names; gets updated by buildFileName(), which is invoked by buildFile for effiency with determining csv vs zip wrt anonymity
	private final Model<String> fileNameModel = new Model<>("");

	private boolean stuNumVisible = false; // current user can see student numbers

	public OwlExportPanel(String id)
	{
		super(id);
	}

	@Override
	public void onInitialize()
	{
		super.onInitialize();

		sortBy = SortType.SORT_BY_SORTING;
		final String userGbUiCatPref = businessService.getUserGbPreference("GROUP_BY_CAT");
		if (businessService.categoriesAreEnabled() && (StringUtils.isBlank(userGbUiCatPref) || BooleanUtils.toBoolean(userGbUiCatPref)))
		{
			sortBy = SortType.SORT_BY_CATEGORY;
		}
		allAssignments = businessService.getGradebookAssignments(sortBy);
		cats = businessService.getGradebookCategories();
		catType = businessService.getGradebookCategoryType();

		OwlBusinessService owlbus = businessService.owl();

		fileBuilder = new OwlExportFileBuilder(ComponentManager.get(FormattedText.class).getDecimalSeparator());

		stuNumVisible = businessService.isStudentNumberVisible();
		config = new OwlExportConfig(stuNumVisible);
		
		add(new AjaxToggleCheckBox("includeStudentName", new PropertyModel<>(config, "includeStudentName")));
		add(new AjaxToggleCheckBox("includeStudentId", new PropertyModel<>(config, "includeStudentId")));
		add(new AjaxToggleCheckBox("includeStudentNumber", new PropertyModel<>(config, "includeStudentNumber")).setVisible(stuNumVisible));
		add(new AjaxToggleCheckBox("includeGradeItemScores", new PropertyModel<>(config, "includeGradeItemScores")));
		add(new AjaxToggleCheckBox("includeGradeItemComments", new PropertyModel<>(config, "includeGradeItemComments")));
		add(new AjaxToggleCheckBox("includeFinalGrade", new PropertyModel<>(config, "includeFinalGrade")));
		final boolean catsWeighted = businessService.getGradebookCategoryType() == GbCategoryType.WEIGHTED_CATEGORY;
		add(new AjaxToggleCheckBox("includePoints", new PropertyModel<>(config, "includePoints")).setVisible(!catsWeighted));
		add(new AjaxToggleCheckBox("includeLastLogDate", new PropertyModel<>(config, "includeLastLogDate")));
		final boolean avgsVisible = businessService.categoriesAreEnabled();
		add(new AjaxToggleCheckBox("includeCategoryAverages", new PropertyModel<>(config, "includeCategoryAverages")).setVisible(avgsVisible));
		add(new AjaxToggleCheckBox("includeCalculatedGrade", new PropertyModel<>(config, "includeCalculatedGrade")));
		add(new AjaxToggleCheckBox("includeGradeOverride", new PropertyModel<>(config, "includeGradeOverride")));

		config.group = getAllGroup();
		final List<GbGroup> groups = businessService.getSiteSectionsAndGroups();
		groups.add(0, config.group);
		DropDownChoice<GbGroup> groupChoice = new DropDownChoice<>("groupFilter", Model.of(config.group), groups, new GbGroupChoiceRenderer());
		groupChoice.add(new AjaxFormComponentUpdatingBehavior("onchange")
		{
			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				OwlExportPanel.this.config.group = (GbGroup) getFormComponent().getDefaultModelObject();
			}
		});
		groupChoice.setNullValid(false);
		// Determine visibility of group filter based on if Gradebook has any anonymous gradebook items;
		boolean siteHasAnonItems = allAssignments.stream().anyMatch(Assignment::isAnon);
		groupChoice.setVisible(!siteHasAnonItems && groups.size() > 1);
		add(groupChoice);

		// are we showing revealed exports?
		boolean showRevealed = siteHasAnonItems && owlbus.fg.areAllSectionsApproved(owlbus.getViewableSectionEids());

		add(new GbDownloadLink("downloadFullGradebook", fileNameModel)
		{
			@Override
			public File load()
			{
				return fileBuilder.buildFile(OwlExportPanel.this, new OwlExportConfig(stuNumVisible), allAssignments, cats);
			}
		});
		add(new GbDownloadLink("revealedExport", fileNameModel)
		{
			@Override
			public File load()
			{
				return fileBuilder.buildRevealedFile(OwlExportPanel.this, new OwlExportConfig(stuNumVisible), allAssignments, cats);
			}
		}.setVisible(showRevealed));
		add(new GbDownloadLink("downloadCustomGradebook", fileNameModel)
		{
			@Override
			public File load()
			{
				return fileBuilder.buildCustomFile(OwlExportPanel.this, config, allAssignments, cats);
			}
		});
		add(new GbDownloadLink("customRevealedExport", fileNameModel)
		{
			@Override
			public File load()
			{
				return fileBuilder.buildCustomRevealedFile(OwlExportPanel.this, config, allAssignments, cats);
			}
		}.setVisible(showRevealed));
	}

	public void buildFileName(OwlExportFormat exportFormat, boolean custom)
	{
		String prefix = getString("importExport.download.filenameprefix");
		String extension = exportFormat.toString().toLowerCase();
		String gradebookName = StringUtils.trimToEmpty(businessService.getGradebook().getName()).replace("\\s", "_");

		List<String> components = new ArrayList<>(3);
		components.add(prefix);

		if (!gradebookName.isEmpty())
		{
			components.add(gradebookName);
		}

		if (custom)
		{
			String groupTitle =  config.group.getId() == null ? getString("importExport.download.filenameallsuffix") : config.group.getTitle();
			if (StringUtils.isNotBlank(groupTitle))
			{
				components.add(groupTitle);
			}
		}

		String cleanFileName = Validator.cleanFilename(String.join("-", components));

		fileNameModel.setObject(String.format("%s.%s", cleanFileName, extension));
	}

	public OwlBusinessService getOwlbus()
	{
		return businessService.owl();
	}

	@Override
	public Gradebook getGradebook()
	{
		return super.getGradebook();
	}

	private GbGroup getAllGroup()
	{
		return ((BasePage) getPage()).getAllGroup();
	}

	public String formatDateTime(Date date)
	{
		return businessService.formatDateTime(date);
	}

	public SortType getSortType()
	{
		return sortBy;
	}

	public GbCategoryType getCatType()
	{
		return catType;
	}

	public class AjaxToggleCheckBox extends AjaxCheckBox
	{
		public AjaxToggleCheckBox(String id, IModel<Boolean> model)
		{
			super(id, model);
		}

		@Override
		protected void onUpdate(AjaxRequestTarget target)
		{
			// nothing to do, the models will automatically update, which is all we need
		}
	}

	public abstract class GbDownloadLink extends DownloadLink
	{
		public GbDownloadLink(String id, IModel<String> fileNameModel)
		{
			super(id, new Model<>(), fileNameModel);
		}

		@Override
		public void onInitialize()
		{
			super.onInitialize();

			setModel(new LoadableDetachableModel<File>()
			{
				@Override
				protected File load()
				{
					return GbDownloadLink.this.load();
				}

			});
			setCacheDuration(Duration.NONE);
			setDeleteAfterDownload(true);
		}

		public abstract File load();
	}
}
