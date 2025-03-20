package org.sakaiproject.sitemanage.impl.owl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.sitemanage.api.owl.OwlMigrationService;
import org.sakaiproject.sitemanage.api.owl.SiteMigrationItem;
import org.sakaiproject.tool.api.SessionManager;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.sakaiproject.sitemanage.api.owl.MigAction;
import org.sakaiproject.sitemanage.api.owl.UserSelection;

@Slf4j
public class OwlMigrationServiceImpl implements OwlMigrationService {

	@Setter protected ContentHostingService contentHostingService;
	@Setter protected EmailService emailService;
	@Setter protected EventTrackingService eventTrackingService;
	@Setter protected ServerConfigurationService serverConfigurationService;
	@Setter protected SessionManager sessionManager;
	@Setter protected SiteService siteService;

	private OwlMigrationDelegate migrationDelegate;

	public void init() {
		log.info("Initializing OwlMigrationServiceImpl");
		migrationDelegate = new OwlMigrationDelegate(contentHostingService, emailService, eventTrackingService, serverConfigurationService, sessionManager, siteService);
	}

	@Override
	public boolean isMigrationTabEnabled() {
		return OwlMigrationDAO.isMigrationEnabled();
	}

	@Override
	public List<SiteMigrationItem> getSiteMigrationItems() {
		if (!isMigrationTabEnabled()) {
			return List.of();
		}

		return migrationDelegate.getSiteMigrationItems();
	}

	@Override
	public Map<String, String> getMigrationTypes() {
		Map<String, String> optionMap = OwlMigrationDAO.getTypeOptions();
		optionMap.putAll(OwlMigrationDAO.getAdminTypeOptions());
		return optionMap;
	}

	@Override
	public Map<String, String> getActiveTypes() {
		return migrationDelegate.getActiveTypes();
	}

	@Override
	public Map<String, String> getMigrationActions() {
		return OwlMigrationDAO.getActionOptions();
	}

	@Override
	public Map<String, String> getActiveActions() {
		return migrationDelegate.getActiveActions();
	}

	@Override
	public Optional<String> getNoActiveTypesDisplay() {
		return OwlMigrationDAO.getDefaultTypeOption();
	}

	@Override
	public List<String> saveSelections(Map<String, UserSelection> siteSelections) {
		return migrationDelegate.saveSelections(siteSelections);
	}

	public Map<String, List<MigAction>> getTypeActionMap() {
		return migrationDelegate.getTypeActionMap();
	}

	@Override
	public List<String> getActionKeysWithResourcesSizeWarnings() {
		return OwlMigrationDAO.getActionsWithSizeChecks();
	}

	@Override
	public String getStatusDisplay(String actionKey, String statusKey) {
		return migrationDelegate.getStatusDisplay(actionKey, statusKey);
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
