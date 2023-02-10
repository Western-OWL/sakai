// 2013.10.07, bbailla2, New
// Represents a triple: (section eid, user eid, anonymous grading id)

package org.sakaiproject.service.gradebook.shared.owl.anongrading;

import java.io.Serializable;

/**
 * Represents a triple: (section eid, user eid, anonymous grading id).
 * Backed by a database table.
 * @author bbailla2
 */
public class OwlAnonGradingID implements Serializable
{
	private Long id; // primary key in database
	private String sectionEid; // the section containing this anonymous grading id
	private String userEid; // the user associated with this grading id(username)
	private Integer anonGradingID; // the anonymous grading id

	/**
	 * Default constructor. Sets up a "null" object with numeric values set to 0,
	 * string values set to empty string.
	 */
	public OwlAnonGradingID()
	{
		id = Long.valueOf(0L);
		sectionEid = "";
		userEid = "";
		anonGradingID = Integer.valueOf(0);
	}

	/**
	 * Returns a unique identifier (database primary key) for this anonymous grading id
	 * @return unique id for this anonymous grading id
	 */
	public Long getId()
	{
		return id;
	}

	/**
	 * Sets the unique id (database primary key) for this anonymous grading id.
	 * Private because the database should manage the ids.
	 * @param value
	 */
	private void setId(Long value)
	{
		id = value;
	}

	/**
	 * Retuns the external id of the course section
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
	 * Returns the external id (username) of the student
	 * @return id (username) of the student
	 */
	public String getUserEid()
	{
		return userEid;
	}

	/**
	 * Sets the external id (username) of the student
	 * @param value id (username) of the student.
	 */
	public void setUserEid(String value)
	{
		userEid = value;
	}

	/**
	 * Returns the anonymous grading id of the student
	 * @return anonymous grading id of the student
	 */
	public Integer getAnonGradingID()
	{
		return anonGradingID;
	}

	/**
	 * Sets the anonymous grading id of the student
	 * @param value anonymous grading id of the student
	 */
	public void setAnonGradingID(Integer value)
	{
		anonGradingID = value;
	}

}
