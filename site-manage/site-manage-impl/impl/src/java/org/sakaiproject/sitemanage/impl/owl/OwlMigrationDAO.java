package org.sakaiproject.sitemanage.impl.owl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

    // Admin Workspace prop keys
    private static final String OWL_MIG_ENABLED = "OWL_MIG_ENABLED";
    private static final String OWL_MIG_SELECTION_OPTS = "OWL_MIG_SELECTION_OPTIONS_MAP";
    private static final String OWL_MIG_STATUS_OPTS = "OWL_MIG_STATUS_DISPLAY_MAP";

    // Delimiters used in Admin Workspace props
    private static final String PIPE_DELIM = "\\|";
    private static final String COLON_DELIM = ":";
    private static final String SEMI_COLON_DELIM = ";";

    private static final String ADMIN_SITE_ID = "!admin";

    private OwlMigrationDAO() { /* * Private default constructor to avoid instantiation */ }

    // TODO: when resolving the cutoff dates, refer to how "sitestats.refResolver.lessonbuilder.read.cutoverDate" is resolved

    /**
     * Checks "OWL_MIG_ENABLED" Admin site property
     *
     * @return true if property is set to true; false otherwise
     */
    public static boolean isMigrationEnabled()
    {
        Optional<Site> s = getAdminWorksite();
        if( s.isPresent() )
        {
            Site site = s.get();
            ResourceProperties props = site.getProperties();
            try
            {
                return props.getBooleanProperty( OWL_MIG_ENABLED );
            }
            catch( EntityPropertyNotDefinedException | EntityPropertyTypeException ex ) { /* * Property not found; ignore */ }
        }

        return false;
    }

    /**
     * Get the UI selection options stored in the "OWL_MIG_SELECTION_OPTIONS_MAP" Admin site property
     * @return A map, where the map's key is the selection option key, and the map's value is the user facing selection option
     */
    public static Map<String, String> getMigrationSelectionOptions()
    {
        Optional<Site> s = getAdminWorksite();
        if( s.isEmpty() )
        {
            return Collections.emptyMap();
        }

        Site site = s.get();
        ResourceProperties props = site.getProperties();

        // Format: undecided:Undecided|doNotMig:Do Not Migrate|selfMig:Self-Migration|assistedMig:Assisted Migration
        String prop = props.getProperty( OWL_MIG_SELECTION_OPTS );

        return parsePipeAndColonDelimitedProp(prop);
    }

    /**
     * Get the status options map stored in the "OWL_MIG_STATUS_DISPLAY_MAP" Admin site property
     * @return A map, where the map's key is the status option key, and the map's value is the (sometimes) user facing status option
     */
    public static Map<String, String> getMigrationStatusOptions()
    {
        Optional<Site> s = getAdminWorksite();
        if( s.isEmpty() )
        {
            return Collections.emptyMap();
        }

        Site site = s.get();
        ResourceProperties props = site.getProperties();

        // Format: migDone:Migrated|doNotMig:Do Not Migrate|manualMig:Manual Migration|pendingMig:Migration Pending|toBeDeleted:To Be Deleted|projPendingMig:Move Pending
        String prop = props.getProperty( OWL_MIG_STATUS_OPTS );

        return parsePipeAndColonDelimitedProp(prop);
    }

    /**
     * Utility method that will take transform a String in the format of "key1:value1|key2:value2|key3:value3" into a Map of key-value pairs
     * @param prop the pipe and colon delimited string (key1:value1|key2:value2|key3:value3)
     * @return A Map of key-value pairs
     */
    private static Map<String, String> parsePipeAndColonDelimitedProp(String prop)
    {
        // Split on '|' so we get an array of key:value pairs (undecided:Undecided, doNotMig:Do Not Migrate; etc.)
        String[] entries = prop.split( PIPE_DELIM );
        if( entries == null )
        {
            return Collections.emptyMap();
        }

        Map<String, String> retMap = new HashMap<>( entries.length );
        for( String entry : entries )
        {
            // Split on ':' so we have key and value separately (undecided, Undecided; etc.)
            String[] keyValue = entry.split( COLON_DELIM );
            if( keyValue != null && keyValue.length == 2 )
            {
                retMap.put( keyValue[0], keyValue[1] );
            }
        }

        return retMap;
    }

    /**
     * Utility method to retrieve the !admin worksite
     * @return an Optional wrapping the Site object, or an empty Optional if the site could not be retreived
     */
    private static Optional<Site> getAdminWorksite()
    {
        try
        {
            return Optional.of( siteService.getSite( ADMIN_SITE_ID ) );
        }
        catch( IdUnusedException ex )
        {
            log.error( "Unable to get !admin site" );
        }

        return Optional.empty();
    }
}
