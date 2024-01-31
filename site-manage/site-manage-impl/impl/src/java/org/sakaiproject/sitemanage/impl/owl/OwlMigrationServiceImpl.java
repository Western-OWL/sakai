package org.sakaiproject.sitemanage.impl.owl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sakaiproject.sitemanage.api.owl.MigrationOption;
import org.sakaiproject.sitemanage.api.owl.OwlMigrationService;
import org.sakaiproject.sitemanage.api.owl.SiteMigrationItem;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OwlMigrationServiceImpl implements OwlMigrationService {

	public void init() {
		log.info("Initializing OwlMigrationServiceImpl");
	}

	public boolean isMigrationTabEnabled() {
		return OwlMigrationDAO.isMigrationEnabled();
	}

	public Map<String, List<SiteMigrationItem>> getSiteMigrationItems() {
		throw new UnsupportedOperationException();
	}

	public List<MigrationOption> getMigrationOptions() {
		throw new UnsupportedOperationException();
	}

	public void saveSelection(SiteMigrationItem siteMigrationItem, String selectionKey) {
		throw new UnsupportedOperationException();
	}

	public Optional<String> getStatusDisplay(String selectionKey, String statusKey) {
		throw new UnsupportedOperationException();
	}

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
