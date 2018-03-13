package org.sakaiproject.gradebookng.business.importExport;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import lombok.Getter;

import org.sakaiproject.gradebookng.business.model.GbUser;
import org.sakaiproject.gradebookng.business.model.ImportedRow;

/**
 * Identifier utility for student numbers.
 * 
 * @author plukasew, bjones86
 */
@Slf4j
public class StudentNumberIdentifier implements UserIdentifier, Serializable
{
    private final Map<String, GbUser> studentNumberMap;

    @Getter
    private final UserIdentificationReport report;

    public StudentNumberIdentifier( Map<String, GbUser> studentNumMap )
    {
        studentNumberMap = studentNumMap;
        report = new UserIdentificationReport( new HashSet<>( studentNumberMap.values() ) );
    }

    @Override
    public GbUser getUser( String studentNumber, ImportedRow row )
    {
        GbUser user = studentNumberMap.get( studentNumber );
        if( user != null )
        {
            report.addIdentifiedUser( user );
            log.debug( "User's student number {} identified as UUID: {}", studentNumber, user.getUserUuid() );
        }
        else
        {
            user = GbUser.forDisplayOnly( studentNumber, "" );
            report.addUnknownUser( user );
            log.debug( "User's student # {} is unknown to this gradebook", studentNumber );
        }

        return user;
    }
}
