// 2012.05.31, plukasew, New
// Record of a course grade approval
// 2012.07.07, plukasew, Modified
// Removed all validation for methods that may be called by Hibernate

package org.sakaiproject.service.gradebook.shared.owl.finalgrades;

import java.io.Serializable;
import java.util.Date;

/**
 * Record of a course grade approval.
 * Backed by database table
 * @author plukasew
 */
public class OwlGradeApproval implements Serializable
{
	private Long id; // primary key in database
	private Date approvalDate;
	private String userEid; // external id (username) of the approver
	private String userIp; // IP address of the approver
	private Boolean uploadedToRegistrar; // true if SFTP of approved grades was successful

	/**
	 * Default constructor. Sets up a "null" object with numeric values set to 0,
	 * String values set to empty, booleans to false, and dates to Unix epoch
	 */
	public OwlGradeApproval()
	{
		id = 0L;
		approvalDate = new Date();
		userEid = "";
		userIp = "";
		uploadedToRegistrar = Boolean.FALSE;
	}

	/**
	 * Returns a unique identifier (database primary key) for this approval
	 * @return unique id for this submission, or 0 if not set. Will not return a null reference.
	 */
	public Long getId()
	{
		return id;
	}

	/**
	 * Sets the unique id (database primary key) for this approval.
	 * @param value
	 */
	private void setId(Long value)
	{
		id = value;
	}

	/**
	 * Returns the date of approval
	 * @return the date of approval, or the Unix epoch if not set. Will not return a null reference.
	 */
	public Date getApprovalDate()
	{
		return approvalDate;
	}

	/**
	 * Sets the date of approval
	 * @param value the date of approval
	 */
	public void setApprovalDate(Date value)
	{
		approvalDate = value;
	}

	/**
	 * Returns the external id (username) of the approver
	 * @return id (username) of the approver
	 */
	public String getUserEid()
	{
		return userEid;
	}

	/**
	 * Sets the external id (username) of the approver
	 * @param value id (username) of approver
	 */
	public void setUserEid(String value)
	{
		userEid = value;
	}

	/**
	 * Return the IP address of the approver
	 * @return IP address of the approver, or empty string if not set. Will not return a null reference.
	 */
	public String getUserIp()
	{
		return userIp;
	}

	/**
	 * Sets the IP address of the approver
	 * @param value ip address of approver
	 */
	public void setUserIp(String value)
	{
		userIp = value;
	}

	/**
	 * Returns whether or not the approved grades were successfully uploaded
	 * to the Office of the Registrar
	 * @return true if the approved grades were successfully uploaded
	 */
	public Boolean getUploadedToRegistrar()
	{
		return uploadedToRegistrar;
	}

	/**
	 * Sets whether or not the approved grades were successfully uploaded
	 * to the Office of the Registrar
	 * @param value true if the approved grades were successfully uploaded
	 */
	public void setUploadedToRegistrar(Boolean value)
	{
		uploadedToRegistrar = value;
	}

} // end class
