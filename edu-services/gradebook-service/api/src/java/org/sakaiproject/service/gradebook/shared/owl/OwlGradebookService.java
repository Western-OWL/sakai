package org.sakaiproject.service.gradebook.shared.owl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sakaiproject.service.gradebook.shared.owl.anongrading.OwlAnonGradingID;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeApproval;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeSubmission;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.report.FGChanges;
import org.sakaiproject.tool.gradebook.facades.owl.OwlAuthz;

/**
 * Bolt-on interface to provide OWL methods to GradebookService. This allows GradebookServiceHibernateImpl to provide a delegate
 * implementation that is compatible with Spring/Hibernate transaction management.
 *
 * @author plukasew
 */
public interface OwlGradebookService
{
	/**
	 * DO NOT CALL THIS METHOD DIRECTLY, IT IS FOR INTERNAL USE ONLY
	 * @return an implementation of OwlGradebookService
	 */
	default OwlGradebookService owlDoNotCall()
	{
		return null; // proper impls will override this, mocks will get this functionality
	}

	default OwlAuthz owlAuthz()
	{
		return owlDoNotCall().owlAuthz();
	}

	default List<OwlGradeSubmission> getAllCourseGradeSubmissionsForSectionInSite(final String sectionEid, final String siteId) throws IllegalArgumentException
	{
		return owlDoNotCall().getAllCourseGradeSubmissionsForSectionInSite(sectionEid, siteId);
	}

	default OwlGradeSubmission getMostRecentCourseGradeSubmissionForSectionInSite(final String sectionEid, final String siteId) throws IllegalArgumentException
	{
		return owlDoNotCall().getMostRecentCourseGradeSubmissionForSectionInSite(sectionEid, siteId);
	}

	// OWL-1228  --plukasew
	default boolean isSectionInSiteApproved(final String sectionEid, final String siteId) throws IllegalArgumentException
	{
		return owlDoNotCall().isSectionInSiteApproved(sectionEid, siteId);
	}

	default boolean areAllSectionsInSiteApproved(final Set<String> sectionEids, final String siteId) throws IllegalArgumentException
	{
		return owlDoNotCall().areAllSectionsInSiteApproved(sectionEids, siteId);
	}

	default Long createSubmission(final OwlGradeSubmission sub) throws IllegalArgumentException
	{
		return owlDoNotCall().createSubmission(sub);
	}

	default void updateSubmission(final OwlGradeSubmission sub) throws IllegalArgumentException
	{
		owlDoNotCall().updateSubmission(sub);
	}

	default Long createApproval(final OwlGradeApproval approval) throws IllegalArgumentException
	{
		return owlDoNotCall().createApproval(approval);
	}

	default boolean isOfficialRegistrarGradingSchemeInUse( final Long gradebookID )
	{
		return owlDoNotCall().isOfficialRegistrarGradingSchemeInUse(gradebookID);
	}

	/* Begin OWL anonymous grading methods  --plukasew
	 *
	 * These methods interface with the owl_anon_grading_id table and are
	 * for accessing and storing anonymous grading ids sourced from Registrar.
	 */

	/**
	 * Returns the contents of the entire OWL_ANON_GRADING_ID table. Only cron jobs/web services should use this method
	 * @returns the list of all OwlAnonGradingIDs in the database
	 */
	default List<OwlAnonGradingID> getAnonGradingIds()
	{
		return owlDoNotCall().getAnonGradingIds();
	}

	/**
	 * Returns all anonymous grading ids for the given section
	 * @param sectionEid the section eid, cannot be null or empty
	 * @return a list of OwlAnonGradingID objects for the given section
	 * @throws IllegalArgumentException if sectionEid is null or empty
	 */
	default List<OwlAnonGradingID> getAnonGradingIdsForSection(final String sectionEid) throws IllegalArgumentException
	{
		return owlDoNotCall().getAnonGradingIdsForSection(sectionEid);
	}

	/**
	 * Returns all anonymous grading ids with matching gradingIDs
	 * @param gradingIDs a collection of gradingIDs to look up
	 * @return a list of OwlAnonGradingIDs associated with the given gradingIDs
	 * @throws IllegalArgumentException if gradingIDs is null or empty
	 */
	default List<OwlAnonGradingID> getAnonGradingIDsByGradingIDs(final Collection<Integer> gradingIDs) throws IllegalArgumentException
	{
		return owlDoNotCall().getAnonGradingIDsByGradingIDs(gradingIDs);
	}

	/**
	 * Returns all anonymous grading ids with matching sectionEIDs
	 * @param sectionEIDs a collection of sectionEIDs to look up
	 * @return a list of OwlAnonGradingIDs associated with the given sectionEIDs
	 * @throws IllegalArgumentException if sectionEIDs is null or empty
	 */
	default List<OwlAnonGradingID> getAnonGradingIDsBySectionEIDs(final Collection<String> sectionEIDs) throws IllegalArgumentException
	{
		return owlDoNotCall().getAnonGradingIDsBySectionEIDs(sectionEIDs);
	}

	/**
	 * Returns all anonymous grading ids with matching sectionEids, keyed by userEid with value
	 * mapping sectionEid -> anonId
	 * @param sectionEids the sectionEids to look up
	 * @return mapping of userEid to collection of sectionEid/anonId pairs, will be empty if sectionEids null/empty or anonIds not found
	 */
	default Map<String, Map<String, String>> getAnonGradingIdMapBySectionEids(final Set<String> sectionEids)
	{
		return owlDoNotCall().getAnonGradingIdMapBySectionEids(sectionEids);
	}

	/**
	 * Returns the anonymous grading id corresponding to the give section/user combination. Do not use this method unless
	 * you really only need to work with a single grading id.
	 * @param sectionEid the section eid, cannot be null or empty
	 * @param userEid the user eid, cannot be null or empty
	 * @return the corresponding OwlAnonGradingID object, or empty if not found
	 * @throws IllegalArgumentException if sectionEid/userEid is null or empty
	 */
	default Optional<OwlAnonGradingID> getAnonGradingId(final String sectionEid, final String userEid) throws IllegalArgumentException
	{
		return owlDoNotCall().getAnonGradingId(sectionEid, userEid);
	}

	/**
	 * Persists a new OwlAnonGradingID object (new row in the db). Do not use this method unless you really
	 * only need to work with a single grading id.
	 * @param gradingId the new object to be persisted, cannot be null or have a grading id of 0
	 * @return the unique identifier for the newly-persisted object
	 * @throws IllegalArgumentException if gradingId is null or has a grading id value of 0
	 */
	default Long createAnonGradingId(final OwlAnonGradingID gradingId) throws IllegalArgumentException
	{
		return owlDoNotCall().createAnonGradingId(gradingId);
	}

	/**
	 * Updates an existing persistent OwlAnonGradingID object (existing row in db). Do not use this method unless you really only need to work with a single grading id.
	 */
	default void updateAnonGradingId(final OwlAnonGradingID gradingId) throws IllegalArgumentException
	{
		owlDoNotCall().updateAnonGradingId(gradingId);
	}

	/**
	 * Persist multiple OwlAnonGradingID objects (new rows in the db). Prefer this method over multiple calls to createAnonGradingId().
	 * Null values or "null" objects in the collection are skipped. The number of successfully persisted objects is returned.
	 * @param gradingIds the set of objects to be persisted, cannot be null
	 * @return the number of objects that were successfully persisted
	 * @throws IllegalArgumentException if gradingIds is null
	 */
	default int createAnonGradingIds(final Set<OwlAnonGradingID> gradingIds) throws IllegalArgumentException
	{
		return owlDoNotCall().createAnonGradingIds(gradingIds);
	}

	/**
	 * Updates multiple OwlAnonGradingID objects (existing rows in the db). Prefer this method over multiple calls to updateAnonGradingId().
	 * Null values or "null" objects in the collection are skipped. The number of successfully persisted objects is returned.
	 * @param gradingIds the set of objects to be updated, cannot be null
	 * @return the number of objects that were successfully updated
	 * @throws IllegalArgumentExcpetion if gradingIds is null
	 */
	default int updateAnonGradingIds(final Set<OwlAnonGradingID> gradingIds) throws IllegalArgumentException
	{
		return owlDoNotCall().updateAnonGradingIds(gradingIds);
	}

	/**
	 * Deletes an existing persistent OwlAnonGradingID object (existing row in db). Do not use this method unless you really
	 * only need to work with a single grading id.
	 * @param gradingId the object to be deleted, cannot be null or having a grading id of 0
	 * @throws IllegalArgumentException if gradingId is null or have a grading id value of 0
	 */
	default void deleteAnonGradingId(final OwlAnonGradingID gradingId) throws IllegalArgumentException
	{
		owlDoNotCall().deleteAnonGradingId(gradingId);
	}

	/**
	 * Deletes multiple OwlAnonGradingID objects (existing rows in the db). Prefer this method over multiple calls to deleteAnonGradingId().
	 * Null values or "null" objects in the collection are skipped. The number of successfully deleted objects is returned.
	 * @param gradingIds the set of objects to be deleted, cannot be null
	 * @return the number of objects that were successfully deleted
	 * @throws IllegalArgumentException if gradingIds is null
	 */
	default int deleteAnonGradingIds(final Set<OwlAnonGradingID> gradingIds) throws IllegalArgumentException
	{
		return owlDoNotCall().deleteAnonGradingIds(gradingIds);
	}

	/* End Owl anonymous grading methods */


	/* Begin Final Grades Report Job methods */

	default FGChanges getFinalGradeChanges(String siteId, String sectionId)
	{
		return owlDoNotCall().getFinalGradeChanges(siteId, sectionId);
	}

	/* End Final Grades Report Job methods */
}
