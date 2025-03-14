package org.sakaiproject.sitemanage.api.owl;

import java.util.Optional;
import lombok.Data;

@Data
public class UserSelection
{
	private final Optional<String> type, action;
}
