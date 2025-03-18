package org.sakaiproject.sitemanage.impl.owl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.content.api.ContentCollection;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.sitemanage.api.owl.SiteMigrationItem;
import org.sakaiproject.sitemanage.api.owl.SiteMigrationItem.ResourcesSizeCategory;
import org.sakaiproject.tool.api.SessionManager;

import lombok.extern.slf4j.Slf4j;
import org.sakaiproject.sitemanage.api.owl.MigAction;
import org.sakaiproject.sitemanage.api.owl.OwlMigrationService;
import org.sakaiproject.sitemanage.api.owl.UserSelection;

@Slf4j
public class OwlMigrationDelegate {

	// OWLTODO: remove services that aren't used, remove from components.xml also
	private AuthzGroupService authzGroupService;
	private ContentHostingService contentHostingService;
	private CourseManagementService courseManagementService;
	private EmailService emailService;
	private EventTrackingService eventTrackingService;
	private ServerConfigurationService serverConfigurationService;
	private SessionManager sessionManager;
	private SiteService siteService;

	// OWLTODO: remove this if it is actually unused
//	final Comparator<SiteMigrationItem> smiComparator = Comparator.comparing(SiteMigrationItem::getSiteTitle)
//		.thenComparing(SiteMigrationItem::getSiteId);

	public OwlMigrationDelegate(AuthzGroupService ags, ContentHostingService chs, CourseManagementService cms, EmailService es, EventTrackingService ets,
			ServerConfigurationService scs, SessionManager sm, SiteService ss) {
		authzGroupService = ags;
		contentHostingService = chs;
		courseManagementService = cms;
		emailService = es;
		eventTrackingService = ets;
		serverConfigurationService = scs;
		sessionManager = sm;
		siteService = ss;
	}

	public List<SiteMigrationItem> getSiteMigrationItems() {
		List<SiteMigrationItem> siteMigItems = new ArrayList<>();
		List<Site> sites = getUserMaintainerSites(getCurrentUserId());
		for (Site site : sites) {
			if ("project".equals(site.getType())) {
				siteMigItems.add(buildSiteMigrationItem(site));
			}
		}

		return siteMigItems;
	}

	public Map<String, String> getActiveTypes() {
		final Map<String, String> typeOptions = OwlMigrationDAO.getTypeOptions();
		List<String> activeTypeOptions = OwlMigrationDAO.getActiveTypeKeys();

		if (activeTypeOptions.stream().anyMatch(typeKey -> !typeOptions.containsKey(typeKey))) {
			logAndSendMisconfigurationEmail("OWL_MIG_ACTIVE_TYPE_KEYS contains items that are not keys in OWL_MIG_TYPE_MAP. Until this is resolved, the migration tab will be in read-only mode.");
			return Collections.emptyMap();
		}

		return activeTypeOptions.stream()
			.collect(Collectors.toMap(key -> key, typeOptions::get, (v1, v2) -> v2, LinkedHashMap::new));
	}

	public Map<String, String> getActiveActions() {
		final Map<String, String> actionOptions = OwlMigrationDAO.getActionOptions();
		List<String> activeActionOptions = OwlMigrationDAO.getActiveActionKeys();

		if (activeActionOptions.stream().anyMatch(actionKey -> !actionOptions.containsKey(actionKey))) {
			logAndSendMisconfigurationEmail("OWL_MIG_ACTIVE_ACTION_KEYS contains items that are not keys in OWL_MIG_ACTIONS_MAP. Until this is resolved, the migration tab will be in read-only mode.");
			return Collections.emptyMap();
		}

		return activeActionOptions.stream()
			.collect(Collectors.toMap(key -> key, actionOptions::get, (v1, v2) -> v2, LinkedHashMap::new));
	}

	public Map<String, List<MigAction>> getTypeActionMap() {
		List<String> activeTypes = OwlMigrationDAO.getActiveTypeKeys();
		List<String> activeActions = OwlMigrationDAO.getActiveActionKeys();
		Map<String, String> actionOptions = OwlMigrationDAO.getActionOptions();
		Map<String, List<String>> typeActionMap = OwlMigrationDAO.getTypesToActionsMap();

		Map<String, List<MigAction>> map = new HashMap<>();
		for (Entry<String, List<String>> entry : typeActionMap.entrySet()) {
			String typeKey = entry.getKey();
			List<String> actionKeys = entry.getValue();
			if (activeTypes.contains(typeKey)) {
				for (String actionKey : actionKeys) {
					if (activeActions.contains(actionKey)) {
						List<MigAction> actions = map.computeIfAbsent(typeKey, key -> new ArrayList<MigAction>());
						actions.add(new MigAction(actionKey, actionOptions.get(actionKey)));
					}
				}
			}
		}

		return map;
	}

	public List<String> saveSelections(Map<String, UserSelection> siteSelections) {

		List<String> changeableTypes = OwlMigrationDAO.getChangeableTypes();
		List<String> activeTypes = OwlMigrationDAO.getActiveTypeKeys();
		List<String> changeableActions = OwlMigrationDAO.getChangeableActions();
		List<String> activeActions = OwlMigrationDAO.getActiveActionKeys();
		Map<String, String> actionStatusMap = OwlMigrationDAO.getInitialActionStatusMap();
		String currentUserEid = getCurrentUserEid();
		List<Site> userSites = getUserMaintainerSites(getCurrentUserId());

		List<String> failedSiteTitles = new ArrayList<>();
		for (Entry<String, UserSelection> siteSelection : siteSelections.entrySet()) {
			String siteId = siteSelection.getKey();
			String typeKey = siteSelection.getValue().getType().orElse("");
			String actionKey = siteSelection.getValue().getAction().orElse("");

			// Validate that the site is eligible for migration
			Optional<Site> site = userSites.stream().filter(userSite -> StringUtils.equals(userSite.getId(), siteId)).findFirst();
			if (!site.isPresent()) {
				log.warn("User {} tried to change the selection(s) for site {} in which they are not a maintainer", currentUserEid, siteId);
				failedSiteTitles.add(siteId);
				continue;
			}

			Site userSite = site.get();
			String siteTitle = userSite.getTitle();

			String statusKey = StringUtils.trimToEmpty(actionStatusMap.get(actionKey));
			Optional<SiteMigrationItemDTO> optDto = OwlMigrationDAO.getSiteMigrationItem(siteId);
			SiteMigrationItemDTO dto;
			Date now = new Date();
			boolean resetAction = false;
			boolean resetStatus = false;

			if (optDto.isPresent()) {
				dto = optDto.get();

				// Check if typeKey provided is active
				boolean typeActive = activeTypes.contains(typeKey);
				if (!typeActive && !"".equals(typeKey)) {
					log.warn("User {} tried to change the type selection for site {} to a value that is not an active type: {}" , currentUserEid, siteId, typeKey);
				}

				// Check if typeKey provided is changeable
				boolean typeChangeable = StringUtils.isEmpty(dto.getTypeKey()) || changeableTypes.contains(dto.getTypeKey());
				if (!typeChangeable && !dto.getTypeKey().equals(typeKey)) {
					// User tried to change their unchangeable type
					log.warn("User {} tried to change the type selection for site {}, but its existing type '{}' is unchangeable", currentUserEid, siteId, dto.getTypeKey());
				}

				// If the typeKey is changing, and no actionKey is provided, we need to reset actionKey
				if (!StringUtils.equals(dto.getTypeKey(), typeKey) && "".equals(actionKey)) {
					resetAction = true;
				}

				// Check if actionKey provided is active
				boolean actionActive = activeActions.contains(actionKey);
				if (!actionActive && !"".equals(actionKey)) {
					log.warn("User {} tried to change the action selection for site {} to a value that is not an active action: {}" , currentUserEid, siteId, actionKey);
				}

				// Check if actionKey provided is changeable
				boolean actionChangeable = StringUtils.isEmpty(dto.getActionKey()) || changeableActions.contains(dto.getActionKey());
				if (!actionChangeable && !dto.getActionKey().equals(actionKey)) {
					// User tried to change their unchangeable action
					log.warn("User {} tried to change the action selection for site {}, but its existing action '{}' is unchangeable", currentUserEid, siteId, dto.getActionKey());
				}

				// If the action is changing we need to update statusKey, whether the statusKey is empty or not
				if (!StringUtils.equals(dto.getActionKey(), actionKey)) {
					resetStatus = true;
				}

				// If there's nothing to save (both type and action are not active nor changeable), skip to next site
				if (!typeActive && !typeChangeable && !actionActive && !actionChangeable) {
					failedSiteTitles.add(siteTitle);
					continue;
				}

				// Set the type if applicable
				if (!"".equals(typeKey)) {
					dto.setTypeKey(typeKey);
					dto.setTypeModifiedDate(now);
					dto.setTypeModifiedEid(currentUserEid);
				}

				// Set the action if applicable
				if (!"".equals(actionKey)) {
					dto.setActionKey(actionKey);
					dto.setActionModifiedDate(now);
					dto.setActionModifiedEid(currentUserEid);
				}

				// Set the status if applicable
				if (!"".equals(statusKey) || resetStatus) {
					dto.setStatusKey(statusKey);
					dto.setStatusModifiedDate(now);
					dto.setStatusModifiedEid(currentUserEid);
				}
			} else {
				// SiteMigrationItemDTO couldn't be retrieved; try creating one
				String typeModifiedEid = "";
				Date typeModifiedDate = null;
				if (!"".equals(typeKey)) {
					typeModifiedEid = currentUserEid;
					typeModifiedDate = now;
				}

				String actionModifiedEid = "";
				Date actionModifiedDate = null;
				if (!"".equals(actionKey)) {
					actionModifiedEid = currentUserEid;
					actionModifiedDate = now;
				}

				String statusModifiedEid = "";
				Date statusModifiedDate = null;
				if (!"".equals(statusKey)) {
					statusModifiedEid = currentUserEid;
					statusModifiedDate = now;
				}

				dto = new SiteMigrationItemDTO(siteId, typeKey, typeModifiedEid, actionKey, actionModifiedEid, statusKey, statusModifiedEid, typeModifiedDate, actionModifiedDate, statusModifiedDate);
			}

			if (OwlMigrationDAO.saveSiteMigrationItem(dto, resetAction, resetStatus)) {
				String eventRef;
				if (StringUtils.isNotBlank(typeKey) && StringUtils.isNotBlank(actionKey)) {
					eventRef = siteId + "->type:" + typeKey + "&action:" + actionKey;
				} else if (StringUtils.isNotBlank(typeKey)) {
					eventRef = siteId + "->type:" + typeKey;
				} else {
					eventRef = siteId + "->action:" + actionKey;
				}
				eventTrackingService.post(eventTrackingService.newEvent(OwlMigrationService.EVENT_OWL_MIGRATION_SELECTION_SAVED, eventRef, true));
			} else {
				failedSiteTitles.add(siteTitle);
			}
		}

		return failedSiteTitles;
	}

	public String getStatusDisplay(String actionKey, String statusKey) {
		if (OwlMigrationDAO.getActionsWithVisibleStatuses().contains(actionKey) && OwlMigrationDAO.getVisibleStatuses().contains(statusKey)) {
			return StringUtils.trimToEmpty(OwlMigrationDAO.getStatusOptions().get(statusKey));
		}
		return "";
	}

	/**
	 * Takes an slf4j style parameterized message describing a misconfiguraiton issue.
	 * The message is both logged and emailed to the configured recipients.
	 */
	public void logAndSendMisconfigurationEmail(String message, String... params) {
		log.error(message, (Object[]) params);

		String sender = serverConfigurationService.getString("smtpFrom@org.sakaiproject.email.api.EmailService", "postmaster@" + serverConfigurationService.getServerName());

		Optional<String> optRecipient = OwlMigrationDAO.getSupportEmailAddress();
		if (!optRecipient.isPresent()) {
			log.error("Migration tab is misconfigured, and there is no email recipient; using owlmigrationquestions@uwo.ca");
		}

		String recipient = optRecipient.orElse("owlmigrationquestions@uwo.ca");
		String environment = serverConfigurationService.getString("ui.service", "OWL");
		String subject = environment + ": Migration tab properties are misconfigured!";
		String emailBody = String.format(message.replace("{}", "%s"), (Object[]) params);

		emailService.send(sender, recipient, subject, emailBody, null, null, null);
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

	/**
	 * The displayed resource size is rounded to the nearest decimal place.
	 * Likewise, the ResourceSizeCategory reflects the rounded value
	 * because, say, a 1.95 GB site only needs 51.2 MB outside of Resources to exceed 2.0 GB
	 */
	private void populateResourcesDetails(SiteMigrationItem item, Site site) {
		if (OwlMigrationDAO.getActionsWithSizeChecks().isEmpty()) {
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
		if (errorThreshold.isPresent() && resourcesSize + 0.05 >= errorThreshold.get()) {
			category = ResourcesSizeCategory.BRIGHTSPACE_LIMIT_EXCEEDED;
		} else if (warnThreshold.isPresent() && resourcesSize + 0.05 >= warnThreshold.get()) {
			category = ResourcesSizeCategory.WARN;
		}
		item.setResourcesSizeCategory(category);
	}

	private void populateMigrationProperties(SiteMigrationItem item, Site site) {
		Optional<SiteMigrationItemDTO> optDto = OwlMigrationDAO.getSiteMigrationItem(site.getId());

		if (optDto.isPresent()) {
			SiteMigrationItemDTO dto = optDto.get();

			String typeKey = StringUtils.defaultIfBlank(dto.getTypeKey(), "undecided");
			boolean isTypeEditable = OwlMigrationDAO.getChangeableTypes().contains(typeKey);
			Optional<String> typeModifiedEid = Optional.ofNullable(StringUtils.trimToNull(dto.getTypeModifiedEid()));
			Optional<Date> typeModifiedDate = Optional.ofNullable(dto.getTypeModifiedDate());

			String actionKey = StringUtils.defaultIfBlank(dto.getActionKey(), "");
			boolean isActionEditable = OwlMigrationDAO.getChangeableActions().contains(actionKey);
			Optional<String> actionModifiedEid = Optional.ofNullable(StringUtils.trimToNull(dto.getActionModifiedEid()));
			Optional<Date> actionModifiedDate = Optional.ofNullable(dto.getActionModifiedDate());

			String statusKey = dto.getStatusKey();
			Optional<String> statusModifiedEid = Optional.ofNullable(StringUtils.trimToNull(dto.getStatusModifiedEid()));
			Optional<Date> statusModifiedDate = Optional.ofNullable(dto.getStatusModifiedDate());

			item.setTypeKey(typeKey);
			item.setTypeEditable(isTypeEditable);
			item.setTypeModifiedEid(typeModifiedEid);
			item.setTypeModifiedDate(typeModifiedDate);
			item.setActionKey(actionKey);
			item.setActionEditable(isActionEditable);
			item.setActionModifiedEid(actionModifiedEid);
			item.setActionModifiedDate(actionModifiedDate);
			item.setStatusKey(statusKey);
			item.setStatusModifiedEid(statusModifiedEid);
			item.setStatusModifiedDate(statusModifiedDate);
		} else {
			log.warn("Failed to get a SiteMigrationItemDTO; using defaults for site {}" + site.getId());
			item.setTypeKey("undecided");
			item.setTypeEditable(true);
			item.setTypeModifiedEid(Optional.empty());
			item.setTypeModifiedDate(Optional.empty());
			item.setActionKey("");
			item.setActionEditable(true);
			item.setActionModifiedEid(Optional.empty());
			item.setActionModifiedDate(Optional.empty());
			item.setStatusKey("");
			item.setStatusModifiedEid(Optional.empty());
			item.setStatusModifiedDate(Optional.empty());
		}
	}

	/**
	 * Get all of the user's sites where they hold the "maintain" role for the site type - exclude descriptions, include unpublished sites
	 * @param userId the internal ID of the user in question
	 * @return List of user's maintainer sites
	 */
	private List<Site> getUserMaintainerSites(String userId) {
		return siteService.getUserSites(false, true).stream().filter(site -> site.hasRole(userId, site.getMaintainRole())).collect(Collectors.toList());
	}

	private float getResourcesSizeInGb(Site site) {
		String siteCollectionId = contentHostingService.getSiteCollection(site.getId());
		try {
			ContentCollection collection = contentHostingService.getCollection(siteCollectionId);
			float kb = (float) collection.getBodySizeK();
			return (float) (kb * Math.pow(1024, -2));
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

	// OWLTODO: remove this if it is actually unused
//	private Instant toInstant(LocalDate localDate) {
//		return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
//	}

	// OWLTODO: remove this if it is actually unused
//	private <A, B> void appendToMap(Map<A, List<B>> map, A key, B value) {
//		List<B> values = map.get(key);
//		if (values == null) {
//			values = new ArrayList<>();
//			map.put(key, values);
//		}
//		values.add(value);
//	}

	// OWLTODO: remove this if it is actually unused
	/**
	 * Inverts a Map whose value is a List
	 * E.g. given {A : [1, 2], B : [3, 4]},
	 * Return {1 : A, 2 : A, 3 : B, 4 : B}
	 */
//	private static <A, B> Map<B, A> invertKeyListMap(Map<A, List<B>> toInvert) {
//		Map<B, A> inverted = new HashMap<>();
//		for (Map.Entry<A, List<B>> entry : toInvert.entrySet()) {
//			for (B valueItem : entry.getValue()) {
//				inverted.put(valueItem, entry.getKey());
//			}
//		}
//		return inverted;
//	}
}
