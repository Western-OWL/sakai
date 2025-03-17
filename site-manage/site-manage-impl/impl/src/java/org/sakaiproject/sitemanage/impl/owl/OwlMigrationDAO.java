package org.sakaiproject.sitemanage.impl.owl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private static final String OWL_MIG_ENABLED                     = "OWL_MIG_ENABLED";
    private static final String OWL_MIG_ACTIVE_TYPE_KEYS            = "OWL_MIG_ACTIVE_TYPE_KEYS";
    private static final String OWL_MIG_TYPE_MAP                    = "OWL_MIG_TYPE_MAP";
    private static final String OWL_MIG_ACTIVE_ADMIN_TYPE_KEYS      = "OWL_MIG_ACTIVE_ADMIN_TYPE_KEYS";
    private static final String OWL_MIG_ADMIN_TYPE_MAP              = "OWL_MIG_ADMIN_TYPE_MAP";
    private static final String OWL_MIG_TYPE_DEFAULT                = "OWL_MIG_TYPE_DEFAULT";
    private static final String OWL_MIG_TYPES_TO_ACTIONS_MAP        = "OWL_MIG_TYPES_TO_ACTIONS_MAP";
    private static final String OWL_MIG_ACTIVE_ACTION_KEYS          = "OWL_MIG_ACTIVE_ACTION_KEYS";
    private static final String OWL_MIG_ACTION_MAP                  = "OWL_MIG_ACTIONS_MAP";
    private static final String OWL_MIG_STATUS_MAP                  = "OWL_MIG_STATUS_MAP";
    private static final String OWL_MIG_ACTION_INIT_STATUS_MAP      = "OWL_MIG_ACTION_INITIAL_STATUS_MAP";
    private static final String OWL_MIG_ACTIONS_WITH_VISIBLE_STATUS = "OWL_MIG_ACTIONS_WITH_VISIBLE_STATUSES";
    private static final String OWL_MIG_VISIBLE_STATUSES            = "OWL_MIG_VISIBLE_STATUSES";
    private static final String OWL_MIG_CHANGEABLE_TYPES            = "OWL_MIG_CHANGEABLE_TYPES";
    private static final String OWL_MIG_CHANGEABLE_ACTIONS          = "OWL_MIG_CHANGEABLE_ACTIONS";
    private static final String OWL_MIG_SITE_SIZE_WARN_THRESHOLD    = "OWL_MIG_SITE_SIZE_WARN_THRESHOLD";
    private static final String OWL_MIG_SITE_SIZE_ERROR_THRESHOLD   = "OWL_MIG_SITE_SIZE_ERROR_THRESHOLD";
    private static final String OWL_MIG_ACTIONS_WITH_SIZE_CHECK     = "OWL_MIG_ACTIONS_WITH_SIZE_CHECKS";
    private static final String OWL_MIG_ADMIN_DISPLAY_NAME          = "OWL_MIG_ADMIN_DISPLAY_NAME";
    private static final String OWL_MIG_SUPPORT_EMAIL               = "OWL_MIG_SUPPORT_EMAIL";

    // Delimiters used in Admin Workspace props
    private static final String PIPE_DELIM          = "\\|"; // Pipe is a special character in regex, so it needs to be escaped
    private static final String COLON_DELIM         = ":";
    private static final String SEMI_COLON_DELIM    = ";";

    // Admin Workspace site ID
    private static final String ADMIN_SITE_ID = "!admin";

    // User site prop keys
    private static final String OWL_PROJ_MIG_TYPE_SELECTION         = "OWL_PROJ_MIG_TYPE_SELECTION";
    private static final String OWL_PROJ_MIG_ACTION_SELECTION       = "OWL_PROJ_MIG_ACTION_SELECTION";
    private static final String OWL_PROJ_MIG_TYPE_SELECTION_DATE    = "OWL_PROJ_MIG_TYPE_SELECTION_DATE";
    private static final String OWL_PROJ_MIG_ACTION_SELECTION_DATE  = "OWL_PROJ_MIG_ACTION_SELECTION_DATE";
    private static final String OWL_PROJ_MIG_TYPE_SELECTION_EID     = "OWL_PROJ_MIG_TYPE_SELECTION_EID";
    private static final String OWL_PROJ_MIG_ACTION_SELECTION_EID   = "OWL_PROJ_MIG_ACTION_SELECTION_EID";
    private static final String OWL_PROJ_MIG_STATUS                 = "OWL_PROJ_MIG_STATUS";
    private static final String OWL_PROJ_MIG_STATUS_DATE            = "OWL_PROJ_MIG_STATUS_DATE";
    private static final String OWL_PROJ_MIG_STATUS_EID             = "OWL_PROJ_MIG_STATUS_EID";

    // Format used for storage and retrieval of Dates as Strings; ex: 2024-02-02 14:18
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";

    private OwlMigrationDAO() { /* Private default constructor to avoid instantiation */ }

    /**
     * Gets the properties for the given site ID and packs them into a SiteMigraitonItemDTO object
     * @param siteID the ID of the site to retrieve the OWL migration properties for
     * @return An Optional wrapping a SiteMigrationItemDTO object packed with the properties (or empty Strings and null dates if the properties are not found) for the given site ID,
     *              or an empty Optional if an error occurred
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
            String typeKey = StringUtils.trimToEmpty( props.getProperty( OWL_PROJ_MIG_TYPE_SELECTION ) );
            String typeModifiedEID = StringUtils.trimToEmpty( props.getProperty( OWL_PROJ_MIG_TYPE_SELECTION_EID ) );
            String typeModifiedDate = props.getProperty( OWL_PROJ_MIG_TYPE_SELECTION_DATE );

            String actionKey = StringUtils.trimToEmpty( props.getProperty( OWL_PROJ_MIG_ACTION_SELECTION ) );
            String actionModifiedEID = StringUtils.trimToEmpty( props.getProperty( OWL_PROJ_MIG_ACTION_SELECTION_EID ) );
            String actionModifiedDate = props.getProperty( OWL_PROJ_MIG_ACTION_SELECTION_DATE );

            String statusKey = StringUtils.trimToEmpty( props.getProperty( OWL_PROJ_MIG_STATUS ) );
            String statusModifiedEID = StringUtils.trimToEmpty( props.getProperty( OWL_PROJ_MIG_STATUS_EID ) );
            String statusModifiedDate = props.getProperty( OWL_PROJ_MIG_STATUS_DATE );

            // Formatter for user site properties represnting datetimes, ex: "2024-02-02 14:18"
            DateFormat df = new SimpleDateFormat( DATE_FORMAT );
            Date typeModDate = parseDate( typeModifiedDate, df);
            Date actionModDate = parseDate( actionModifiedDate, df);
            Date statusModDate = parseDate( statusModifiedDate, df);

            return Optional.of( new SiteMigrationItemDTO( siteID, typeKey, typeModifiedEID, actionKey, actionModifiedEID, statusKey, statusModifiedEID, typeModDate, actionModDate, statusModDate ) );
        }
        catch( IdUnusedException ex )
        {
            log.error("Unable to retrieve site or property for {}", siteID, ex );
            return Optional.empty();
        }
    }

    /**
     * Save or update the appropriate items from the SiteMigrationItem into site properties for the site ID packed.
     * @param dto SiteMigrationItem object containing the relevant data to save, and the site ID to save it to
     * @return true if the operation completed without issues, false if the site could not be retrieved and thus the save/update could not be performed
     * @throws IllegalArgumentException if the SiteMigrationItemDTO is null, or any of it's required members are null (siteID, typeKey, typeModifiedDate, typeModifiedEid,
     *              actionKey, actionModifiedDate, actionModifiedEid)
     */
    public static boolean saveSiteMigrationItem( SiteMigrationItemDTO dto ) throws IllegalArgumentException
    {
        if( dto == null )
        {
            throw new IllegalArgumentException( "SiteMigrationItemDTO cannot be null" );
        }
        if( dto.getSiteID() == null || dto.getTypeKey() == null || dto.getTypeModifiedDate() == null || dto.getTypeModifiedEid() == null )
        {
            throw new IllegalArgumentException( "SiteMigrationItemDTO members cannot be null: siteID, typeKey, typeModifiedDate, typeModifiedEid" );
        }

        try
        {
            Site site = siteService.getSite( dto.getSiteID() );
            ResourcePropertiesEdit props = site.getPropertiesEdit();
            props.addProperty( OWL_PROJ_MIG_TYPE_SELECTION, dto.getTypeKey() );
            props.addProperty( OWL_PROJ_MIG_TYPE_SELECTION_EID, dto.getTypeModifiedEid() );
            props.addProperty( OWL_PROJ_MIG_ACTION_SELECTION, dto.getActionKey() );
            props.addProperty( OWL_PROJ_MIG_ACTION_SELECTION_EID, dto.getActionModifiedEid() );
            props.addProperty( OWL_PROJ_MIG_STATUS, StringUtils.trimToEmpty( dto.getStatusKey() ) );
            props.addProperty( OWL_PROJ_MIG_STATUS_EID, StringUtils.trimToEmpty( dto.getStatusModifiedEid() ) );

            DateFormat df = new SimpleDateFormat( DATE_FORMAT );
            String actionModifiedDate = dto.getActionModifiedDate() != null ? df.format( dto.getActionModifiedDate() ) : "";
            String statusModifiedDate = dto.getStatusModifiedDate() != null ? df.format( dto.getStatusModifiedDate() ) : "";

            props.addProperty( OWL_PROJ_MIG_TYPE_SELECTION_DATE, df.format( dto.getTypeModifiedDate() ) );
            props.addProperty( OWL_PROJ_MIG_ACTION_SELECTION_DATE, actionModifiedDate );
            props.addProperty( OWL_PROJ_MIG_STATUS_DATE, statusModifiedDate );

            siteService.save( site );
            return true;
        }
        catch( IdUnusedException | PermissionException ex )
        {
            log.error( "Unable to retrieve user site by ID [{}]; cannot save SiteMigrationItemDTO", dto.getSiteID(), ex );
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
        return Optional.ofNullable( StringUtils.trimToNull( prop ) );
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
                log.error( "OWL_MIG_ENABLED admin property not found, or malformed!" );
            }
        }

        return false;
    }

    /**
     * Get the support email address stored in the OWL_MIG_SUPPORT_EMAIL Admin site property
     * @return An Optional wrapping the email address String, or empty Optional if the property wasn't found
     */
    public static Optional<String> getSupportEmailAddress()
    {
        String supportEmail = StringUtils.trimToNull( getSitePropString( OWL_MIG_SUPPORT_EMAIL ) );
        return Optional.ofNullable( supportEmail );
    }

    /**
     * Get the List of type keys which are "active" (available to be selected in the UI)
     * @return A List of Strings, where each String is a type key which is available in the UI for selection by end users
     */
    public static List<String> getActiveTypeKeys()
    {
        // Format: undecided|acad|research|hrTrain|stuTrain|empTrain|extTrain|extOther|nonInstruct|other
        return parsePipeDelimitedProp( OWL_MIG_ACTIVE_TYPE_KEYS );
    }

    /**
     * Get the default type value that will be displayed in the UI when there are no "active" type keys
     * @return An Optional containing the default type value, or an empty Optional if the property can't be found or parsed properly
     */
    public static Optional<String> getDefaultTypeOption()
    {
        String defaultSelectionOption = StringUtils.trimToNull( getSitePropString( OWL_MIG_TYPE_DEFAULT ) );
        return Optional.ofNullable( defaultSelectionOption );
    }

    /**
     * Get the UI type options stored in the "OWL_MIG_TYPE_MAP" Admin site property
     * @return A map, where the map's key is the type option key, and the map's value is the user facing type option
     */
    public static Map<String, String> getTypeOptions()
    {
        // Format: undecided:Undecided|acad:Supplementary Academic Materials|research:Research
        return parsePipeAndColonDelimitedProp( OWL_MIG_TYPE_MAP );
    }

    /**
     * Get the List of admin-only type keys which are "active" (available to be selected in the UI)
     * @return A List of Strings, where each String is a type key which is available in the UI for selection by admins only
     */
    public static List<String> getActiveAdminTypeKeys()
    {
        // Format: undecided|acad|research|hrTrain|stuTrain|empTrain|extTrain|extOther|nonInstruct|other
        return parsePipeDelimitedProp( OWL_MIG_ACTIVE_ADMIN_TYPE_KEYS );
    }

    /**
     * Get the UI type options stored in the "OWL_MIG_ADMIN_TYPE_MAP" Admin site property
     * @return A map, where the map's key is the type option key, and the map's value is the admin facing type option
     */
    public static Map<String, String> getAdminTypeOptions()
    {
        // Format: undecided:Undecided|acad:Supplementary Academic Materials|research:Research
        return parsePipeAndColonDelimitedProp( OWL_MIG_ADMIN_TYPE_MAP );
    }

    /**
     * Get the map of type->actions stored in the "OWL_MIG_TYPES_TO_ACTIONS_MAP" Admin site property
     * @return A Map who's keys are type keys, and the value is a List of action keys available for the given type
     */
    public static Map<String, List<String>> getTypesToActionsMap()
    {
        // Format: acad:undecided;mig;alt;ret;selfDel;del|research:undecided;alt;ret;selfDel;del|hrTrain:undecided;mig;alt;ret;selfDel;del
        Map<String, String> map = parsePipeAndColonDelimitedProp( OWL_MIG_TYPES_TO_ACTIONS_MAP );
        if (map.isEmpty())
        {
            return Collections.emptyMap();
        }

        // Now we have key=<typeKey>, value=<actionKeyList>; we need to parse out the value into a List
        LinkedHashMap<String, List<String>> retMap = new LinkedHashMap<>( map.size() );
        for( Map.Entry<String, String> entry : map.entrySet() )
        {
            String key = entry.getKey();
            List<String> value = parseSemiColonDelimitedProp( entry.getValue() );
            retMap.put( key, value );
        }

        return retMap;
    }

    /**
     * Get the List of action keys which are "active" (available to be selected in the UI)
     * @return A List of Strings, where each String is an action key which is available in the UI for selection by end users
     */
    public static List<String> getActiveActionKeys()
    {
        // Format: undecided|mig|alt|ret|selfDel|del
        return parsePipeDelimitedProp( OWL_MIG_ACTIVE_ACTION_KEYS );
    }

    /**
     * Get the UI action options stored in the "OWL_MIG_ACTIONS_MAP" Admin site property
     * @return A map, where the map's key is the action option key, and the map's value is the user facing action option
     */
    public static Map<String, String> getActionOptions()
    {
        // Format: undecided:Undecided|mig:Request Migration to OWL Brightspace|alt:Transition to Alternate Solution
        return parsePipeAndColonDelimitedProp( OWL_MIG_ACTION_MAP );
    }

    /**
     * Get the status options map stored in the "OWL_MIG_STATUS_MAP" Admin site property
     * @return A map, where the map's key is the status option key, and the map's value is the (sometimes) user facing status option
     */
    public static Map<String, String> getStatusOptions()
    {
        // Format: migDone:Migrated|pendingMig:Migration Pending|transDone:Transitioned to Alternate Solution|transPending:Transition to Alternate Solution Pending
        return parsePipeAndColonDelimitedProp( OWL_MIG_STATUS_MAP );
    }

    /**
     * Get the initial status map stored in "OWL_MIG_ACTION_INITIAL_STATUS_MAP" Admin site property
     * @return A map, where the map's key is the action option key, and the map's value is the initial status key
     */
    public static Map<String, String> getInitialActionStatusMap()
    {
        // Format: mig:pendingMig|alt:transPending|ret:ret|selfDel:selfDel|del:del
        return parsePipeAndColonDelimitedProp( OWL_MIG_ACTION_INIT_STATUS_MAP );
    }

    /**
     * Get the actions with visible statuses stored in "OWL_MIG_ACTIONS_WITH_VISIBLE_STATUSES" Admin site property.
     * NOTE: if the action key is not in this list, the status will not be displayed even if it is contained in getVisibleStatuses() (below)
     * @return List of Strings, where each String is an action key who's statuses are allowed to be exposed in the UI
     */
    public static List<String> getActionsWithVisibleStatuses()
    {
        // Format: mig|alt|ret|selfDel|del
        return parsePipeDelimitedProp( OWL_MIG_ACTIONS_WITH_VISIBLE_STATUS );
    }

    /**
     * Get the visible statuses stored in "OWL_MIG_VISIBLE_STATUSES" Admin site property
     * @return List of Strings, where each String is a status key who's corresponding value is allowed to be exposed in the UI
     */
    public static List<String> getVisibleStatuses()
    {
        // Format: migDone|pendingMig|transDone|transPending|ret|selfDel|del
        return parsePipeDelimitedProp( OWL_MIG_VISIBLE_STATUSES );
    }

    /**
     * Get the list of changeable types stored in the "OWL_MIG_CHANGEABLE_TYPES" Admin site property
     * @return List of Strings, where each String is a type key that is allowed to be changed in the UI by end users
     */
    public static List<String> getChangeableTypes()
    {
        // Format: undecided|other
        return parsePipeDelimitedProp( OWL_MIG_CHANGEABLE_TYPES );
    }

    /**
     * Get the list of changeable actions stored in the "OWL_MIG_CHANGEABLE_ACTIONS" Admin site property
     * @return List of Strings, where each String is an action key that is allowed to be changed in the UI by end users
     */
    public static List<String> getChangeableActions()
    {
        // Format: undecided|actionKey2
        return parsePipeDelimitedProp( OWL_MIG_CHANGEABLE_ACTIONS );
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
        return Optional.ofNullable( StringUtils.trimToNull( prop ) );
    }

    /**
     * Get the list of action keys who when chosen will trigger a site resources size check in the UI for the given site.
     * @return A List of Strings, where each String is an action key that should trigger a site resources size check for the site when selected.
     */
    public static List<String> getActionsWithSizeChecks()
    {
        // Format: mig|actionKey2|actionKey3
        return parsePipeDelimitedProp( OWL_MIG_ACTIONS_WITH_SIZE_CHECK );
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
     * Parse the given date String into a Date object
     * @param date String representation of a Date
     * @param df DateFormat object to use when parsing
     * @return Date object equivalent of the input String, or null if the String is empty, blank, or null itself
     */
    private static Date parseDate( String date, DateFormat df )
    {
        if( StringUtils.isBlank( date ) )
        {
            return null;
        }

        try
        {
            return df.parse( date );
        }
        catch( ParseException e )
        {
            log.error("Unable to parse date String to Date: {}", date );
            return null;
        }
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

        return s.get().getProperties().getProperty( sitePropKey );
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
        if( StringUtils.isBlank( prop ) )
        {
            return Collections.emptyList();
        }

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
