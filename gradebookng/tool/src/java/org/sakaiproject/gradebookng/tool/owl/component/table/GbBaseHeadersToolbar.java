package org.sakaiproject.gradebookng.tool.owl.component.table;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;

/**
 *
 * @author plukasew
 */
public class GbBaseHeadersToolbar<S> extends HeadersToolbar<S>
{
	public <T> GbBaseHeadersToolbar(final DataTable<T, S> table, final ISortStateLocator stateLocator)
	{
		super(table, stateLocator);
	}
}

