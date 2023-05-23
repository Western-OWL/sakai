package org.sakaiproject.webapi;

/**
 * Thrown when a disabled enpoint is accessed
 * @author bbailla2
 */
public class EndpointDisabledException extends SecurityException {

	public EndpointDisabledException(String message) {
		super(message);
	}

}
