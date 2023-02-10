package org.sakaiproject.gradebookng.tool.owl.panels.finalgrades;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.sakaiproject.gradebookng.business.owl.finalgrades.CourseGradePdfGenerator;
import org.sakaiproject.gradebookng.business.owl.finalgrades.CourseGradeSubmitter;
import org.sakaiproject.gradebookng.tool.owl.pages.FinalGradesPage;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeSubmission;

/**
 *
 * @author plukasew
 */
public class DownloadLinkPanel extends Panel
{
	private static final Log LOG = LogFactory.getLog(DownloadLinkPanel.class);
	
	public DownloadLinkPanel(String id, IModel<OwlGradeSubmission> subModel)
	{
		super(id, subModel);
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		
		OwlGradeSubmission sub = (OwlGradeSubmission) getDefaultModelObject();
		FinalGradesPage page = (FinalGradesPage) getPage();
		CourseGradeSubmitter submitter = page.getSubmitter();
		DownloadLink dl = new DownloadLink("downloadLink", new PdfReportModel(sub),
						new PdfFileNameModel(submitter.getSelectedSection().getTitle(), sub.getSectionEid(), sub.getSubmissionDate()));
		dl.setDeleteAfterDownload(true);

		dl.setEnabled(submitter.isCurrentUserAbleToSubmit() || submitter.isCurrentUserAbleToApprove());
		add(dl);
	}
	
	class PdfReportModel extends LoadableDetachableModel<File>
	{
		// OWLTODO: store just the id and retrieve from businessService?
		private final OwlGradeSubmission sub;
		
		public PdfReportModel(OwlGradeSubmission sub)
		{
			this.sub = sub;
		}
		
		@Override
		protected File load()
		{
			FinalGradesPage page = (FinalGradesPage) DownloadLinkPanel.this.getPage();
			try
			{
				// OWLTODO: this permission check is probably redundant but shouldn't hurt performance too much...
				CourseGradeSubmitter submitter = page.getSubmitter();
				if (submitter.isCurrentUserAbleToSubmit() || submitter.isCurrentUserAbleToApprove())
				{
					CourseGradePdfGenerator pdfGen = new CourseGradePdfGenerator(sub);
					CourseGradeSubmitter.findPreviousApproval(sub.getPrevSubmission()).ifPresent(approval -> pdfGen.setPreviousApprovedGrades(approval.getGradeData()));
					File tempFile = File.createTempFile("pdf", null);
					FileOutputStream fos = new FileOutputStream(tempFile);
					pdfGen.generateIntoOutputStream(fos);
					fos.flush();
					fos.close();
					return tempFile;
				}
				
				Session.get().error("You don't have permission to view this report");
				return null;
			}
			catch (IOException e)
			{
				LOG.error("Could not generate PDF for submission id = " + sub.getId(), e);
				Session.get().error("Unable to generate PDF file. Please try again later. If the problem persists, contact "
						+ CourseGradeSubmitter.SUPPORT_EMAIL);
				return null;
			}
			
		}
	}
	
	class PdfFileNameModel extends LoadableDetachableModel<String>
	{
		private String filename;
		
		public PdfFileNameModel(String sectionName, String sectionEid, Date submissionDate)
		{
			DateFormat formatter = new SimpleDateFormat("MM_dd_yyyy-HH_mm_ss");

			filename = sectionName + "-" + sectionEid + "-" + formatter.format(submissionDate) + ".pdf";
			filename = filename.replaceAll("/", "-");
			//in regex, slashes are escaped again, so this is actually one slash
			String slash = "\\\\";
			filename = filename.replaceAll(slash, "-");
			filename = filename.replaceAll("\\s+", "_");
		}
		
		@Override
		protected String load()
		{
			return filename;    
		}
	}
}
