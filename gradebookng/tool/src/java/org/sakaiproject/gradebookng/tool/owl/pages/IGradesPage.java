package org.sakaiproject.gradebookng.tool.owl.pages;

import java.util.Collections;
import java.util.List;

import org.apache.wicket.Session;

import org.sakaiproject.gradebookng.business.GbRole;
import org.sakaiproject.gradebookng.business.model.GbGroup;
import org.sakaiproject.gradebookng.business.owl.OwlGbStudentGradeInfo;
import org.sakaiproject.gradebookng.tool.model.GradebookUiSettings;
import org.sakaiproject.gradebookng.tool.owl.model.OwlGbUiSettings;
import org.sakaiproject.gradebookng.tool.owl.model.UiSettings;
import org.sakaiproject.gradebookng.tool.pages.BasePage;
import org.sakaiproject.tool.gradebook.Gradebook;

/**
 *
 * @author plukasew
 */
public interface IGradesPage
{
	default GbRole getCurrentRole()
	{
		return ((BasePage) this).getCurrentRole();
	}

	default List<OwlGbStudentGradeInfo> refreshStudentGradeInfo()
	{
		return Collections.emptyList();
	}

	public GradebookUiSettings getUiSettings();
	public GradebookUiSettings getUiSettings(Gradebook gradebook);

	public void setUiSettings(final GradebookUiSettings settings);

	/**
	 * Default implementation, used by GradebookPage. FinalGradesPage will override.
	 * @return
	 */
	default OwlGbUiSettings getOwlUiSettings()
	{
		return getOwlUiSettings("OWL_GB_UI_SETTINGS");
	}

	/**
	 * Default implementation, used by GradebookPage. FinalGradesPage will override.
	 * @param value
	 */
	default void setOwlUiSettings(final OwlGbUiSettings value)
	{
		setOwlUiSettings("OWL_GB_UI_SETTINGS", value);
	}

	default OwlGbUiSettings getOwlUiSettings(String key)
	{
		OwlGbUiSettings settings = (OwlGbUiSettings) Session.get().getAttribute(key);
		if (settings == null)
		{
			settings = new OwlGbUiSettings();
			setOwlUiSettings(key, settings);
		}

		return settings;
	}

	default void setOwlUiSettings(String key, final OwlGbUiSettings value)
	{
		Session.get().setAttribute(key, value);
	}

	default UiSettings getGbUiSettings()
	{
		return new UiSettings(getUiSettings(), getOwlUiSettings());
	}

	default UiSettings getGbUiSettings(Gradebook gradebook)
	{
		return new UiSettings(getUiSettings(gradebook), getOwlUiSettings());
	}

	default void setGbUiSettings(UiSettings value)
	{
		setUiSettings(value.gb);
		setOwlUiSettings(value.owl);
	}
}
