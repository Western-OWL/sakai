package org.sakaiproject.gradebookng.tool.actions.owl;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.Serializable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.sakaiproject.gradebookng.tool.actions.Action;
import org.sakaiproject.gradebookng.tool.actions.ActionResponse;

/**
 * A no-op action used for anon grading support.
 *
 * @author plukasew
 */
public class OwlEmptyAction implements Action, Serializable
{

	@Override
	public ActionResponse handleEvent(JsonNode params, AjaxRequestTarget target)
	{
		return new OwlEmptyOkResponse();
	}

	// the regular EmptyOkReponse is package-private so we need this clone
	private static class OwlEmptyOkResponse implements ActionResponse
	{
		@Override
		public String getStatus()
		{
			return "OK";
		}

		@Override
		public String toJson()
		{
			return "{}";
		}
	}

}
