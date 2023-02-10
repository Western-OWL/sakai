package org.sakaiproject.gradebookng.tool.owl.component.table.columns;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.sakaiproject.gradebookng.business.SortDirection;
import org.sakaiproject.gradebookng.tool.component.GbAjaxLink;
import org.sakaiproject.gradebookng.tool.owl.model.UiSettings;
import org.sakaiproject.gradebookng.tool.owl.pages.FinalGradesPage;

/**
 *
 * @author plukasew
 */
public abstract class GbColumnSortToggleLink<T> extends GbAjaxLink<T>
{
	public GbColumnSortToggleLink(String id)
	{
		super(id);
	}
	
	public GbColumnSortToggleLink(String id, IModel<T> model)
	{
		super(id, model);
	}
	
	@Override
	public void onClick(AjaxRequestTarget target)
	{
		final FinalGradesPage page = (FinalGradesPage) getPage();
		final UiSettings settings = page.getGbUiSettings();
		
		toggle(settings);
		
		// save settings
		page.setGbUiSettings(settings);

		// reset back to page one
		page.resetPaging();
				
		// refresh
		page.redrawSpreadsheet(target);
	}
	
	/**
	 * Toggles between ASC/DESC sort directions based on the value returned by getSort().
	 * If you need to do more, override this method
	 * @param settings
	 * @param owlSettings
	 */
	protected void toggle(UiSettings settings)
	{
		SortDirection sort = getSort(settings);
		setSort(settings, sort == null ? SortDirection.getDefault() : sort.toggle());
	}
	
	protected abstract SortDirection getSort(UiSettings settings);
	
	protected abstract void setSort(UiSettings settings, SortDirection value);
}
