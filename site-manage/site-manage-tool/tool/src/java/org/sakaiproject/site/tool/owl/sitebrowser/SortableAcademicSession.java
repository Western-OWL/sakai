package org.sakaiproject.site.tool.owl.sitebrowser;

import java.util.Date;

import lombok.Getter;

import org.sakaiproject.coursemanagement.api.AcademicSession;

/**
 * This class exists to allow easy sorting by first word of title, start date
 * in Site Browser using the standard sorting method in that class.
 * Needing to sort on first word of title and then start date is an OWL-specific
 * requirement and is therefore not a good candidate for contribution back to the Sakai
 * community.
 * See also: OWL-829
 * @author plukasew
 */
public class SortableAcademicSession implements AcademicSession
{
    @Getter private AcademicSession academicSession;
    private String titleFirstWord;

    public SortableAcademicSession(AcademicSession as)
    {
        academicSession = as;
        titleFirstWord = "";
        String[] tokens = as.getTitle().split("\\s+");
        if (tokens.length > 0)
        {
            titleFirstWord = tokens[0];
        }
    }

    public String getEid()
    {
        return academicSession.getEid();
    }

    public String getAuthority()
    {
        return academicSession.getAuthority();
    }

    /**
     * Returns on the first word of the title, to facilitate the ordering
     * desired for OWL's Site Browser
     * @return the first word of the title, or empty string if anything goes wrong
     */
    public String getTitle()
    {
        return titleFirstWord;
    }

    public String getDescription()
    {
        return academicSession.getDescription();
    }

    public Date getStartDate()
    {
        return academicSession.getStartDate();
    }

    public Date getEndDate()
    {
        return academicSession.getEndDate();
    }

    public void setDescription(String description) {}
    public void setEid(String eid) {}
    public void setAuthority(String authority) {}
    public void setTitle(String title) {}
    public void setStartDate(Date startDate) {}
    public void setEndDate(Date endDate) {}

} // end class SortableAcademicSession

