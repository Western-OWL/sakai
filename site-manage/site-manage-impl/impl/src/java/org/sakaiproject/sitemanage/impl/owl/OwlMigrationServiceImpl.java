package org.sakaiproject.sitemanage.impl.owl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.event.api.EventTrackingService;
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
	protected EventTrackingService eventTrackingService;
	@Setter
	protected SessionManager sessionManager;
	@Setter
	protected SiteService siteService;

	private OwlMigrationDelegate migrationDelegate;

	public void init() {
		log.info("Initializing OwlMigrationServiceImpl");

		migrationDelegate = new OwlMigrationDelegate(authzGroupService, contentHostingService, courseManagementService, eventTrackingService, sessionManager, siteService);
	}

	@Override
	public boolean isMigrationTabEnabled() {
		return OwlMigrationDAO.isMigrationEnabled();
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
	public List<String> saveSelections(Map<String, String>  siteSelections) {
		return migrationDelegate.saveSelections(siteSelections);
	}

	@Override
	public List<String> getSelectionKeysWithResourcesSizeWarnings() {
		return OwlMigrationDAO.getSelectionsWithSizeChecks();
	}

	@Override
	public String getStatusDisplay(String selectionKey, String statusKey) {
		return migrationDelegate.getStatusDisplay(selectionKey, statusKey);
	}

	@Override
	public Optional<String> getAdminDisplayName() {
		return OwlMigrationDAO.getAdminDisplayName();
	}

	@Override
	public String getUIMessage(String messageKey) {
		return OwlMigrationDAO.getUiMessage(messageKey).orElse("");
	}

}
