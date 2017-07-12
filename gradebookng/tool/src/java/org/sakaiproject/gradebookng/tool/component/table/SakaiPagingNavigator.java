/**
 * $URL$
 * $Id$
 *
 * Copyright (c) 2006-2009 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.gradebookng.tool.component.table;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigationIncrementLink;

import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigationLink;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.navigation.paging.IPagingLabelProvider;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigation;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.gradebookng.tool.component.SakaiSpinnerAjaxCallListener;
import org.sakaiproject.gradebookng.tool.component.dropdown.SakaiSpinnerDropDownChoice;
import org.sakaiproject.gradebookng.tool.component.dropdown.SakaiSpinningSelectOnChangeBehavior;
import org.sakaiproject.gradebookng.tool.component.dropdown.SakaiStringResourceChoiceRenderer;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;
import org.sakaiproject.gradebookng.tool.pages.IGradesPage;

public class SakaiPagingNavigator extends AjaxPagingNavigator {

	private static final long serialVersionUID = 1L;
	
	// OWLTODO: maybe this is better represented as an enum with helper methods to keep all the special cases in one place
	public static final String PAGE_SIZE_ALL_STR = "ALL";
	public static final int PAGE_SIZE_ALL = Integer.MAX_VALUE;
	public static final List<String> STANDARD_PAGE_SIZES = Arrays.asList( new String[] { "5", "10", "20", "50", "100", "200", "500", "1000", PAGE_SIZE_ALL_STR} );

	/**
	 * Constructor.
	 * 
	 * @param id
	 *            See Component
	 * @param pageable
	 *            The pageable component the page links are referring to.
	 */
	public SakaiPagingNavigator(final String id, final IPageable pageable)
	{
		this(id, pageable, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *            See Component
	 * @param pageable
	 *            The pageable component the page links are referring to.
	 * @param labelProvider
	 *            The label provider for the link text.
	 */
	public SakaiPagingNavigator(final String id, final IPageable pageable,
		final IPagingLabelProvider labelProvider)
	{
		super(id, pageable, labelProvider);

	}
	
	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		
		setDefaultModel(new CompoundPropertyModel(this));

		// Get the row number selector
		add(newRowNumberSelector(getPageable()));

		// Add additional page links
		replace(newPagingNavigationLink("first", getPageable(), 0));
		replace(newPagingNavigationIncrementLink("prev", getPageable(), -1));
		replace(newPagingNavigationIncrementLink("next", getPageable(), 1));
		replace(newPagingNavigationLink("last", getPageable(), -1));
	}

	/**
	 * Create a new increment link. May be subclassed to make use of specialized links, e.g. Ajaxian
	 * links.
	 * 
	 * @param id
	 *            the link id
	 * @param pageable
	 *            the pageable to control
	 * @param increment
	 *            the increment
	 * @return the increment link
	 */
	@Override
	protected Link newPagingNavigationIncrementLink(String id, IPageable pageable, int increment)
	{
		return new AjaxPagingNavigationIncrementLink(id, pageable, increment)
		{
			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
			{
				super.updateAjaxAttributes(attributes);
				attributes.setChannel(new AjaxChannel("blocking", AjaxChannel.Type.ACTIVE));
		
				AjaxCallListener listener = new SakaiSpinnerAjaxCallListener(getMarkupId(), true);
				attributes.getAjaxCallListeners().add(listener);
			}
		};
	}

	/**
	 * Create a new pagenumber link. May be subclassed to make use of specialized links, e.g.
	 * Ajaxian links.
	 * 
	 * @param id
	 *            the link id
	 * @param pageable
	 *            the pageable to control
	 * @param pageNumber
	 *            the page to jump to
	 * @return the pagenumber link
	 */
	protected Link newPagingNavigationLink(String id, IPageable pageable, int pageNumber)
	{
		return new AjaxPagingNavigationLink(id, pageable, pageNumber)
		{
			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
			{
				super.updateAjaxAttributes(attributes);
				attributes.setChannel(new AjaxChannel("blocking", AjaxChannel.Type.ACTIVE));
		
				AjaxCallListener listener = new SakaiSpinnerAjaxCallListener(getMarkupId(), true);
				attributes.getAjaxCallListeners().add(listener);
			}
		};
	}
	
	protected SakaiSpinnerDropDownChoice<String> newRowNumberSelector(final IPageable pageable)
	{
		IModel<String> choiceModel = new PropertyModel<>(this, "rowNumberSelector");
		IModel<String> labelModel = new StringResourceModel("pager_select_label", this, null);
		SakaiSpinnerDropDownChoice<String> ddc = new SakaiSpinnerDropDownChoice<>("rowNumberSelector", choiceModel, STANDARD_PAGE_SIZES,
			new SakaiStringResourceChoiceRenderer("pager_textPageSize", this)
			{
				@Override
				public Object getDisplayValue(String object)
				{
					if (PAGE_SIZE_ALL_STR.equals(object))
					{
						return new StringResourceModel("pager_textPageSizeAll", SakaiPagingNavigator.this, null).getString();
					}
					
					return super.getDisplayValue(object);
				}
			},
			labelModel,
			new SakaiSpinningSelectOnChangeBehavior()
			{
				@Override
				protected void onUpdate(AjaxRequestTarget target)
				{
					String pageSizeStr = getFormComponent().getDefaultModelObjectAsString();
					int pageSize = PAGE_SIZE_ALL_STR.equals(pageSizeStr) ?
							PAGE_SIZE_ALL : NumberUtils.toInt(pageSizeStr, GradebookUiSettings.DEFAULT_GRADES_PAGE_SIZE);

					final IGradesPage page = (IGradesPage) getPage();

					page.updatePageSize(pageSize, target);
				}
			});
		
		return ddc;
	}
	
	public String getRowNumberSelector()
	{
		long items = ((DataTable) getPageable()).getItemsPerPage();
		return items == PAGE_SIZE_ALL ? PAGE_SIZE_ALL_STR : String.valueOf(items);
	}
	public void setRowNumberSelector(String value)
	{
		try
		{
			int val = PAGE_SIZE_ALL_STR.equals(value) ? PAGE_SIZE_ALL : Integer.parseInt(value);
			((DataTable) getPageable()).setItemsPerPage(val);
		}
		catch (NumberFormatException e)
		{
			// do nothing, ignore invalid input
		}
	}

	/**
	 * Create a new PagingNavigation. May be subclassed to make us of specialized PagingNavigation.
	 * 
	 * @param pageable
	 *            the pageable component
	 * @param labelProvider
	 *            The label provider for the link text.
	 * @return the navigation object
	 */
	@Override
	protected PagingNavigation newNavigation(final String id, final IPageable pageable,
		final IPagingLabelProvider labelProvider)
	{
		return new PagingNavigation("navigation", pageable, labelProvider)
		{
			@Override
			public boolean isVisible()
			{
				// hide the numbered navigation bar e.g. 1 | 2 | 3 etc.
				return false;
			}
		};
	}
	
	
}
