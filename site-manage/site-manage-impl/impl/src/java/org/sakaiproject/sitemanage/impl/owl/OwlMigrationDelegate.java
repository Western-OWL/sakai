package org.sakaiproject.sitemanage.impl.owl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.content.api.ContentCollection;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.coursemanagement.api.AcademicSession;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.coursemanagement.api.CourseOffering;
import org.sakaiproject.coursemanagement.api.CourseSet;
import org.sakaiproject.coursemanagement.api.Section;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.sitemanage.api.owl.SiteMigrationItem;
import org.sakaiproject.sitemanage.api.owl.SiteMigrationItem.ResourcesSizeCategory;
import org.sakaiproject.tool.api.SessionManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OwlMigrationDelegate {

	private AuthzGroupService authzGroupService;
	private ContentHostingService contentHostingService;
	private CourseManagementService courseManagementService;
	private SessionManager sessionManager;
	private SiteService siteService;

	final Comparator<SiteMigrationItem> SMI_COMPARATOR = Comparator.comparing(SiteMigrationItem::getSiteTitle)
		.thenComparing(SiteMigrationItem::getSiteId);

	public OwlMigrationDelegate(AuthzGroupService authzGroupService, ContentHostingService contentHostingService, 
			CourseManagementService courseManagementService, SessionManager sessionManager,
			SiteService siteService) {
		this.authzGroupService = authzGroupService;
		this.contentHostingService = contentHostingService;
		this.courseManagementService = courseManagementService;
		this.sessionManager = sessionManager;
		this.siteService = siteService;
	}

	public Map<String, List<SiteMigrationItem>> getSiteMigrationItems() {
		// Start as a HashMap while iterating user's sites, will create a LinkedHashMap at the end of the method to reorder items
		Map<String, List<SiteMigrationItem>> siteMigrationItems = new HashMap<>();

		Optional<GroupIdentificationParameters> optGip = buildGroupIdentificationParameters();
		if (!optGip.isPresent()) {
			return siteMigrationItems;
		}
		GroupIdentificationParameters gip = optGip.get();

		List<Site> sites = getUserSites();

		// Construct and append siteMigrationItems to map of common terms -> List<SiteMigrationItem>
		for (Site site : sites) {
			Optional<GroupAndTerm> groupAndTerm = getGroupAndTermIfSiteEligibleForMigration(site, gip, false);
			if (groupAndTerm.isPresent()) {
				String group = groupAndTerm.get().group;
				String academicSessionEid = groupAndTerm.get().academicSessionEid;
				appendToMap(siteMigrationItems, group, buildSiteMigrationItem(site, academicSessionEid));
			}
		}

		// Re-insert siteMigrationItems into a LinkedHashMap in the order expected by the UI
		return groupSiteMigrationItems(siteMigrationItems, gip.groupTermMap);
	}

	public List<String> saveSelections(Map<String, String> siteSelections) {
		Optional<GroupIdentificationParameters> optGip = buildGroupIdentificationParameters();
		if (optGip.isEmpty()) {
			log.warn("saveSelections invoked by user {} who has no eligible sites to migrate", getCurrentUserEid());
			return new ArrayList(siteSelections.keySet());
		}
		GroupIdentificationParameters gip = optGip.get();

		List<String> changeableSelections = OwlMigrationDAO.getChangeableSelections();
		Map<String, String> selectionStatusMap = OwlMigrationDAO.getMigrationInitialStatusMap();

		List<Site> userSites = getUserSites();

		List<String> failedSiteIds = new ArrayList<>();

		for (Map.Entry<String, String> siteSelection : siteSelections.entrySet()) {
			String siteId = siteSelection.getKey();
			String selectionKey = siteSelection.getValue();

			Optional<Site> site = userSites.stream().filter(userSite -> StringUtils.equals(userSite.getId(), siteId)).findFirst();

			// Validate that the site is eligible for migration
			if (!site.isPresent() || !getGroupAndTermIfSiteEligibleForMigration(site.get(), gip, true).isPresent()) {
				failedSiteIds.add(siteId);
				continue;
			}

			String statusKey = selectionStatusMap.get(selectionKey);

			Optional<SiteMigrationItemDTO> optDto = OwlMigrationDAO.getSiteMigrationItem(siteId);
			SiteMigrationItemDTO dto;

			Date now = new Date();

			if (optDto.isPresent()) {
				dto = optDto.get();

				// Update only if the selection is changeable
				if (changeableSelections.contains(dto.getSelectionKey())) {
					if (!dto.getSelectionKey().equals(selectionKey)) {
						// User tried to change their unchangeable selection
						log.warn("User {} tried to change the selection for site {}, but its existing selection '{}' is unchangeable", getCurrentUserEid(), siteId, dto.getSelectionKey());
						failedSiteIds.add(siteId);
					}
					continue;
				}

				// Set the selection
				dto.setSelectionKey(selectionKey);
				dto.setSelectionModifiedDate(now);
				dto.setSelectionModifiedEid(gip.userEid);

				// Set the status if applicable
				if (statusKey != null) {
					dto.setStatusKey(statusKey);
					dto.setStatusModifiedDate(now);
					dto.setStatusModifiedEid(gip.userEid);
				}
			} else {
				// SiteMigrationItemDTO couldn't be retrieved; try creating one
				String statusModifiedEid = statusKey == null ? null : gip.userEid;
				Date statusModifiedDate = statusKey == null ? null : now;
				dto = new SiteMigrationItemDTO(siteId, selectionKey, gip.userEid, statusKey, statusModifiedEid, now, statusModifiedDate);
			}

			boolean selectionPersisted = OwlMigrationDAO.saveSiteMigrationItem(dto);
			if (!selectionPersisted) {
				failedSiteIds.add(siteId);
			}
		}

		return failedSiteIds;
	}

	/**
	 * Gets all the information necessary to identify whether site is eligible for migration, and which group it belongs to
	 * Returns empty optional if we can immediately identify that the user cannot migrate any sites
	 */
	private Optional<GroupIdentificationParameters> buildGroupIdentificationParameters() {
		// Try to return early for non-instructors ASAP:
		// Get instructor sections. Users with no instructor roles can skip all course site processing
		String userEid = getCurrentUserEid();
		final Set<String> instructingSectionEids = getInstructingSectionEids(userEid);
		boolean skipCourses = instructingSectionEids.isEmpty();

		List<String> eligibleTerms = OwlMigrationDAO.getEligibleTermsForMigration();
		boolean projectSitesEligible = eligibleTerms.stream().anyMatch("project"::equalsIgnoreCase);
		if (!projectSitesEligible && skipCourses) {
			return Optional.empty();
		}

		Map<String, List<String>> groupTermMap = OwlMigrationDAO.getTermGroupingMap();

		// Ensure we have a UI grouping for project sites if they're eligible.
		String projectGroup = groupTermMap.keySet().stream().filter(group -> StringUtils.containsIgnoreCase(group, "project")).findFirst().orElse("Project Sites");
		if (projectSitesEligible && !groupTermMap.containsKey(projectGroup)) {
			// Project sites are eligible, but they are not placed in the term groupings (authoritative source for UI ordering). Add "Project Sites" to the end.
			groupTermMap.put(projectGroup, Collections.emptyList());
		}

		GroupIdentificationParameters gip = new GroupIdentificationParameters();
		gip.userEid = userEid;
		gip.instructingSectionEids = instructingSectionEids;
		gip.skipCourses = skipCourses;
		gip.eligibleTerms = eligibleTerms;
		gip.projectSitesEligible = projectSitesEligible;
		gip.groupTermMap = groupTermMap;
		gip.projectGroup = projectGroup;
		gip.termCodeGroupMap = invertKeyListMap(groupTermMap);

		return Optional.of(gip);
	}

	private Optional<GroupAndTerm> getGroupAndTermIfSiteEligibleForMigration(Site site, GroupIdentificationParameters params, boolean logWhenIneligible) {

		String userEid = params.userEid;
		Set<String> instructingSectionEids = params.instructingSectionEids;
		boolean skipCourses = params.skipCourses;
		List<String> eligibleTerms = params.eligibleTerms;
		boolean projectSitesEligible = params.projectSitesEligible;
		Map<String, List<String>> groupTermMap = params.groupTermMap;
		String projectGroup = params.projectGroup;
		Map<String, String> termCodeGroupMap = params.termCodeGroupMap;

		if (projectSitesEligible && "project".equals(site.getType()) &&
				!cutoffDateApplies(OwlMigrationDAO.getProjectSiteCutoffDate(), site)
				&& site.getUserRole(getCurrentUserId()).getId().equals(site.getMaintainRole())) {
			return Optional.of(new GroupAndTerm(projectGroup));
		}

		if (skipCourses || !"course".equals(site.getType())) {
			if (logWhenIneligible) {
				log.warn("Site {} is not eligible for migration: it doesn't meet project site criteria, or it's a course site and the user has no instructor enrollments. User: {}", site.getId(), userEid);
			}
			return Optional.empty();
		}

		if (cutoffDateApplies(OwlMigrationDAO.getCourseSiteCutoffDate(), site)) {
			if (logWhenIneligible) {
				log.warn("Site {} is not eligible for migration: creation date precedes the course site cutoff date. User: {}", site.getId(), userEid);
			}
			return Optional.empty();
		}

		Set<String> sectionEids = getProvidersForCourseSite(site);
		Optional<String> firstInstructingSectionEidInSite = sectionEids.stream()
			.filter(sectionEid -> instructingSectionEids.contains(sectionEid)).findFirst();
		if (!firstInstructingSectionEidInSite.isPresent()) {
			// User is not an instructor in any of this site's sections
			if (logWhenIneligible) {
				log.warn("Site {} is not eligible for migration: it is a course site in which the user has no instructor enrollments. User: {}", site.getId(), userEid);
			}
			return Optional.empty();
		}

		Optional<String> optAcademicSessionEid = getAcademicSessionEidForSectionEid(firstInstructingSectionEidInSite.get());
		if (!optAcademicSessionEid.isPresent()) {
			log.error("An instructor's section's corresponding academic session was not identified. Instructor {}, sectionEid {}", userEid, firstInstructingSectionEidInSite.get());
			return Optional.empty();
		}

		final String academicSessionEid = optAcademicSessionEid.get();
		if (!eligibleTerms.contains(academicSessionEid)) {
			if (logWhenIneligible) {
				log.warn("Site {} is not eligible for migration: its corresponding academic session {} is not in the list of eligible terms. User: {}", site.getId(), academicSessionEid, userEid);
			}
			return Optional.empty();
		}

		/*
		 * Get the common term for the academic session:
		 * Stream entries mapping termCodes (E.g. "1229") to common terms (E.g. "Fall / Winter 2022")
		 * Filter entries such that the site's academicSession (E.g. "UWOUGRD1229") contains the term code ("1229")
		 * Map to the term code's corresponding common term (E.g. "Fall / Winter 2022")
		 */
		Optional<String> group = termCodeGroupMap.entrySet().stream()
			.filter(termCodeGroupEntry -> academicSessionEid.contains(termCodeGroupEntry.getKey()))
			.map(Map.Entry::getValue).findFirst();

		if (!group.isPresent()) {
			log.error("Site {} is eligible, but its academic session {} does not have a corresponding group. Please review OWL_MIG_TERM_GROUPINGS", site.getId(), academicSessionEid);
		}

		return Optional.of(new GroupAndTerm(group.get(), academicSessionEid));
	}

	public String getStatusDisplay(String selectionKey, String statusKey) {
		if (OwlMigrationDAO.getSelectionsWithVisibleStatuses().contains(selectionKey) && 
			OwlMigrationDAO.getVisibleStatuses().contains(statusKey)) {
			return StringUtils.trimToEmpty(OwlMigrationDAO.getMigrationStatusOptions().get(statusKey));
		}
		return "";
	}

	private Map<String, List<SiteMigrationItem>> groupSiteMigrationItems(Map<String, List<SiteMigrationItem>> siteMigrationItems, Map<String, List<String>> groupTermMap) {
		Map<String, List<SiteMigrationItem>> groupedSMIs = new LinkedHashMap<>();

		final List<String> eligibleTerms = OwlMigrationDAO.getEligibleTermsForMigration();

		groupTermMap.keySet().stream()
			.filter(siteMigrationItems::containsKey).forEach(group -> {
				List<SiteMigrationItem> siteMigrationItemList = siteMigrationItems.get(group);

				// Order the sites within the group by term, with terms ordered as included in eligibleTermsForMigration.
				// Safe for project sites: indexOf returns -1 (all are equal)
				Comparator<SiteMigrationItem> orderedMatchingEligibleTerms = (SiteMigrationItem s1, SiteMigrationItem s2) ->
					Integer.compare(eligibleTerms.indexOf(s1.getAcademicSessionEid()), eligibleTerms.indexOf(s2.getAcademicSessionEid()));

				siteMigrationItemList.sort(orderedMatchingEligibleTerms.thenComparing(SMI_COMPARATOR));
				groupedSMIs.put(group, siteMigrationItemList);
		});

		return groupedSMIs;
	}

	private SiteMigrationItem buildSiteMigrationItem(Site site) {
		return buildSiteMigrationItem(site, "");
	}

	private SiteMigrationItem buildSiteMigrationItem(Site site, String academicSessionEid) {
		SiteMigrationItem item = new SiteMigrationItem();

		populateSiteDetails(item, site);
		populateResourcesDetails(item, site);
		populateMigrationProperties(item, site);

		item.setAcademicSessionEid(academicSessionEid);

		return item;
	}

	private void populateSiteDetails(SiteMigrationItem item, Site site) {
		item.setSiteId(site.getId());
		item.setSiteTitle(site.getTitle());
		item.setSiteUrl(site.getUrl());
	}

	private void populateResourcesDetails(SiteMigrationItem item, Site site) {
		if (OwlMigrationDAO.getSelectionsWithSizeChecks().isEmpty()) {
			// Resource sizes don't matter; return early for performance
			item.setResourcesSize("");
			item.setResourcesSizeCategory(ResourcesSizeCategory.NONE);
			return;
		}

		float resourcesSize = getResourcesSizeInGb(site);
		String resourcesSizeDisplay = String.format(Locale.US, "%.1f", resourcesSize);
		item.setResourcesSize(resourcesSizeDisplay);

		Optional<Float> errorThreshold = OwlMigrationDAO.getSiteSizeErrorThreshold();
		Optional<Float> warnThreshold = OwlMigrationDAO.getSiteSizeWarningThreshold();

		ResourcesSizeCategory category = ResourcesSizeCategory.NONE;
		if (errorThreshold.isPresent() && resourcesSize > errorThreshold.get()) {
			category = ResourcesSizeCategory.BRIGHTSPACE_LIMIT_EXCEEDED;
		} else if (warnThreshold.isPresent() && resourcesSize > warnThreshold.get()) {
			category = ResourcesSizeCategory.WARN;
		}
		item.setResourcesSizeCategory(category);
	}

	private void populateMigrationProperties(SiteMigrationItem item, Site site) {
		Optional<SiteMigrationItemDTO> optDto = OwlMigrationDAO.getSiteMigrationItem(site.getId());

		if (optDto.isPresent()) {
			SiteMigrationItemDTO dto = optDto.get();

			String selectionKey = StringUtils.defaultIfBlank(dto.getSelectionKey(), "undecided");
			boolean isSelectionEditable = OwlMigrationDAO.getChangeableSelections().contains(selectionKey);
			Optional<String> selectionModifiedEid = Optional.ofNullable(StringUtils.trimToNull(dto.getSelectionModifiedEid()));
			Optional<Date> selectionModifiedDate = Optional.ofNullable(dto.getSelectionModifiedDate());
			String statusKey = dto.getStatusKey();
			Optional<String> statusModifiedEid = Optional.ofNullable(StringUtils.trimToNull(dto.getStatusModifiedEid()));
			Optional<Date> statusModifiedDate = Optional.ofNullable(dto.getStatusModifiedDate());

			item.setSelectionKey(selectionKey);
			item.setSelectionEditable(isSelectionEditable);
			item.setSelectionModifiedEid(selectionModifiedEid);
			item.setSelectionModifiedDate(selectionModifiedDate);
			item.setStatusKey(statusKey);
			item.setStatusModifiedEid(statusModifiedEid);
			item.setStatusModifiedDate(statusModifiedDate);
		} else {
			log.warn("Failed to get a SiteMigrationItemDTO; using defaults for site {}" + site.getId());
			item.setSelectionKey("undecided");
			item.setSelectionEditable(true);
			item.setSelectionModifiedEid(Optional.empty());
			item.setSelectionModifiedDate(Optional.empty());
			item.setStatusKey("");
			item.setStatusModifiedEid(Optional.empty());
			item.setStatusModifiedDate(Optional.empty());
		}
	}

	private boolean cutoffDateApplies(Optional<LocalDate> cutoff, Site site) {
		if (!cutoff.isPresent()) {
			return false;
		}
		return site.getCreatedDate().toInstant().isBefore(toInstant(cutoff.get()));
	}

	private Set<String> getInstructingSectionEids(String userEid) {
		return courseManagementService.findSectionRoles(userEid).entrySet()
			.stream().filter(entry -> "I".equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toSet());
	}

	private List<Site> getUserSites() {
		// Get all of the user's sites - exclude descriptions, include unpublished sites
		return siteService.getUserSites(false, true);
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

	private float getResourcesSizeInGb(Site site) {
		String siteCollectionId = contentHostingService.getSiteCollection(site.getId());
		try {
			ContentCollection collection = contentHostingService.getCollection(siteCollectionId);
			float kb = (float)collection.getBodySizeK();
			return (float)(kb * Math.pow(1024, -2));
		} catch (IdUnusedException e) {
			// Ignore - this is common if the site doesn't have a resources tool
		} catch (TypeException | PermissionException e) {
			log.warn("Exception accessing the content collection for site {}; collectionId: {} ", site.getId(), siteCollectionId, e);
		}
		return 0f;
	}

	private String getCurrentUserEid() {
		return sessionManager.getCurrentSession().getUserEid();
	}

	private String getCurrentUserId() {
		return sessionManager.getCurrentSessionUserId();
	}

	private Instant toInstant(LocalDate localDate) {
		return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
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

	private class GroupIdentificationParameters {
		String userEid;
		Set<String> instructingSectionEids;
		boolean skipCourses;
		List<String> eligibleTerms;
		boolean projectSitesEligible;
		Map<String, List<String>> groupTermMap;
		String projectGroup;
		Map<String, String> termCodeGroupMap;
	}

	private class GroupAndTerm {
		String group;
		String academicSessionEid;

		public GroupAndTerm(String group, String academicSessionEid) {
			this.group = group;
			this.academicSessionEid = academicSessionEid;
		}

		public GroupAndTerm(String group) {
			this.group = group;
			this.academicSessionEid = "";
		}
	}
}
