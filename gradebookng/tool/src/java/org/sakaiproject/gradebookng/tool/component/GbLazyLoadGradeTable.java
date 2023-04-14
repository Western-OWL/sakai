package org.sakaiproject.gradebookng.tool.component;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.gradebookng.tool.model.GbGradeTableData;
import org.sakaiproject.portal.util.PortalUtils;

/**
 *
 * @author plukasew
 */
public class GbLazyLoadGradeTable extends AjaxLazyLoadPanel
{
	@SpringBean(name = "org.sakaiproject.component.api.ServerConfigurationService")
	protected ServerConfigurationService serverConfigService;

	public GbLazyLoadGradeTable(String id, IModel<GbGradeTableData> model)
	{
		super(id, model);
	}

	@Override
	public Component getLazyLoadComponent(String markupId)
	{
		GbGradeTable gradeTable = new GbGradeTable(markupId, (IModel<GbGradeTableData>) getDefaultModel());
		gradeTable.setRenderBodyOnly(true);

		return gradeTable;
	}

	@Override
	public void renderHead(final IHeaderResponse response)
	{
		final String version = PortalUtils.getCDNQuery();

		response.render(JavaScriptHeaderItem.forUrl("/library/js/view-preferences.js"));
		response.render(JavaScriptHeaderItem.forUrl("/library/js/sakai-reminder.js"));

		response.render(JavaScriptHeaderItem.forUrl(String.format("/gradebookng-tool/scripts/gradebook-gbgrade-table.js%s", version)));

		response.render(JavaScriptHeaderItem.forUrl(String.format("/gradebookng-tool/webjars/handsontable/6.2.2/handsontable.full.min.js%s", version)));
		response.render(CssHeaderItem.forUrl(String.format("/gradebookng-tool/webjars/handsontable/6.2.2/handsontable.full.min.css%s", version)));

		int sectionsColumnWidth = serverConfigService.getInt("gradebookng.sectionsColumnWidth", 140);
		int studentNumberColumnWidth = serverConfigService.getInt("gradebookng.studentNumberColumnWidth", 140);
		boolean allowColumnResizing = serverConfigService.getBoolean("gradebookng.allowColumnResizing", false);
		StringBuilder sb = new StringBuilder();
		sb.append("var sectionsColumnWidth = ").append(sectionsColumnWidth);
		sb.append(", allowColumnResizing = ").append(allowColumnResizing);
		sb.append(", studentNumberColumnWidth = ").append(studentNumberColumnWidth).append(";");
		response.render(JavaScriptHeaderItem.forScript(sb.toString(), null));
	}

	@Override
	protected void onComponentLoaded(Component component, AjaxRequestTarget target)
	{
		GbGradeTable gt = (GbGradeTable) component;
		target.appendJavaScript(gt.getLoadTableDataJs());
	}

	@Override
	public Component getLoadingComponent(String markupId)
	{
		return new Label(markupId, "<div class=\"gb-lazyLoadGradeTable\"><div class=\"spinButton\"></div></div>").setEscapeModelStrings(false);
	}
}
