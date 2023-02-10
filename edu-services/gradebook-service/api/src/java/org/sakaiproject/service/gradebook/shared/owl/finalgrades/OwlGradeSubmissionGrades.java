// 2012.05.29, plukasew, New
// Provides a snapshot of a single grade at time of submission
// 2012.07.07, plukasew, Modified
// Removed all validation for any methods that may be called by Hibernate

package org.sakaiproject.service.gradebook.shared.owl.finalgrades;

import java.io.Serializable;
import java.util.Objects;

/**
 * Provides a snapshot of a single grade at time of submission.
 * Backed by database table.
 * @author plukasew
 */
public class OwlGradeSubmissionGrades implements Serializable
{
	private Long submissionId; // submission this grade data was included in
	private String studentEid; // student username
	private String studentFirstName; // first name of student
	private String studentLastName; // last name of student
	private String studentNumber; // Western student number for this student
	private String grade; // grade in 3-char format accepted by Office of the Registar

	/**
	 * Default constructor. Sets up a "null" object with numeric values set to 0 and
	 * string values set to empty strings.
	 */
	public OwlGradeSubmissionGrades()
	{
		submissionId = 0L;
		studentEid = "";
		studentFirstName = "";
		studentLastName = "";
		studentNumber = "";
		grade = "";
	}

	/**
	 * Returns the id for the submission this grade data belongs to
	 * @return id of the submission this grade data belongs to, or 0 if not set. Will not return a null reference.
	 */
	public Long getSubmissionId()
	{
		return submissionId;
	}

	/**
	 * Sets the id for the submission this grade data belongs to
	 * @param value unique id of the submission
	 */
	public void setSubmissionId(Long value)
	{
		submissionId = value;
	}

	/**
	 * Returns the external id (username) of the student
	 * @return id (username) of the student. Returns empty string for a "null" object. Will not return a null reference.
	 */
	public String getStudentEid()
	{
		return studentEid;
	}

	/**
	 * Sets the external id (username) of the student. 
	 * @param value id (username) of student
	 */
	public void setStudentEid(String value)
	{
		studentEid = value;
	}

	/**
	 * Convenience method that returns the full name of the student, beginning with last name
	 * @return the full name of the student, or empty string if not possible. Will not return a null reference.
	 */
	public String getStudentNameLastFirst()
	{
		String name = "";
		if (!studentLastName.isEmpty())
		{
			name = studentLastName + ", " + studentFirstName;
		}
		
		return name;
	}

	public String getStudentName()
	{
		String name = "";
		if (!studentLastName.isEmpty())
		{
			name = studentFirstName + " " + studentLastName;
		}
		
		return name;
	}

	/**
	 * Returns the first name of the student
	 * @return student's first name.
	 */
	public String getStudentFirstName()
	{
		return studentFirstName;
	}

	/**
	 * Sets the first name of the student
	 * @param value student's first name
	 */
	public void setStudentFirstName(String value)
	{
		studentFirstName = value;
	}

	/**
	 * Returns the last name of the student
	 * @return student's last name
	 */
	public String getStudentLastName()
	{
		return studentLastName;
	}

	/**
	 * Sets the last name of the student
	 * @param value student's last name
	 */
	public void setStudentLastName(String value)
	{
		studentLastName = value;
	}

	/**
	 * Returns the Western student number for the student
	 * @return the student number
	 */
	public String getStudentNumber()
	{
		return studentNumber;
	}

	/**
	 * Sets the Western student number for the student
	 * @param value the student number
	 */
	public void setStudentNumber(String value)
	{
		studentNumber = value;
	}

	/**
	 * Returns the grade
	 * @return the grade
	 */
	public String getGrade()
	{
		return grade;
	}

	/**
	 * Sets the grade (this should be in 3-char Registrar's format, but is not enforced here)
	 * @param value 
	 */
	public void setGrade(String value)
	{
	   grade = value;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final OwlGradeSubmissionGrades other = (OwlGradeSubmissionGrades) obj;
		if (!Objects.equals(this.submissionId, other.submissionId) && (this.submissionId == null || !this.submissionId.equals(other.submissionId))) {
			return false;
		}
		if ((this.studentEid == null) ? (other.studentEid != null) : !this.studentEid.equals(other.studentEid)) {
			return false;
		}
		if ((this.studentFirstName == null) ? (other.studentFirstName != null) : !this.studentFirstName.equals(other.studentFirstName)) {
			return false;
		}
		if ((this.studentLastName == null) ? (other.studentLastName != null) : !this.studentLastName.equals(other.studentLastName)) {
			return false;
		}
		if ((this.studentNumber == null) ? (other.studentNumber != null) : !this.studentNumber.equals(other.studentNumber)) {
			return false;
		}
		return !((this.grade == null) ? (other.grade != null) : !this.grade.equals(other.grade));
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 37 * hash + (this.submissionId != null ? this.submissionId.hashCode() : 0);
		hash = 37 * hash + (this.studentEid != null ? this.studentEid.hashCode() : 0);
		hash = 37 * hash + (this.studentFirstName != null ? this.studentFirstName.hashCode() : 0);
		hash = 37 * hash + (this.studentLastName != null ? this.studentLastName.hashCode() : 0);
		hash = 37 * hash + (this.studentNumber != null ? this.studentNumber.hashCode() : 0);
		hash = 37 * hash + (this.grade != null ? this.grade.hashCode() : 0);
		return hash;
	}

}

