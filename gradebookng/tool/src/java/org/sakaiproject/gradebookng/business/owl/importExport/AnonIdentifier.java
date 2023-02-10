package org.sakaiproject.gradebookng.business.owl.importExport;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import org.sakaiproject.gradebookng.business.importExport.UserIdentificationReport;
import org.sakaiproject.gradebookng.business.model.GbUnidentifiedUser;

import org.sakaiproject.gradebookng.business.model.GbUser;
import org.sakaiproject.gradebookng.business.model.GbUserBase;
import org.sakaiproject.gradebookng.business.model.ImportedRow;

/**
 * Identifier utility for anonymous IDs.
 * 
 * @author plukasew, bjones86
 */
@Slf4j
public class AnonIdentifier implements UserIdentifier, Serializable
{
    private final Map<String, GbUser> anonIdMap;  // OWLTODO: serializing this large map might be costly

    @Getter
    private final UserIdentificationReport report;

    public AnonIdentifier( Map<String, GbUser> anonymousIdMap )
    {
        anonIdMap = anonymousIdMap;
        report = new UserIdentificationReport( new HashSet<>( anonIdMap.values() ) );
    }

    @Override
    public GbUserBase getUser( ImportedRow row )
    {
		String anonID = row.getAnonID();
        GbUser user = anonIdMap.get( anonID );
        if( user != null )
        {
            report.addIdentifiedUser( user );
            log.debug( "User's anon ID {} identified as UUID: {}", anonID, user.getUserUuid() );
			return user;
        }

		GbUnidentifiedUser uu = new GbUnidentifiedUser(anonID, "");
		report.addUnknownUser( uu );
		log.debug( "User's anon ID {} is unknown to this gradebook", anonID );
        return uu;
    }
}
