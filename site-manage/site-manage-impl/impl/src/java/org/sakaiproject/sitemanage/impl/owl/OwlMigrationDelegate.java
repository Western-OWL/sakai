package org.sakaiproject.sitemanage.impl.owl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.coursemanagement.api.AcademicSession;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.coursemanagement.api.CourseOffering;
import org.sakaiproject.coursemanagement.api.CourseSet;
import org.sakaiproject.coursemanagement.api.Section;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.sitemanage.api.owl.SiteMigrationItem;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OwlMigrationDelegate {

	private AuthzGroupService authzGroupService;
	private ContentHostingService contentHostingService;
	private CourseManagementService courseManagementService;
	private SiteService siteService;

	public OwlMigrationDelegate(AuthzGroupService authzGroupService, ContentHostingService contentHostingService, 
			CourseManagementService courseManagementService, SiteService siteService) {
		this.authzGroupService = authzGroupService;
		this.contentHostingService = contentHostingService;
		this.courseManagementService = courseManagementService;
		this.siteService = siteService;
	}

	public void getResourcesSizeInKb(Site site) {
		/*
		ContentCollection collection = contentHostingService.getCollection(siteCollectionId);
		return collection.getBodySizeK();
		 */
		throw new UnsupportedOperationException();
	}

	public Map<String, List<SiteMigrationItem>> getSiteMigrationItems() {

		Map<String, List<SiteMigrationItem>> siteMigrationItems = new HashMap<>();

		// Get all of the user's sites - exclude descriptions, include unpublished sites
		List<Site> sites = siteService.getUserSites(false, true);
		for (Site site : sites) {
			if ("course".equals(site.getType())) {
				List<Section> sections = getSectionsForCourseSite(site);
				// TODO: filter only sections in which the user is enrolled as an instructor
				List<String> academicSessionEids = getAcademicSessionEidsForSections(sections);
			}
		}
		/* TODO:
		 * Get eligible academic sessions
		 * Query user's course sites that meets all of the following conditions:
		 *     1: User is enrolled as an instructor
		 *     2: The site has an 'eligibleCommonTerm'
		 *     3: Site created after course site creation cutoff date
		 * If project sites are eligible for migration:
		 *     Query user's project sites whose creation date is after the project site creation cutoff date
		 */
		throw new UnsupportedOperationException();
	}

	private List<Section> getSectionsForCourseSite(Site site) {
		List<Section> sections = new ArrayList<>();
		Set<String> providerIds = authzGroupService.getProviderIds(site.getReference());
		for (String providerId : providerIds) {
			Section section = courseManagementService.getSection(providerId);
			if (section != null) {
				sections.add(section);
			}
		}
		return sections;
	}

	private List<String> getAcademicSessionEidsForSections(List<Section> sections) {
		List<String> sessions = new ArrayList<>();
		for (Section section : sections) {
			CourseOffering offering = courseManagementService.getCourseOffering(section.getCourseOfferingEid());
			if (offering == null) {
				continue;
			}
			AcademicSession session = offering.getAcademicSession();
			if (session == null || StringUtils.isBlank(session.getEid())) {
				continue;
			}
			sessions.add(session.getEid());
		}

		return sessions;
	}

	private Optional<String> getEligibleCommonTerm(List<String> academicSessions) {
		// Assumption - no courses are crosslisted with terms in multiple common term groupings, or if there are, we don't care about their presentation
		/*
		Map<String, String> termCodesToCommonTerms = invert OWL_MIG_TERM_GROUPINGS
		for (String session : academicSessions) {
			if (!eligibleTermCodes.contains(session) {
				continue;
			}

			for (termCode : termCodesToCommonTerms.keySet()) {
				// E.g. term code contains "1225":
				if (session.contains(termCode)) {
					return termCodesToCommonTerms.get(termCode);
				}
			}
		}
		return Optional.empty();
		*/
		throw new UnsupportedOperationException();
	}
}
