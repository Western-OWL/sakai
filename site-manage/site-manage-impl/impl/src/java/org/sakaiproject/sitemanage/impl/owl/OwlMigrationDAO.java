package org.sakaiproject.sitemanage.impl.owl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;

import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;

/**
 * This class provides all data access and persistence for the OWL Migration site properties, both global and individual user sites.
 */
@Slf4j
public class OwlMigrationDAO
{
    // Services
    @Setter private static SiteService siteService;

    // Admin Workspace prop keys
    private static final String OWL_MIG_ENABLED                             = "OWL_MIG_ENABLED";
    private static final String OWL_MIG_SELECTION_OPTS                      = "OWL_MIG_SELECTION_OPTIONS_MAP";
    private static final String OWL_MIG_STATUS_OPTS                         = "OWL_MIG_STATUS_DISPLAY_MAP";
    private static final String OWL_MIG_INIT_STATUS_MAP                     = "OWL_MIG_SELECTION_INITIAL_STATUS_MAP";
    private static final String OWL_MIG_SELECTIONS_WITH_VISIBLE_STATUSES    = "OWL_MIG_SELECTIONS_WITH_VISIBLE_STATUSES";
    private static final String OWL_MIG_VISIBLE_STATUSES                    = "OWL_MIG_VISIBLE_STATUSES";
    private static final String OWL_MIG_CHANGEABLE_SELECTIONS               = "OWL_MIG_CHANGEABLE_SELECTIONS";
    private static final String OWL_MIG_ELIGIBLE_TERMS                      = "OWL_MIG_ELIGIBLE_TERMS";
    private static final String OWL_MIG_TERM_GROUPINGS                      = "OWL_MIG_TERM_GROUPINGS";
    private static final String OWL_MIG_PROJECT_SITE_CUTOFF_DATE            = "OWL_MIG_PROJECT_SITE_CUTOFF_DATE";
    private static final String OWL_MIG_COURSE_SITE_CUTOFF_DATE             = "OWL_MIG_COURSE_SITE_CUTOFF_DATE";
    private static final String OWL_MIG_SITE_SIZE_WARN_THRESHOLD            = "OWL_MIG_SITE_SIZE_WARN_THRESHOLD";
    private static final String OWL_MIG_SITE_SIZE_ERROR_THRESHOLD           = "OWL_MIG_SITE_SIZE_ERROR_THRESHOLD";
    private static final String OWL_MIG_ADMIN_DISPLAY_NAME                  = "OWL_MIG_ADMIN_DISPLAY_NAME";
    private static final String OWL_MIG_SELECTIONS_WITH_SIZE_CHECKS         = "OWL_MIG_SELECTIONS_WITH_SIZE_CHECKS";

    // Delimiters used in Admin Workspace props
    private static final String PIPE_DELIM          = "\\|"; // Pipe is a special character in regex, so it needs to be escaped
    private static final String COLON_DELIM         = ":";
    private static final String SEMI_COLON_DELIM    = ";";

    // Admin Workspace site ID
    private static final String ADMIN_SITE_ID = "!admin";

    // User site prop keys
    private static final String OWL_MIG_USER_SELECTION          = "OWL_MIG_USER_SELECTION";
    private static final String OWL_MIG_USER_SELETION_DATE      = "OWL_MIG_USER_SELETION_DATE";
    private static final String OWL_MIG_USER_SELECTION_EID      = "OWL_MIG_USER_SELECTION_EID";
    private static final String OWL_MIG_STATUS                  = "OWL_MIG_STATUS";
    private static final String OWL_MIG_STATUS_MODIFIED_DATE    = "OWL_MIG_STATUS_MODIFIED_DATE";
    private static final String OWL_MIG_STATUS_MODIFIED_EID     = "OWL_MIG_STATUS_MODIFIED_EID";

    // Format used for storage and retrieval of Dates as Strings; ex: 2024-02-02 14:18
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";

    private OwlMigrationDAO() { /* Private default constructor to avoid instantiation */ }

    /**
     * Gets the properties for the given site ID and packs them into a SiteMigraitonItemDTO object
     * @param siteID the ID of the site to retrieve the OWL migration properties for
     * @return An Optional wrapping a SiteMigrationItemDTO object packed with the properties (or empty Strings and null dates if the properties are not found) for the given site ID, or an empty Optional if an error occurred
     * @throws IllegalArgumentException if the siteID parameter is null or empty
     */
    public static Optional<SiteMigrationItemDTO> getSiteMigrationItem( String siteID ) throws IllegalArgumentException
    {
        if( StringUtils.isBlank( siteID ) )
        {
            throw new IllegalArgumentException( "siteID cannot be null or empty" );
        }

        try
        {
            Site site = siteService.getSite( siteID );
            ResourceProperties props = site.getProperties();
            String selectionKey = StringUtils.trimToEmpty( props.getProperty( OWL_MIG_USER_SELECTION ) );
            String selectionModifiedEID = StringUtils.trimToEmpty( props.getProperty( OWL_MIG_USER_SELECTION_EID ) );
            String statusKey = StringUtils.trimToEmpty( props.getProperty( OWL_MIG_STATUS ) );
            String statusModifiedEID = StringUtils.trimToEmpty( props.getProperty( OWL_MIG_STATUS_MODIFIED_EID ) );
            String selectionModifiedDate = props.getProperty( OWL_MIG_USER_SELETION_DATE );
            String statusModifiedDate = props.getProperty( OWL_MIG_STATUS_MODIFIED_DATE );

            // Formatter for user site properties represnting datetimes, ex: "2024-02-02 14:18"
            DateFormat df = new SimpleDateFormat( DATE_FORMAT );
            Date selModDate = StringUtils.isBlank( selectionModifiedDate ) ? null : df.parse( selectionModifiedDate );
            Date statModDate = StringUtils.isBlank( statusModifiedDate ) ? null : df.parse( statusModifiedDate );
            return Optional.of( new SiteMigrationItemDTO( siteID, selectionKey, selectionModifiedEID, statusKey, statusModifiedEID, selModDate, statModDate ) );
        }
        catch( IdUnusedException | ParseException ex )
        {
            log.error("Unable to retrieve site or property for {}", siteID, ex );
            return Optional.empty();
        }
    }

    /**
     * Save or update the appropriate items from the SiteMigrationItem into site properties for the site ID packed.
     * @param dto SiteMigrationItem object containing the relevant data to save, and the site ID to save it to
     * @return true if the operation completed without issues, false if the site could not be retrieved and thus the save/update could not be performed
     * @throws IllegalArgumentException if the SiteMigrationItemDTO is null, or any of it's members are null
     */
    public static boolean saveSiteMigrationItem( SiteMigrationItemDTO dto ) throws IllegalArgumentException
    {
        if( dto == null )
        {
            throw new IllegalArgumentException( "SiteMigrationItemDTO cannot be null" );
        }
        if( dto.getSiteID() == null || dto.getSelectionKey() == null || dto.getSelectionModifiedDate() == null || dto.getSelectionModifiedEid() == null || dto.getStatusKey() == null ||
            dto.getStatusModifiedDate() == null || dto.getStatusModifiedEid() == null )
        {
            throw new IllegalArgumentException( "SiteMigrationItemDTO members cannot be null" );
        }

        try
        {
            Site site = siteService.getSite( dto.getSiteID() );
            ResourcePropertiesEdit props = site.getPropertiesEdit();
            props.addProperty( OWL_MIG_USER_SELECTION, dto.getSelectionKey() );
            props.addProperty( OWL_MIG_USER_SELECTION_EID, dto.getSelectionModifiedEid() );
            props.addProperty( OWL_MIG_STATUS, dto.getStatusKey() );
            props.addProperty( OWL_MIG_STATUS_MODIFIED_EID, dto.getStatusModifiedEid() );

            DateFormat df = new SimpleDateFormat( DATE_FORMAT );
            props.addProperty( OWL_MIG_USER_SELETION_DATE, df.format( dto.getSelectionModifiedDate() ) );
            props.addProperty( OWL_MIG_STATUS_MODIFIED_DATE, df.format( dto.getStatusModifiedDate() ) );

            siteService.save( site );
            return true;
        }
        catch( IdUnusedException | PermissionException ex )
        {
            log.error( "Unable to retrieve user site; cannot save SiteMigrationItemDTO", ex );
            return false;
        }
    }

    /**
     * Get the value stored in "OWL_MIG_ADMIN_DISPLAY_NAME" Admin site property. This value is used in the UI rather than displaying actual admin EIDs.
     * @return The dummy display name for Admin EIDs in the UI
     */
    public static Optional<String> getAdminDisplayName()
    {
        String prop = getSitePropString( OWL_MIG_ADMIN_DISPLAY_NAME );
        return Optional.ofNullable( prop );
    }

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
            catch( EntityPropertyNotDefinedException | EntityPropertyTypeException ex )
            {
                log.error( "OWL_MIG_ENABLED admin property not found, or malformed", ex );
            }
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
        String prop = getSitePropString( sitePropKey );
        return Optional.ofNullable( prop );
    }

    /**
     * Get the list of eligible term codes stored in the "OWL_MIG_ELIGIBLE_TERMS" Admin site property
     * @return A List of Strings, where each String is a term code; all sites belonging to the term codes are eligible for migration options
     */
    public static List<String> getEligibleTermsForMigration()
    {
        // Format: UWOCONT1245|UWOGRAD1241|UWOUGRD1239|UWOPREL1239|UWOCONT1239
        return parsePipeDelimitedProp( OWL_MIG_ELIGIBLE_TERMS );
    }

    /**
     * Get the list of selection keys who when chosen will trigger a site resources size check in the UI for the given site.
     * @return A List of Strings, where each String is a selection key that should trigger a site resources size check for the site when selected.
     */
    public static List<String> getSelectionsWithSizeChecks()
    {
        // Format: selfMig|assistedMig
        return parsePipeDelimitedProp( OWL_MIG_SELECTIONS_WITH_SIZE_CHECKS );
    }

    /**
     * Get the map of term groupings stored in the "OWL_MIG_TERM_GROUPINGS" Admin site property
     * @return A Map who's keys are the groupings, and the value is a List of Strings representing term code substrings. Any term code that contains the substring belongs to the given grouping
     */
    public static Map<String, List<String>> getTermGroupingMap()
    {
        // Format: Summer 2022:1225;1226|Fall/Winter 2022:1228;1229;1231|Summer 2023:1235;1236|Fall/Winter 2023:1238;1239;1241|Summer 2024:1245;1246
        LinkedHashMap<String, String> map = (LinkedHashMap) parsePipeAndColonDelimitedProp( OWL_MIG_TERM_GROUPINGS );
        if (map.isEmpty())
        {
            return Collections.emptyMap();
        }

        // Now we have key=<grouping>, value=<termSubStringList>; we need to parse out the value into a List
        LinkedHashMap<String, List<String>> retMap = new LinkedHashMap<>( map.size() );
        for( Entry<String, String> entry : map.entrySet() )
        {
            String key = entry.getKey();
            List<String> value = parseSemiColonDelimitedProp( entry.getValue() );
            retMap.put( key, value );
        }

        return retMap;
    }

    /**
     * Get the project site cutoff date stored in the "OWL_MIG_PROJECT_SITE_CUTOFF_DATE" Admin site property
     * @return LocalDate representing the date stored in Admin properties
     */
    public static Optional<LocalDate> getProjectSiteCutoffDate()
    {
        // Format: 2022-02-28
        return getSitePropLocalDate( OWL_MIG_PROJECT_SITE_CUTOFF_DATE );
    }

    /**
     * Get the course site cutoff date stored in the "OWL_MIG_COURSE_SITE_CUTOFF_DATE" Admin site property
     * @return LocalDate representing the date stored in Admin properties
     */
    public static Optional<LocalDate> getCourseSiteCutoffDate()
    {
        // Format: 2022-02-28
        return getSitePropLocalDate( OWL_MIG_COURSE_SITE_CUTOFF_DATE );
    }

    /**
     * Get the site size warning threshold value stored in "OWL_MIG_SITE_SIZE_WARN_THRESHOLD" Admin site property.
     * This value is assumed to be measured in gigabytes, with a maximum of 1 decimal place.
     * @return An Optional wrapping the parsed float value, or an empty Optional if the property was not found or could not be parsed properly.
     */
    public static Optional<Float> getSiteSizeWarningThreshold()
    {
        // Format: 1.5
        return getSitePropFloat( OWL_MIG_SITE_SIZE_WARN_THRESHOLD );
    }

    /**
     * Get the site size error threshold value stored in "OWL_MIG_SITE_SIZE_ERROR_THRESHOLD" Admin site property.
     * This value is assumed to be measured in gigabytes, with a maximum of 1 decimal place.
     * @return An Optional wrapping the parsed float value, or an empty Optional if the property was not found or could not be parsed properly.
     */
    public static Optional<Float> getSiteSizeErrorThreshold()
    {
        // Format: 2.0
        return getSitePropFloat( OWL_MIG_SITE_SIZE_ERROR_THRESHOLD );
    }

    /**
     * Utility function to get an arbitrary site property from Admin Workspace as a String
     * @param sitePropKey the key of the desired property stored in Admin site properties
     * @return The String value of the property, empty string if the site could not be resolved, or null if the property does not exist
     */
    private static String getSitePropString( String sitePropKey )
    {
        Optional<Site> s = getAdminWorksite();
        if( s.isEmpty() )
        {
            return "";
        }

        Site site = s.get();
        ResourceProperties props = site.getProperties();
        return props.getProperty( sitePropKey );
    }

    /**
     * Utility function to get an arbitrary site property from Admin Worksite, and parse it into a Float representation.
     * @param sitePropKey the key of the proprety stored in Admin site properties that contains the desired floating point value
     * @return An Optional wrapping the parsed float value, or an empty Optional if the property was not found or could not be parsed properly.
     */
    private static Optional<Float> getSitePropFloat( String sitePropKey )
    {
        String prop = getSitePropString( sitePropKey );
        if( StringUtils.isBlank( prop ) )
        {
            return Optional.empty();
        }

        try
        {
            return Optional.of( Float.valueOf( prop ) );
        }
        catch( NumberFormatException ex )
        {
            return Optional.empty();
        }
    }

    /**
     * Utility function to get an arbitrary site property from Admin Worksite, and parse it into a LocalDate representation.
     * This function assumes the String date format is 'YYYY-MM-DD', ex: 2022-02-28
     * @param sitePropKey the key of the property stored in Admin site properties that contains the desired date
     * @return An Optional containing the LocalDate representation of the String date if the property is found and can be parsed; Empty Optional if parsing fails or the property is empty or can't be found.
     */
    private static Optional<LocalDate> getSitePropLocalDate( String sitePropKey )
    {
        String date = getSitePropString( sitePropKey );
        if( StringUtils.isBlank( date ) )
        {
            return Optional.empty();
        }

        try
        {
            return Optional.of( LocalDate.parse( date ) );
        }
        catch( Exception ex )
        {
            return Optional.empty();
        }
    }

    /**
     * Utility method to parse the given String with the given delimiter
     * @param valueToParse the String value to parse with the given delimiter
     * @param delimiter the delimiter to use when parsing the String
     * @return A List of Strings
     */
    private static List<String> parseValueWithDelimiter( String valueToParse, String delimiter )
    {
        String[] entries = valueToParse.split( delimiter );
        if( entries == null )
        {
            return Collections.emptyList();
        }

        List<String> retList = new ArrayList<>( entries.length );
        retList.addAll( Arrays.asList( entries ) );
        return retList;
    }

    /**
     * Utility method that will parse the given String in the format of "value1;value2;value3" into a List of Strings
     * @param valueToParse a String in the format of "value1;value2;value3" to be parsed into a List of Strings
     * @return A List of Strings
     */
    private static List<String> parseSemiColonDelimitedProp( String valueToParse )
    {
        // Split on ';' so we get an array of key:value pairs (key1:value1, key2:value2; etc.)
        return parseValueWithDelimiter( valueToParse, SEMI_COLON_DELIM );
    }

    /**
     * Utility method that first gets a property stored in Admin site props, then transforms the property in the format of "value1|value2|value3|value4" into a List of Strings
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
        return parseValueWithDelimiter( prop, PIPE_DELIM );
    }

    /**
     * Utility method that first gets a property stored in Admin site props, then transforms the property in the format of "key1:value1|key2:value2|key3:value3" into a Map of key-value pairs
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
