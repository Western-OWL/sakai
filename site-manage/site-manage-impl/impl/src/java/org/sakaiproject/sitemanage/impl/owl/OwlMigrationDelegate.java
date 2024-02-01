package org.sakaiproject.sitemanage.impl.owl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.sakaiproject.tool.api.SessionManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OwlMigrationDelegate {

	private AuthzGroupService authzGroupService;
	private ContentHostingService contentHostingService;
	private CourseManagementService courseManagementService;
	private SessionManager sessionManager;
	private SiteService siteService;

	public OwlMigrationDelegate(AuthzGroupService authzGroupService, ContentHostingService contentHostingService, 
			CourseManagementService courseManagementService, SessionManager sessionManager,
			SiteService siteService) {
		this.authzGroupService = authzGroupService;
		this.contentHostingService = contentHostingService;
		this.courseManagementService = courseManagementService;
		this.sessionManager = sessionManager;
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

		// Start as a HashMap while iterating user's sites, can create a LinkedHashMap at the end of the method to reorder items
		// E.g. iterate over common terms, and do orderedSiteMigrationItems.put(commonTerm, siteMigrationItems.get(commonTerm))
		Map<String, List<SiteMigrationItem>> siteMigrationItems = new HashMap<>();

		List<String> eligibleTerms = OwlMigrationDAO.getEligibleTermsForMigration();

		// Get all of the user's sites - exclude descriptions, include unpublished sites
		List<Site> sites = siteService.getUserSites(false, true);

		// Add project sites
		if (eligibleTerms.stream().anyMatch("project"::equalsIgnoreCase)) {
			sites.stream().filter(site -> "project".equals(site.getType())).forEach(site -> {
				// TODO: also filter the project site cutoff date if set
				appendToMap(siteMigrationItems, "project", buildSiteMigrationItem(site));
			});
		}

		// Get instructor sections
		String userEid = getCurrentUserEid();
		final Set<String> instructingSectionEids = courseManagementService.findSectionRoles(userEid).entrySet()
			.stream().filter(entry -> "I".equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toSet());

		if (instructingSectionEids.isEmpty()) {
			// Not an instructor in any sections; return early
			return siteMigrationItems;
		}

		Map<String, List<String>> groupTermMap = OwlMigrationDAO.getTermGroupingMap();
		Map<String, String> termCodeGroupMap = invertKeyListMap(groupTermMap);

		// Append course sites to map of common terms -> List<SiteMigrationItem>
		for (Site site : sites) {
			if (!"course".equals(site.getType())) {
				continue;
			}

			Set<String> sectionEids = getProvidersForCourseSite(site);
			Optional<String> firstInstructingSectionEidInSite = sectionEids.stream()
				.filter(sectionEid -> instructingSectionEids.contains(sectionEid)).findFirst();
			if (!firstInstructingSectionEidInSite.isPresent()) {
				// User is not an instructor in any of this site's sections
				continue;
			}

			Optional<String> optAcademicSessionEid = getAcademicSessionEidForSectionEid(firstInstructingSectionEidInSite.get());
			if (!optAcademicSessionEid.isPresent()) {
				log.error("An instructor's section's corresponding academic session was not identified. Instructor {}, sectionEid {}", userEid, firstInstructingSectionEidInSite.get());
				continue;
			}

			final String academicSessionEid = optAcademicSessionEid.get();
			if (!eligibleTerms.contains(academicSessionEid)) {
				continue;
			}

			// TODO: if course site creation threshold set, check site creation date; continue if site is too old

			/*
			 * Get the common term for the academic session:
			 * Stream entries mapping termCodes (E.g. "1229") to common terms (E.g. "Fall / Winter 2022")
			 * Filter entries such that the site's academicSession (E.g. "UWOUGRD1229") contains the term code ("1229")
			 * Map to the term code's corresponding common term (E.g. "Fall / Winter 2022")
			 */
			termCodeGroupMap.entrySet().stream()
				.filter(termCodeGroupEntry -> academicSessionEid.contains(termCodeGroupEntry.getKey()))
				.map(Map.Entry::getValue).findFirst().ifPresent(commonTerm -> 
					appendToMap(siteMigrationItems, commonTerm, buildSiteMigrationItem(site))
				);
		}


		// Now sort everything
		Map<String, List<SiteMigrationItem>> orderedSMIs = new LinkedHashMap<>();

		// TODO: should we sort on the term code? Can be accomplished by adding a property to siteMigrationItem
		final Comparator<SiteMigrationItem> smiTitleComparator = (smi1, smi2) -> smi1.siteTitle.compareTo(smi2.siteTitle);
		final Comparator<SiteMigrationItem> smiComparator = smiTitleComparator.thenComparing((smi1, smi2) -> smi1.siteId.compareTo(smi2.siteId));

		groupTermMap.keySet().stream()
			.filter(siteMigrationItems::containsKey).forEach(commonTerm -> {
				List<SiteMigrationItem> siteMigrationItemList = siteMigrationItems.get(commonTerm);
				siteMigrationItemList.sort(smiComparator);
				orderedSMIs.put(commonTerm, siteMigrationItemList);
		});

		return orderedSMIs;
	}

	public SiteMigrationItem buildSiteMigrationItem(Site site) {
		SiteMigrationItem item = new SiteMigrationItem();
		item.siteId = site.getId();
		item.siteTitle = site.getTitle();
		// TODO: lots more!
		return item;
	}

	private String getCurrentUserEid() {
		return sessionManager.getCurrentSession().getUserEid();
	}

	private Set<String> getProvidersForCourseSite(Site site) {
		return authzGroupService.getProviderIds(site.getReference());
	}

	private Optional<String> getAcademicSessionEidForSectionEid(String sectionEid) {
		/*
		 * Note: there is a 'term' site property used to group sites in portal; it's pretty reliable, but not perfect - some recent courses slip through the cracks.
		 * May be an approach to consider to improve performance in the 'common case', and then fall back to CM ascension if the term property is blank.
		 */
		Section section = courseManagementService.getSection(sectionEid);
		if (section == null) {
			return Optional.empty();
		}
		CourseOffering offering = courseManagementService.getCourseOffering(section.getCourseOfferingEid());
		if (offering == null) {
			return Optional.empty();
		}
		AcademicSession session = offering.getAcademicSession();
		if (session == null || StringUtils.isBlank(session.getEid())) {
			return Optional.empty();
		}
		return Optional.of(session.getEid());
	}

	private static <A, B> void appendToMap(Map<A, List<B>> map, A key, B value) {
		List<B> values = map.get(key);
		if (values == null) {
			values = new ArrayList<>();
			map.put(key, values);
		}
		values.add(value);
	}

	/**
	 * Inverts a Map whose value is a List
	 * E.g. given {A : [1, 2], B : [3, 4]},
	 * Return {1 : A, 2 : A, 3 : B, 4 : B}
	 */
	private static <A, B> Map<B, A> invertKeyListMap(Map<A, List<B>> toInvert) {
		Map<B, A> inverted = new HashMap<>();
		for (Map.Entry<A, List<B>> entry : toInvert.entrySet()) {
			for (B valueItem : entry.getValue()) {
				inverted.put(valueItem, entry.getKey());
			}
		}
		return inverted;
	}
}
