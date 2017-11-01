package org.sakaiproject.gradebookng.tool.panels.importExport;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.lang.Bytes;

import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.exception.GbImportExportInvalidFileTypeException;
import org.sakaiproject.gradebookng.business.model.ImportedSpreadsheetWrapper;
import org.sakaiproject.gradebookng.business.util.ImportGradesHelper;
import org.sakaiproject.gradebookng.tool.component.SakaiAjaxButton;
import org.sakaiproject.gradebookng.tool.model.ImportWizardModel;
import org.sakaiproject.gradebookng.tool.pages.GradebookPage;
import org.sakaiproject.gradebookng.tool.pages.ImportExportPage;

/**
 * Upload/Download page
 */
@Slf4j
public class GradeImportUploadStep extends Panel {
	private final String panelId;

	@SpringBean(name = "org.sakaiproject.gradebookng.business.GradebookNgBusinessService")
	protected GradebookNgBusinessService businessService;

	public GradeImportUploadStep(final String id) {
		super(id);
		panelId = id;
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		add(new ExportPanel("export"));
		add(new UploadForm("form"));
	}

	/*
	 * Upload form
	 */
	private class UploadForm extends Form<Void> {

		FileUploadField fileUploadField;

		public UploadForm(final String id) {
			super(id);

			setMultiPart(true);
			setMaxSize(Bytes.megabytes(2));

			SakaiAjaxButton continueButton = new SakaiAjaxButton("continuebutton") {
				@Override
				public void onSubmit(AjaxRequestTarget target, Form<?> form) {
					processUploadedFile(target);
				}
			};
			continueButton.setOutputMarkupId(true);
			continueButton.setEnabled(false);
			add(continueButton);

			fileUploadField = new FileUploadField("upload");
			fileUploadField.add(new AjaxFormSubmitBehavior("onchange") {
				@Override
				protected void onSubmit(final AjaxRequestTarget target) {
					FileUpload file = fileUploadField.getFileUpload();
					boolean continueEnabled = file != null;
					continueButton.setEnabled(continueEnabled);
					target.add(continueButton);
				}
			});
			add(fileUploadField);

			final SakaiAjaxButton cancel = new SakaiAjaxButton("cancelbutton") {
				@Override
				public void onSubmit(AjaxRequestTarget target, Form<?> form) {
					setResponsePage(GradebookPage.class);
				}
			};
			cancel.setDefaultFormProcessing(false);
			add(cancel);
		}

		public void processUploadedFile(AjaxRequestTarget target) {

			final ImportExportPage page = (ImportExportPage) getPage();
			final FileUpload upload = fileUploadField.getFileUpload();
			if (upload != null) {

				log.debug("file upload success");

				// turn file into list
				// TODO would be nice to capture the values from these exceptions
				ImportedSpreadsheetWrapper spreadsheetWrapper;
				try {
					spreadsheetWrapper = ImportGradesHelper.parseImportedGradeFile(upload.getInputStream(), upload.getContentType(), upload.getClientFileName(), businessService);
				} catch (final GbImportExportInvalidFileTypeException e) {
					error(getString("importExport.error.incorrecttype"));
					page.updateFeedback(target);
					return;
				} catch (final InvalidFormatException e) {
					error(getString("importExport.error.incorrecttype"));
					page.updateFeedback(target);
					return;
				} catch (final IOException e) {
					error(getString("importExport.error.unknown"));
					page.updateFeedback(target);
					return;
				}

				final ImportWizardModel importWizardModel = new ImportWizardModel();
				importWizardModel.setSpreadsheetWrapper(spreadsheetWrapper);
				boolean uploadSuccess = ImportGradesHelper.setupImportWizardModelForSelectionStep(page, GradeImportUploadStep.this, importWizardModel, businessService, target);

				if (!uploadSuccess)
				{
					// For whatever issues were encountered, ImportGradesHelper.setupImportWizardModelForSelectionStep will have updated the feedbackPanels; just return
					return;
				}

				final Component newPanel = new GradeItemImportSelectionStep(GradeImportUploadStep.this.panelId, Model.of(importWizardModel));
				newPanel.setOutputMarkupId(true);

				// AJAX the new panel into place
				WebMarkupContainer container = page.container;
				container.addOrReplace(newPanel);
				target.add(container);
			} else {
				error(getString("importExport.error.noFileSelected"));
				page.updateFeedback(target);
			}
		}
	}
}
