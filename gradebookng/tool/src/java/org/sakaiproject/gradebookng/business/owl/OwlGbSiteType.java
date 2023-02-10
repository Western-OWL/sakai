package org.sakaiproject.gradebookng.business.owl;

import org.sakaiproject.site.api.Site;

/**
 *
 * @author plukasew
 */
public enum OwlGbSiteType
{
	COURSE, PROJECT;

	public static OwlGbSiteType from(Site site)
	{
		return COURSE.name().equalsIgnoreCase(site.getType()) ? OwlGbSiteType.COURSE : OwlGbSiteType.PROJECT;
	}
}
