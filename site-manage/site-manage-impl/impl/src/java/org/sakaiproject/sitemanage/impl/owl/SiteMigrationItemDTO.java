package org.sakaiproject.sitemanage.impl.owl;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO used for persisting data to user site properties.
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
