package org.sakaiproject.gradebookng.business.owl;

import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.model.GbUser;
import org.sakaiproject.gradebookng.business.owl.anon.OwlAnonTypes;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.user.api.User;

/**
 * Wraps a GbUser and adds support for anonymous grading
 * @author plukasew
 */
public class OwlGbUser implements Serializable, Comparable<OwlGbUser>
{
	public final GbUser gbUser;

	public final int anonId;

	private OwlGbUser()
	{
		this(new GbUser("", "", "", "", "", "", ""), OwlAnonTypes.NO_ID);
	}

	private OwlGbUser(GbUser gbUser, int anonId)
	{
		this.gbUser = gbUser;
		this.anonId = anonId;
	}

	public static OwlGbUser fromUser(final User u)
	{
		return fromUserWithStudentNumber(u, "");
	}

	public static OwlGbUser of(final GbUser gbUser, final int anonId)
	{
		return new OwlGbUser(gbUser, anonId);
	}

	public static OwlGbUser fromUserAcquiringStudentNumber(final User u, GradebookNgBusinessService businessService)
	{
		String num = businessService.getStudentNumber(u, businessService.getCurrentSite().orElse(null));
		return fromUserWithStudentNumber(u, num);
	}

	public static OwlGbUser fromUserAcquiringRevealedStudentNumber(final User u, final Site site, GradebookNgBusinessService bus)
	{
		String num = bus.owl().getRevealedStudentNumber(u, site);
		return fromUserWithStudentNumber(u, num);
	}

	public static OwlGbUser fromUserAcquiringStudentNumberAndAnonIdMap(final User u, GradebookNgBusinessService bus)
	{
		String num = bus.getStudentNumber(u, bus.getCurrentSite().orElse(null));
		int anonId = bus.owl().anon.getSectionAnonIdForUser(u.getEid()).orElse(OwlAnonTypes.NO_ID);
		return fromUserWithStudentNumberAndAnonId(u, num, anonId);
	}

	public static OwlGbUser fromUserWithStudentNumber(final User u, final String studentNumber)
	{
		GbUser user = new GbUser(u, studentNumber);
		return new OwlGbUser(user, OwlAnonTypes.NO_ID);
	}

	public static OwlGbUser fromUserWithStudentNumberAndAnonId(final User u, final String studentNumber, final int anonId)
	{
		GbUser user = new GbUser(u, studentNumber);
		return new OwlGbUser(user, anonId);
	}

	public boolean isValid()
	{
		return StringUtils.isNotBlank(gbUser.getUserUuid());
	}

	@Override
	public int compareTo(OwlGbUser other)
	{
		return gbUser.compareTo(other.gbUser);
	}
}
