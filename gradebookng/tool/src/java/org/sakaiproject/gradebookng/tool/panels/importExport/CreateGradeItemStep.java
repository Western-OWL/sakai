package org.sakaiproject.gradebookng.tool.panels.importExport;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.gradebookng.business.model.ProcessedGradeItem;
import org.sakaiproject.gradebookng.tool.model.ImportWizardModel;
import org.sakaiproject.gradebookng.tool.pages.ImportExportPage;
import org.sakaiproject.gradebookng.tool.panels.AddOrEditGradeItemPanelContent;
import org.sakaiproject.service.gradebook.shared.Assignment;

import lombok.extern.slf4j.Slf4j;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.sakaiproject.gradebookng.tool.component.SakaiAjaxButton;

/**
 * Importer has detected that items need to be created so extract the data and wrap the 'AddOrEditGradeItemPanelContent' panel
 */
@Slf4j
public class CreateGradeItemStep extends Panel {

    private final String panelId;
    private final IModel<ImportWizardModel> model;

    PreviewImportedGradesPanel previewGradesPanel;

    public CreateGradeItemStep(final String id, final IModel<ImportWizardModel> importWizardModel) {
        super(id);
        panelId = id;
        model = importWizardModel;
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        //unpack model
        final ImportWizardModel importWizardModel = this.model.getObject();

        final int step = importWizardModel.getStep();

        // original data
        final ProcessedGradeItem processedGradeItem = importWizardModel.getItemsToCreate().get(step - 1);

        // setup new assignment for populating
        final Assignment assignment = new Assignment();
        assignment.setName(StringUtils.trim(processedGradeItem.getItemTitle()));
        if(StringUtils.isNotBlank(processedGradeItem.getItemPointValue())) {
            assignment.setPoints(Double.parseDouble(processedGradeItem.getItemPointValue()));
        }

        final Model<Assignment> assignmentModel = new Model<>(assignment);

        @SuppressWarnings("unchecked")
        final Form<Assignment> form = new Form("form", assignmentModel);
        add(form);

        final SakaiAjaxButton nextButton = new SakaiAjaxButton("nextbutton") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                final Assignment a = (Assignment) form.getDefaultModel().getObject();

                //add to model
                importWizardModel.getAssignmentsToCreate().add(a);

                log.debug("Assignment: {}", assignment);

                // sync up the assignment data so we can present it for confirmation
                processedGradeItem.setAssignmentTitle(a.getName());
                processedGradeItem.setAssignmentPoints(a.getPoints());

                //Figure out if there are more steps
                //If so, go to the next step (ie do it all over again)
                Component newPanel;
                if (step < importWizardModel.getTotalSteps()) {
                    importWizardModel.setStep(step+1);
                    newPanel = new CreateGradeItemStep(CreateGradeItemStep.this.panelId, Model.of(importWizardModel));
                } else {
                    //If not, continue on in the wizard
                    newPanel = new GradeImportConfirmationStep(CreateGradeItemStep.this.panelId, Model.of(importWizardModel));
                }

                // clear any previous errors
                final ImportExportPage page = (ImportExportPage) getPage();
                page.clearFeedback();
                target.add(page.feedbackPanel);

                // AJAX the new panel into place
                newPanel.setOutputMarkupId(true);
                WebMarkupContainer container = page.container;
                container.addOrReplace(newPanel);
                target.add(newPanel);
            }
        };
        form.add(nextButton);

        final SakaiAjaxButton backButton = new SakaiAjaxButton("backbutton") {
            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {

                // clear any previous errors
                final ImportExportPage page = (ImportExportPage) getPage();
                page.clearFeedback();
                target.add(page.feedbackPanel);

                // Create the previous panel
                Component previousPanel;
                if (step > 1) {
                    importWizardModel.setStep(step-1);
                    previousPanel = new CreateGradeItemStep(CreateGradeItemStep.this.panelId, Model.of(importWizardModel));
                }
                else {
                    previousPanel = new GradeItemImportSelectionStep(CreateGradeItemStep.this.panelId, Model.of(importWizardModel));
                }

                // AJAX the previous panel into place
                previousPanel.setOutputMarkupId(true);
                WebMarkupContainer container = page.container;
                container.addOrReplace(previousPanel);
                target.add(container);
            }
        };
        backButton.setDefaultFormProcessing(false);
        form.add(backButton);

        //wrap the form create panel
        form.add(new Label("createItemHeader", new StringResourceModel("importExport.createItem.heading", this, null, step, importWizardModel.getTotalSteps())));
        form.add(new AddOrEditGradeItemPanelContent("subComponents", assignmentModel));

        previewGradesPanel = new PreviewImportedGradesPanel("previewGradesPanel", model);
        form.add(previewGradesPanel);
    }
}
