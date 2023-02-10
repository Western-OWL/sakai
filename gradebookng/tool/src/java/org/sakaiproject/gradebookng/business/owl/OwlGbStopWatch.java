package org.sakaiproject.gradebookng.business.owl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

/**
 *
 * @author plukasew
 */
@Slf4j
public class OwlGbStopWatch extends StopWatch
{
	private final String context;

	public OwlGbStopWatch(String context)
	{
		this.context = context;
		start();
	}

	public void time(final String msg)
	{
		stop();
		log.debug("Time for [{}].[{}] was: {} ms", context, msg, getTime());
		reset();
		start();
	}
}
