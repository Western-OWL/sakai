package org.sakaiproject.webapi;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Singleton component representing Dashboard configuration.
 * @author bbailla2
 */
@Component("org.sakaiproject.webapi.DashboardConfig")
@Scope("singleton")
public class DashboardConfig {

	private static final String SAK_PROP_DASHBOARD_ENABLED = "webapi.dashboard.enabled";
	private static final boolean SAK_PROP_DASHBOARD_ENABLED_DEFAULT = true;

	private static boolean dashboardEnabled;
	public boolean isDashboardEnabled() { return dashboardEnabled; }

	private ServerConfigurationService serverConfigurationService;

	@Autowired
	public DashboardConfig(ServerConfigurationService serverConfigurationService) {

		this.serverConfigurationService = serverConfigurationService;

		dashboardEnabled = serverConfigurationService.getBoolean(SAK_PROP_DASHBOARD_ENABLED, SAK_PROP_DASHBOARD_ENABLED_DEFAULT);
	}

}
