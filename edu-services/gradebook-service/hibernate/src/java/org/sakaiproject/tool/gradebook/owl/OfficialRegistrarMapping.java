package org.sakaiproject.tool.gradebook.owl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import org.sakaiproject.tool.gradebook.GradeMapping;

/**
 * An OfficialRegistrarMapping defines the set of grades available to a
 * gradebook as whole number percentages mapped onto themselves, and letter
 * as well as alphanumeric grades that aren't mapped to any numbers
 *
 * @author bbailla2
 * @author plukasew
 */
public class OfficialRegistrarMapping extends GradeMapping
{
	private static final String MAPPING_NAME = "Official Registrar Grades";

	private List grades;
	private List defaultValues;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection getGrades()
	{
		return grades;
	}

	/**
	* {@inheritDoc}
	*/
	@Override
	public List getDefaultValues()
	{
		return defaultValues;
	}

	/**
	 * ctor - makes a mapping from 100-100, 99-99, ..., 0-0
	 */
	public OfficialRegistrarMapping()
	{
		//empty super.gradeMap - we're not using it
		setGradeMap(new LinkedHashMap());

		/*
		 * construct our mapping from 100 to 0.
		 * We do this in reverse order because
		 * the default values are expected to be
		 * this way (otherwise they cannot be
		 * edited).
		 * */
		grades = new ArrayList();
		defaultValues = new ArrayList();

		//other Western grades
		addNonPercentage("AUD");
		addNonPercentage("COM");
		addNonPercentage("CR");
		addNonPercentage("FAI");
		addNonPercentage("INC");
		addNonPercentage("IPR");
		addNonPercentage("NC");
		addNonPercentage("NGR");
		addNonPercentage("PAS");
		addNonPercentage("PDG");
		addNonPercentage("SAT");
		addNonPercentage("SPC");
		addNonPercentage("SRP");
		addNonPercentage("WDN");

		addNonPercentage("A+");
		addNonPercentage("A");
		addNonPercentage("A-");
		addNonPercentage("B+");
		addNonPercentage("B");
		addNonPercentage("B-");
		addNonPercentage("C+");
		addNonPercentage("C");
		addNonPercentage("C-");
		addNonPercentage("D+");
		addNonPercentage("D");
		addNonPercentage("D-");
		addNonPercentage("F");

		for (int i = 100; i >= 0; i--)
		{
			grades.add(String.valueOf(i));
			defaultValues.add(Double.valueOf(i));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName()
	{
		return MAPPING_NAME;
	}

	private void addNonPercentage(String name)
	{
		grades.add(name);
		defaultValues.add(null);
	}
}
