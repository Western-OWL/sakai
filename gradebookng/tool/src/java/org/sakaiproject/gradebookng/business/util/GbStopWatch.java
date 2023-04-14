/**
 * Copyright (c) 2003-2016 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.gradebookng.business.util;

import org.apache.commons.lang3.time.StopWatch;

import lombok.extern.slf4j.Slf4j;

/**
 * Stopwatch extension that times pieces of the logic to determine impact on any modifications.
 */
@Slf4j
public class GbStopWatch extends StopWatch {

	private String context;

	public GbStopWatch() {
		this("");
	}

	public GbStopWatch(String context) {
		this.context = context;
		start();
	}

	public void time(final String msg) {
		if (context.isEmpty()) {
			stop();
			log.debug("Time for [" + msg + "] was: " + getTime() + "ms");
			reset();
			start();
		} else {
			timeWithContext(context, msg);
		}
	}

	public void timeWithContext(final String ctx, final String msg) {
		stop();
		log.debug("Time for [" + ctx + "].[" + msg + "] was: " + getTime() + "ms");
		reset();
		start();
	}
}
