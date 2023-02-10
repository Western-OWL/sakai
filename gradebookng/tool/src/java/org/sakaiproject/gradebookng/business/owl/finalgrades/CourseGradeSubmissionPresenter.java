package org.sakaiproject.gradebookng.business.owl.finalgrades;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.gradebookng.business.model.GbGroup;
import org.sakaiproject.gradebookng.tool.owl.pages.FinalGradesPage;

/**
 * Interfaces CourseGradeSubmitter with presentation framework for better isolation
 * @author plukasew
 */
public class CourseGradeSubmissionPresenter
{
	private final FinalGradesPage page;
	
	public CourseGradeSubmissionPresenter(final FinalGradesPage page)
	{
		this.page = page;
	}
	
	public String getUserIp()
	{
		String userIp = ((HttpServletRequest) page.getRequest().getContainerRequest()).getRemoteAddr();
		return StringUtils.trimToEmpty(userIp);
	}
	
	public void presentError(String msg)
	{
        page.submitAndApproveError(msg);
	}
	
	public void presentMsg(String msg)
	{
		page.submitAndApproveMsg(msg);
		
	}
	
	public GbGroup getSelectedSection()
	{
		GbGroup filter = page.getUiSettings().getGroupFilter();
		if (filter == null)
		{
			filter = page.getSections().get(0);
		}
		
		return filter;
	}
	
	public String getLocalizedString(String key)
	{
		return page.getString(key);
	}
	
	public String getLocalizedString(String key, String[] params)
	{
		return new StringResourceModel(key, page, null, (Object[]) params).getString();
	}
}
