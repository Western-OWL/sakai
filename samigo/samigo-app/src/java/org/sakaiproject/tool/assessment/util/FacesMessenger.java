package org.sakaiproject.tool.assessment.util;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

/**
 *
 * @author plukasew
 */
public interface FacesMessenger
{
	default void error(FacesContext context, String msg)
	{
		context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
	}

	default void warn(FacesContext context, String msg)
	{
		context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, msg, null));
	}

	default void info(FacesContext context, String msg)
	{
		context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
	}
}
