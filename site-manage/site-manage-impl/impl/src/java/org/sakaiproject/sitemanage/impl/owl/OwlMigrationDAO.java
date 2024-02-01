package org.sakaiproject.sitemanage.impl.owl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
    private static final String OWL_MIG_ENABLED                             = "OWL_MIG_ENABLED";
    private static final String OWL_MIG_SELECTION_OPTS                      = "OWL_MIG_SELECTION_OPTIONS_MAP";
    private static final String OWL_MIG_STATUS_OPTS                         = "OWL_MIG_STATUS_DISPLAY_MAP";
    private static final String OWL_MIG_INIT_STATUS_MAP                     = "OWL_MIG_SELECTION_INITIAL_STATUS_MAP";
    private static final String OWL_MIG_SELECTIONS_WITH_VISIBLE_STATUSES    = "OWL_MIG_SELECTIONS_WITH_VISIBLE_STATUSES";
    private static final String OWL_MIG_VISIBLE_STATUSES                    = "OWL_MIG_VISIBLE_STATUSES";
    private static final String OWL_MIG_CHANGEABLE_SELECTIONS               = "OWL_MIG_CHANGEABLE_SELECTIONS";
    private static final String OWL_MIG_ELIGIBLE_TERMS                      = "OWL_MIG_ELIGIBLE_TERMS";

    // Delimiters used in Admin Workspace props
    private static final String PIPE_DELIM          = "\\|"; // Pipe is a special character in regex, so it needs to be escaped
    private static final String COLON_DELIM         = ":";
    private static final String SEMI_COLON_DELIM    = ";";

    private static final String ADMIN_SITE_ID = "!admin";

    private OwlMigrationDAO() { /* Private default constructor to avoid instantiation */ }

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
            catch( EntityPropertyNotDefinedException | EntityPropertyTypeException ex ) { /* Property not found; ignore */ }
        }

        return false;
    }

    /**
     * Get the UI selection options stored in the "OWL_MIG_SELECTION_OPTIONS_MAP" Admin site property
     * @return A map, where the map's key is the selection option key, and the map's value is the user facing selection option
     */
    public static Map<String, String> getMigrationSelectionOptions()
    {
        // Format: undecided:Undecided|doNotMig:Do Not Migrate|selfMig:Self-Migration|assistedMig:Assisted Migration
        return parsePipeAndColonDelimitedProp( OWL_MIG_SELECTION_OPTS );
    }

    /**
     * Get the status options map stored in the "OWL_MIG_STATUS_DISPLAY_MAP" Admin site property
     * @return A map, where the map's key is the status option key, and the map's value is the (sometimes) user facing status option
     */
    public static Map<String, String> getMigrationStatusOptions()
    {
        // Format: migDone:Migrated|doNotMig:Do Not Migrate|manualMig:Manual Migration|pendingMig:Migration Pending|toBeDeleted:To Be Deleted|projPendingMig:Move Pending
        return parsePipeAndColonDelimitedProp( OWL_MIG_STATUS_OPTS );
    }

    /**
     * Get the initial status map stored in "OWL_MIG_SELECTION_INITIAL_STATUS_MAP" Admin site property
     * @return A map, where the map's key is the migration selection option key, and the map's value is the initial status key
     */
    public static Map<String, String> getMigrationInitialStatusMap()
    {
        // Format: doNotMig:doNotMig|selfMig:manualMig|assistedMig:pendingMig
        return parsePipeAndColonDelimitedProp( OWL_MIG_INIT_STATUS_MAP );
    }

    /**
     * Get the selections with visible statuses stored in "OWL_MIG_SELECTIONS_WITH_VISIBLE_STATUSES" Admin site property.
     * NOTE: if the selection key is not in this list, the status will not be displayed even if it is contained in getVisibleStatuses() (below)
     * @return List of Strings, where each String is a selection key who's statuses are allowed to be exposed in the UI
     */
    public static List<String> getSelectionsWithVisibleStatuses()
    {
        // Format: assistedMig|statusKey2|statusKey3
        return parsePipeDelimitedProp( OWL_MIG_SELECTIONS_WITH_VISIBLE_STATUSES );
    }

    /**
     * Get the visible statuses stored in "OWL_MIG_VISIBLE_STATUSES" Admin site property
     * @return List of Strings, where each String is a status key who's corresponding value is allowed to be exposed in the UI
     */
    public static List<String> getVisibleStatuses()
    {
        // Format: migDone|pendingMig
        return parsePipeDelimitedProp( OWL_MIG_VISIBLE_STATUSES );
    }

    /**
     * Get the list of changeable selections stored in the "OWL_MIG_CHANGEABLE_SELECTIONS" Admin site property
     * @return List of Strings, where each String is a selection key that is allowed to be changed in the UI by end users
     */
    public static List<String> getChangeableSelections()
    {
        // Format: undecided|selectionKey2|selectionKey3
        return parsePipeDelimitedProp( OWL_MIG_CHANGEABLE_SELECTIONS );
    }

    /**
     * Utility method to retrieve an arbitrary property stored in Admin site properties.
     * This will be used mostly to retrieve the banners, and confirmation messages.
     * @param sitePropKey the key of the property to retrieve from Admin site properties
     * @return An Optional containing the value of the property, or an empty Optional if the property couldn't be found
     */
    public static Optional<String> getUiMessage( String sitePropKey )
    {
        Optional<Site> s = getAdminWorksite();
        if( s.isEmpty() )
        {
            return Optional.empty();
        }

        Site site = s.get();
        ResourceProperties props = site.getProperties();
        String prop = props.getProperty( sitePropKey );
        return prop == null ? Optional.empty() : Optional.of( prop );
    }

    /**
     * Get the list of eligible term codes stored in the "OWL_MIG_ELIGIBLE_TERMS" Admin site property
     * @return A List of Strings, where each String is a term code; all sites belonging to the term codes are eligible for migration options
     */
    public static List<String> getEligibleTermsForMigration()
    {
        // Format: <termCode1>|<termCode2>|<termCode3>
        return parsePipeDelimitedProp( OWL_MIG_ELIGIBLE_TERMS );
    }

    /**
     * Utility method that will transform a String in the format of "value1|value2|value3|value4" into a List of Strings
     * @param sitePropKey the key of the property stored in Admin site properties that contains the pipe delimited string (value1|value2|value3|value4)
     * @return A List of Strings
     */
    private static List<String> parsePipeDelimitedProp( String sitePropKey )
    {
        Optional<Site> s = getAdminWorksite();
        if( s.isEmpty() )
        {
            return Collections.emptyList();
        }

        Site site = s.get();
        ResourceProperties props = site.getProperties();

        // Format: value1|value2|value3
        String prop = props.getProperty( sitePropKey );

        // Split on '|' so we get an array of key:value pairs (key1:value1, key2:value2; etc.)
        String[] entries = prop.split( PIPE_DELIM );
        if( entries == null )
        {
            return Collections.emptyList();
        }

        List<String> retList = new ArrayList<>( entries.length );
        retList.addAll( Arrays.asList( entries ) );

        return retList;
    }

    /**
     * Utility method that will transform a String in the format of "key1:value1|key2:value2|key3:value3" into a Map of key-value pairs
     * @param sitePropKey the key of the property stored in Admin site properties that contains the pipe and colon delimited string (key1:value1|key2:value2|key3:value3)
     * @return A Map of key-value pairs
     */
    private static Map<String, String> parsePipeAndColonDelimitedProp( String sitePropKey )
    {
        // Split on '|' so we get a List of key:value pairs (key1:value1, key2:value2; etc.)
        List<String> entries = parsePipeDelimitedProp( sitePropKey );
        if( entries.isEmpty() )
        {
            return Collections.emptyMap();
        }

        LinkedHashMap<String, String> retMap = new LinkedHashMap<>( entries.size() );
        for( String entry : entries )
        {
            // Split on ':' so we have key and value separately
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
