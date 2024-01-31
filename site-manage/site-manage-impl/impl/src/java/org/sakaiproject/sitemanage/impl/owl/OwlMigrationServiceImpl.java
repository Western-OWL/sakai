package org.sakaiproject.sitemanage.impl.owl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.sitemanage.api.owl.MigrationOption;
import org.sakaiproject.sitemanage.api.owl.OwlMigrationService;
import org.sakaiproject.sitemanage.api.owl.SiteMigrationItem;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OwlMigrationServiceImpl implements OwlMigrationService {

	@Setter
	protected AuthzGroupService authzGroupService;
	@Setter
	protected ContentHostingService contentHostingService;
	@Setter
	protected SiteService siteService;
	@Setter
	protected CourseManagementService courseManagementService;

	private OwlMigrationDelegate migrationDelegate;

	public void init() {
		log.info("Initializing OwlMigrationServiceImpl");

		migrationDelegate = new OwlMigrationDelegate(authzGroupService, contentHostingService, courseManagementService, siteService);
	}

	@Override
	public boolean isMigrationTabEnabled() {
		return OwlMigrationDAO.isMigrationEnabled();
	}

	@Override
	public List<String> getCommonTerms() {
		// TODO: OWL_MIG_TERM_GROUPINGS maps common terms to actual terms, so the keys are sufficient for ordering
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, List<SiteMigrationItem>> getSiteMigrationItems() {
		if (!isMigrationTabEnabled()) {
			return Collections.emptyMap();
		}
		return migrationDelegate.getSiteMigrationItems();
	}


	@Override
	public List<MigrationOption> getMigrationOptions() {
		Map<String, String> migOptions = OwlMigrationDAO.getMigrationSelectionOptions();
		throw new UnsupportedOperationException();
	}

	@Override
	public void saveSelection(String siteId, String selectionKey) {
		// TODO: validate before saving
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> getStatusDisplay(String selectionKey, String statusKey) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getUIMessage(String messageKey) {
		throw new UnsupportedOperationException();
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
