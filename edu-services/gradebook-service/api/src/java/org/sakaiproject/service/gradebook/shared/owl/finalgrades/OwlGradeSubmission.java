// 2012.05.29, plukasew, New
// Record of a course grade submission
// 2012.07.07, plukasew, Modified
// Removed all validation for methods that may be called by Hibernate

package org.sakaiproject.service.gradebook.shared.owl.finalgrades;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * Record of a course grade submission or approval.
 * Backed by database table.
 * @author plukasew
 */
public class OwlGradeSubmission implements Serializable
{
	// Status management constants
	public static final int UNDEFINED_STATUS = 0;
	public static final int DISCARDED_STATUS = 1;
	public static final int PENDING_APPROVAL_STATUS = 2;
	public static final int APPROVED_STATUS = 3;
	public static final String[] STATUS_NAMES = {"Undefined", "Discarded", "Pending Approval", "Approved"};

	public static final long NO_PREVIOUS_SUBMISSION = 0L;
	public static final long NO_APPROVAL = 0L;

	public static final String INITIAL_TYPE = "Initial";
	public static final String REVISION_TYPE = "Revision";

	private Long id; // primary key in database
	private String siteId; // site this submission was made from
	private String sectionEid; // section this submission is for
	private Date submissionDate;
	private String userEid; //submitter external id (username)
	private String userIp; // IP address of submitter
	private Integer status;
	private OwlGradeSubmission prevSubmission; //previous submission, if this is a revision
	private OwlGradeApproval approval;
	private Set<OwlGradeSubmissionGrades> gradeData;

	/**
	 * Default constructor. Sets up a "null" object with numeric values set to 0,
	 * string values set to empty string, and dates set to Unix epoch.
	 */
	public OwlGradeSubmission()
	{
		id = 0L;
		siteId = "";
		sectionEid = "";
		submissionDate = new Date(0L);
		userEid = "";
		userIp = "";
		status = UNDEFINED_STATUS;
		prevSubmission = null;
		approval = null;
		gradeData = null;
	}

	/**
	 * Returns a unique identifier (database primary key) for this submission
	 * @return unique id for this submission
	 */
	public Long getId()
	{
		return id;
	}

	/**
	 * Sets the unique id (database primary key) for this submission.
	 * Private because the database should manage the ids.
	 * @param value
	 */
	private void setId(Long value)
	{
		id = value;
	}

	/**
	 * Returns id of the Sakai site this submission was made from
	 * @return id of site
	 */
	public String getSiteId()
	{
		return siteId;
	}

	/**	
	 * Sets the Sakai site id this submission was made from.
	 * @param value the site id
	 */
	public void setSiteId(String value)
	{
		siteId = value;
	}

	/**
	 * Returns the external id of the course section
	 * @return id of the course section
	 */
	public String getSectionEid()
	{
		return sectionEid;
	}

	/**
	 * Sets the external id of the course section
	 * @param value id of the course section
	 */
	public void setSectionEid(String value)
	{
		sectionEid = value;
	}

	/**
	 * Returns date of submission
	 * @return date of submission
	 */
	public Date getSubmissionDate()
	{
		return submissionDate;
	}

	/**
	 * Sets the date of this submission
	 * @param value the submission date
	 */
	public void setSubmissionDate(Date value)
	{
		submissionDate = value;
	}

	/**
	 * Returns the external id (username) of the submitter 
	 * @return id (username) of the submitter
	 */
	public String getUserEid()
	{
		return userEid;
	}

	/**
	 * Sets the external id (username) of the submitter
	 * @param value id (username) of the submitter.
	 */
	public void setUserEid(String value)
	{
		userEid = value;
	}

	/**
	 * Returns the IP address of the submitter
	 * @return IP address of the submitter
	 */
	public String getUserIp()
	{
		return userIp;
	}

	/**
	 * Sets the IP address of the submitter
	 * @param value IP address of submitter
	 */
	public void setUserIp(String value)
	{
		userIp = value;
	}

	/**
	 * Returns the current status of this submission. See the status
	 * code constants defined in this class for valid status option.
	 * @return current status of submission
	 */
	public Integer getStatusCode()
	{
		return status;
	}

	/**
	 * Sets the current status of this submission. See the status code
	 * constants defined in this class for valid status options.
	 * @param value the status code
	 */
	public void setStatusCode(Integer value)
	{
		status = value;
	}

	/**
	 * Returns the previous submission, if this submission is a revision. 
	 * @return the previous submission, or null
	 */
	public OwlGradeSubmission getPrevSubmission()
	{
		return prevSubmission;
	}

	public boolean hasPrevSubmission()
	{
		return prevSubmission != null;
	}

	/**
	 * Sets the previous submission. 
	 * @param value the previous submission 
	 */
	public void setPrevSubmission(OwlGradeSubmission value)
	{
		prevSubmission = value;
	}

	public OwlGradeApproval getApproval()
	{
		return approval;
	}

	public boolean hasApproval()
	{
		return approval != null;
	}

	public void setApproval(OwlGradeApproval value)
	{
		approval = value;
	}

	public Set<OwlGradeSubmissionGrades> getGradeData()
	{
		return gradeData;
	}

	public void setGradeData(Set<OwlGradeSubmissionGrades> grades)
	{
		gradeData = grades;
	}

	public String getSubmissionType()
	{
		String type = INITIAL_TYPE;
	 	if (prevSubmission != null)
		{
			type = REVISION_TYPE;
		}

		return type;
	}

} // end class
