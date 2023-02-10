package org.sakaiproject.gradebookng.tool.owl.panels.finalgrades;

import java.io.Serializable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.gradebookng.business.owl.finalgrades.CourseGradeStatistics;
import org.sakaiproject.gradebookng.tool.owl.panels.finalgrades.CourseGradeSubmissionPanel.CourseGradeSubmissionData;


/**
 *
 * @author plukasew
 */
public class SectionStatisticsPanel extends Panel
{
	private Label mean, median, sd, n, mode, skewness, aplus, c, min, excluded, a, d, max, noGradeEntered, b, f;
	private WebMarkupContainer redrawable;
	
	public SectionStatisticsPanel(String id, IModel<CourseGradeSubmissionData> model)
	{
		super(id, model);
	}
	
	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		
		// Create the accordion header
        final Label sectionStatsHeader = new Label( "sectionStatsHeader",
				new StringResourceModel( "finalgrades.sectionStatsHeader", this, null));
		add(sectionStatsHeader);
		
		redrawable = new WebMarkupContainer("redrawable")
		{
			@Override
			protected void onBeforeRender()
			{
				super.onBeforeRender();

				/*CourseGradesPage page = (CourseGradesPage) getPage();
				CourseGradeSubmitter cgs = page.getSubmitter();
				cgs.clearStats();
				CourseGradeStatistics stats = cgs.getStatsForSelectedSection();*/
				SectionStats secStats = ((CourseGradeSubmissionData) SectionStatisticsPanel.this.getDefaultModelObject()).getStats();
				CourseGradeStatistics stats = secStats.getStats();
				int missingGradeCount = secStats.getMissingGradeCount();

				mean.setDefaultModelObject(stats.getMeanForDisplay());
				median.setDefaultModelObject(stats.getMedianForDisplay());
				sd.setDefaultModelObject(stats.getStandardDeviationForDisplay());
				n.setDefaultModelObject(stats.getNumericCount());

				mode.setDefaultModelObject(stats.getModeForDisplay());
				skewness.setDefaultModelObject(stats.getSkewnessForDisplay());
				aplus.setDefaultModelObject(stats.getCountForLetterGrade().get("A+"));
				c.setDefaultModelObject(stats.getCountForLetterGrade().get("C"));

				min.setDefaultModelObject(stats.getMinimumForDisplay());
				excluded.setDefaultModelObject(stats.getNonNumericCount());
				a.setDefaultModelObject(stats.getCountForLetterGrade().get("A"));
				d.setDefaultModelObject(stats.getCountForLetterGrade().get("D"));

				max.setDefaultModelObject(stats.getMaximumForDisplay());
				noGradeEntered.setDefaultModelObject(missingGradeCount);
				b.setDefaultModelObject(stats.getCountForLetterGrade().get("B"));
				f.setDefaultModelObject(stats.getCountForLetterGrade().get("F"));

			}
		};
		redrawable.setOutputMarkupId(true);
		add(redrawable);
		
		redrawable.add(mean = new Label("mean", ""));
		redrawable.add(median = new Label("median", ""));
		redrawable.add(sd = new Label("sd", ""));
		redrawable.add(n = new Label("n", ""));
		
		redrawable.add(mode = new Label("mode", ""));
		redrawable.add(skewness = new Label("skewness", ""));
		redrawable.add(aplus = new Label("aplus", ""));
		redrawable.add(c = new Label("c", ""));
		
		redrawable.add(min = new Label("min", ""));
		redrawable.add(excluded = new Label("excluded", ""));
		redrawable.add(a = new Label("a", ""));
		redrawable.add(d = new Label("d", ""));
		
		redrawable.add(max = new Label("max", ""));
		redrawable.add(noGradeEntered = new Label("noGradeEntered", ""));
		redrawable.add(b = new Label("b", ""));
		redrawable.add(f = new Label("f", ""));
	}
	
	public void redraw(AjaxRequestTarget target)
	{
		if (target != null)
		{
			target.add(redrawable);
		}
	}
	
	@RequiredArgsConstructor
	public static class SectionStats implements Serializable
	{
		@Getter
		private final CourseGradeStatistics stats;
		
		@Getter
		private final int missingGradeCount;
	}

}
