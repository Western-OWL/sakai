package org.sakaiproject.gradebookng.business.owl.importExport;

import org.sakaiproject.gradebookng.business.importExport.UserIdentificationReport;
import org.sakaiproject.gradebookng.business.model.GbUserBase;
import org.sakaiproject.gradebookng.business.model.ImportedRow;

/**
 * Generic interface to be implemented by all identifier objects.
 * 
 * @author plukasew, bjones86
 */
public interface UserIdentifier
{
    /**
     * Finds the user by the given identifier
     * @param row - the row data (which captures the first and last name of users not enrolled in the site)
     * @return the user
     */
    public GbUserBase getUser(ImportedRow row);

    public UserIdentificationReport getReport();
}
