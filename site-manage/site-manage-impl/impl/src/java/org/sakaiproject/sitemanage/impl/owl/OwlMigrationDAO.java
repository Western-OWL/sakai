package org.sakaiproject.sitemanage.impl.owl;

import java.util.Optional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;

@Slf4j
public class OwlMigrationDAO
{
    @Setter
    private static SiteService siteService;

    private static final String OWL_MIG_ENABLED = "OWL_MIG_ENABLED";

    private OwlMigrationDAO() { /* Private default constructor to avoid instantiation */ }

    // TODO: when resolving the cutoff dates, refer to how "sitestats.refResolver.lessonbuilder.read.cutoverDate" is resolved

    /**
     * Checks "OWL_MIG_ENABLED" Admin site property
     * @return true if property is set to true; false otherwise
     */
    public static boolean isMigrationEnabled()
    {
        Optional<Site> s = getAdminWorksite();
        if(s.isPresent())
        {
            Site site = s.get();
            ResourceProperties props = site.getProperties();
            try
            {
                return props.getBooleanProperty(OWL_MIG_ENABLED);
            }
            catch(EntityPropertyNotDefinedException | EntityPropertyTypeException ex) { /* Property not found; ignore */ }
        }

        return false;
    }

    private static Optional<Site> getAdminWorksite()
    {
        try
        {
            return Optional.of(siteService.getSite("!admin"));
        }
        catch(IdUnusedException ex)
        {
            log.error("Unable to get !admin site");
        }

        return Optional.empty();
    }
}
