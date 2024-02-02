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

		// Try to short circuit non-instructors ASAP:
		// Get instructor sections. Users with no instructor roles can skip all course site processing
		String userEid = getCurrentUserEid();
		final Set<String> instructingSectionEids = courseManagementService.findSectionRoles(userEid).entrySet()
			.stream().filter(entry -> "I".equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toSet());
		boolean skipCourses = instructingSectionEids.isEmpty();

		List<String> eligibleTerms = OwlMigrationDAO.getEligibleTermsForMigration();
		boolean projectSitesEligible = eligibleTerms.stream().anyMatch("project"::equalsIgnoreCase);
		if (!projectSitesEligible && skipCourses) {
			return siteMigrationItems;
		}

		Map<String, List<String>> groupTermMap = OwlMigrationDAO.getTermGroupingMap();

		// Ensure we have a UI grouping for project sites if they're eligible.
		String projectGroup = groupTermMap.keySet().stream().filter(group -> StringUtils.containsIgnoreCase(group, "project")).findFirst().orElse("Project Sites");
		if (projectSitesEligible && !groupTermMap.containsKey(projectGroup)) {
			// Project sites are eligible, but they are not placed in the term groupings (authoritative source for UI ordering). Add "Project Sites" to the end.
			groupTermMap.put(projectGroup, Collections.emptyList());
		}

		Map<String, String> termCodeGroupMap = invertKeyListMap(groupTermMap);

		// Get all of the user's sites - exclude descriptions, include unpublished sites
		List<Site> sites = siteService.getUserSites(false, true);

		// Construct and append siteMigrationItems to map of common terms -> List<SiteMigrationItem>
		for (Site site : sites) {
			if (projectSitesEligible && "project".equals(site.getType())) {
				appendToMap(siteMigrationItems, projectGroup, buildSiteMigrationItem(site));
				continue;
			}

			if (skipCourses || !"course".equals(site.getType())) {
				continue;
			}

			if (cutoffDateApplies(OwlMigrationDAO.getCourseSiteCutoffDate(), site)) {
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

		// Re-insert siteMigrationItems into a LinkedHashMap in the order expected by the UI
		return groupSiteMigrationItems(siteMigrationItems, groupTermMap);
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

		groupTermMap.keySet().stream()
			.filter(siteMigrationItems::containsKey).forEach(group -> {
				List<SiteMigrationItem> siteMigrationItemList = siteMigrationItems.get(group);
				// TODO: if group is not the projectGroup, we can sort on terms - but what order?
				// If we do, we should use sort(TERM_COMPARATOR.thenComparing(SMI_COMPARATOR))
				siteMigrationItemList.sort(SMI_COMPARATOR);
				groupedSMIs.put(group, siteMigrationItemList);
		});

		return groupedSMIs;
	}

	private SiteMigrationItem buildSiteMigrationItem(Site site) {
		SiteMigrationItem item = new SiteMigrationItem();

		populateSiteDetails(item, site);
		populateResourcesDetails(item, site);
		populateMigrationProperties(item, site);

		return item;
	}

	private void populateSiteDetails(SiteMigrationItem item, Site site) {
		item.setSiteId(site.getId());
		item.setSiteTitle(site.getTitle());
		item.setSiteUrl(site.getUrl());
	}

	private void populateResourcesDetails(SiteMigrationItem item, Site site) {
		float resourcesSize = getResourcesSizeInGb(site);
		String resourcesSizeDisplay = String.format(Locale.US, "%.1f", resourcesSize);
		item.setResourcesSize(resourcesSizeDisplay);

		float errorThreshold = OwlMigrationDAO.getSiteSizeErrorThreshold().orElse(1.5f);
		float warnThreshold = OwlMigrationDAO.getSiteSizeWarningThreshold().orElse(2f);

		ResourcesSizeCategory category = ResourcesSizeCategory.NONE;
		if (resourcesSize > errorThreshold) {
			category = ResourcesSizeCategory.BRIGHTSPACE_LIMIT_EXCEEDED;
		} else if (resourcesSize > warnThreshold) {
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
}
