package org.sakaiproject.sitemanage.impl.owl;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO used for persisting data to user site properties.
 *
 * NOTE: if a property can't be found in the given site, default values of empty String and null Date will be used accordingly.
 */
@Data
@AllArgsConstructor
public class SiteMigrationItemDTO
{
    private String siteID;
    private String selectionKey;
    private String selectionModifiedEid;
    private String statusKey;
    private String statusModifiedEid;
    private Date selectionModifiedDate;
    private Date statusModifiedDate;
}
