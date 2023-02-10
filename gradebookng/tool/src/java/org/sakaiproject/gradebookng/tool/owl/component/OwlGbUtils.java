package org.sakaiproject.gradebookng.tool.owl.component;

import java.util.Optional;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.model.GbUser;
import org.sakaiproject.gradebookng.tool.owl.pages.IGradesPage;

/**
 *
 * @author plukasew
 */
public class OwlGbUtils
{
	/**
	 * Returns the parent cell for given component
	 * @param component the component whose parent we want to find
	 * @param parentId the wicket id of the parent cell component
	 * @return the parent component, or empty if not found
	 */
	public static Optional<Component> getParentCellFor(final Component component, final String parentId)
	{	
		if (component == null)
		{
			return Optional.empty();
		}
		
		if (StringUtils.equals(component.getMarkupAttributes().getString("wicket:id"), parentId))
		{
			return Optional.of(component);
		}
		
		return getParentCellFor(component.getParent(), parentId);
	}

	public static StringResourceModel getModalTitleModel(GradebookNgBusinessService bus, GbUser user, Page page, String normalKey, Object[] normalArgs)
	{
		return getModalTitleModel(bus, user, page, normalKey, normalArgs, ArrayUtils.EMPTY_OBJECT_ARRAY);
	}

	public static StringResourceModel getModalTitleModel(GradebookNgBusinessService bus, GbUser user, Page page, String normalKey, Object[] normalArgs, Object[] anonArgs)
	{
		if (((IGradesPage) page).getOwlUiSettings().isContextAnonymous())
		{
			String anonId = bus.owl().anon.getSectionAnonIdForUser(user.getDisplayId()).map(String::valueOf).orElse("");
			return new StringResourceModel(normalKey + ".anonymous", null, ArrayUtils.addAll(new Object[] { anonId }, anonArgs));
		}

		return new StringResourceModel(normalKey, null, normalArgs);
	}
}
