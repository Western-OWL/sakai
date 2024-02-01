package org.sakaiproject.sitemanage.impl.owl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.sitemanage.api.owl.OwlMigrationService;
import org.sakaiproject.sitemanage.api.owl.SiteMigrationItem;
import org.sakaiproject.tool.api.SessionManager;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OwlMigrationServiceImpl implements OwlMigrationService {

	@Setter
	protected AuthzGroupService authzGroupService;
	@Setter
	protected ContentHostingService contentHostingService;
	@Setter
	protected CourseManagementService courseManagementService;
	@Setter
	protected SessionManager sessionManager;
	@Setter
	protected SiteService siteService;

	private OwlMigrationDelegate migrationDelegate;

	public void init() {
		log.info("Initializing OwlMigrationServiceImpl");

		migrationDelegate = new OwlMigrationDelegate(authzGroupService, contentHostingService, courseManagementService, sessionManager, siteService);
	}

	@Override
	public boolean isMigrationTabEnabled() {
		return OwlMigrationDAO.isMigrationEnabled();
	}

	@Override
	public List<String> getCommonTerms() {
		// TODO: OWL_MIG_TERM_GROUPINGS maps common terms to actual terms, so the keys are sufficient for ordering
		// TODO: mocked for UI development
		return java.util.Arrays.asList("Fall / Winter 2024", "Summer 2024", "Fall / Winter 2023", "Summer 2023");
	}

	@Override
	public Map<String, List<SiteMigrationItem>> getSiteMigrationItems() {
		if (!isMigrationTabEnabled()) {
			return Collections.emptyMap();
		}
		return migrationDelegate.getSiteMigrationItems();
	}


	@Override
	public Map<String, String> getMigrationOptions() {
		return OwlMigrationDAO.getMigrationSelectionOptions();
	}

	@Override
	public void saveSelection(String siteId, String selectionKey) {
		// TODO: validate before saving
		log.info("saveSelection invoked {}, {}", siteId, selectionKey);
	}

	@Override
	public String getStatusDisplay(String selectionKey, String statusKey) {
		// TODO: mocked for UI development
		return Math.random() > 0.5 ? "(" + selectionKey + " - display)" : "";
	}

	@Override
	public String getUIMessage(String messageKey) {
		// TODO: mocked for UI development
		return "(" + messageKey + " - display)";
	}

	/* TODO: private methods to implement:
	 * migrationStatusMapping
	 * selectionsWithVisibleStatuses
	 * visibleStatuses
	 * eligibleTerms
	 * academicSessionCommonTermMap
	 * siteAgeCutoffDate
	 *
	 * migrationTabEnabled could be made private here too
	 */

}
