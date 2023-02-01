/**********************************************************************************
 * $URL:  $
 * $Id:  $
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007, 2008, 2009 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/
package org.sakaiproject.sitemanage.impl;

// imports
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.AuthzRealmLockException;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.GroupNotDefinedException;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.util.SiteConstants;
import org.sakaiproject.site.util.SiteGroupHelper;
import org.sakaiproject.thread_local.api.ThreadLocalManager;

/**
* <p>RoleGroupEventWatcher is for </p>
*
* @author University of Michigan, Sakai Software Development Team
* @version $Revision$
*/
@Slf4j
public class RoleGroupEventWatcher implements Observer
{
	/*******************************************************************************
	* Dependencies and their setter methods
	*******************************************************************************/

	@Setter protected EventTrackingService eventTrackingService = null;
	@Setter protected SiteService siteService = null;
	@Setter protected AuthzGroupService authzGroupService = null;
	@Setter protected EntityManager entityManager;
	@Setter protected SecurityService securityService;
	@Setter protected ThreadLocalManager threadLocalManager;

	/*******************************************************************************
	* Init and Destroy
	*******************************************************************************/

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
			// start watching the events - only those generated on this server, not those from elsewhere
			eventTrackingService.addLocalObserver(this);

			log.debug("init()");
		}
		catch (Exception t)
		{
			log.warn("init(): ", t);
		}
	}

	/**
	* Returns to uninitialized state.
	*/
	public void destroy()
	{
		// done with event watching
		eventTrackingService.deleteObserver(this);

		log.debug("destroy()");
	}

	/*******************************************************************************
	* Observer implementation
	*******************************************************************************/
	/**
	* This method is called whenever the observed object is changed. An
	* application calls an <tt>Observable</tt> object's
	* <code>notifyObservers</code> method to have all the object's
	* observers notified of the change.
	*
	* default implementation is to cause the courier service to deliver to the
	* interface controlled by my controller.  Extensions can override.
	*
	* @param   o     the observable object.
	* @param   arg   an argument passed to the <code>notifyObservers</code>
	*                 method.
	*/
	public void update(Observable o, Object arg)
	{
		// arg is Event
		if (!(arg instanceof Event))
		{
			return;
		}
		Event event = (Event) arg;

		// check the event function against the functions we have notifications watching for
		String function = event.getEvent();

		if (function.equals(AuthzGroupService.SECURE_UPDATE_AUTHZ_GROUP)
				|| AuthzGroupService.SECURE_JOIN_AUTHZ_GROUP.equals(function) || AuthzGroupService.SECURE_UNJOIN_AUTHZ_GROUP.equals(function))
		{
			Reference ref = entityManager.newReference(event.getResource());

			// look for group reference. Need to replace it with parent site reference
			String refId = ref.getId();
			String groupType = Entity.SEPARATOR + SiteService.GROUP_SUBTYPE;
			if (refId.contains(groupType))
			{
				refId = refId.substring(0, refId.indexOf(groupType));
				ref = entityManager.newReference(refId);
			}

			// this is the current realm reference
			if (threadLocalManager.get("current.event.resource.ref") == null)
			{
				threadLocalManager.set("current.event.resource.ref", ref);

				// importing from template, bypass the permission checking:
				// temporarily allow the user to read and write to site, authzgroup, etc.
				SecurityAdvisor advisor = new SecurityAdvisor()
				{
					@Override
					public SecurityAdvisor.SecurityAdvice isAllowed(String userId, String function, String reference)
					{
						return SecurityAdvisor.SecurityAdvice.ALLOWED;
					}
				};

				try
				{
					securityService.pushAdvisor(advisor);
					String realmId = ref.getId();
					if (realmId != null && !realmId.contains(SiteService.GROUP_SUBTYPE) && realmId.startsWith("/" + SiteService.SITE_SUBTYPE + "/"))
					{
						// not for group realm update, only for site realm updates
						String siteId = realmId.replace(SiteService.REFERENCE_ROOT + "/", "");
						AuthzGroup r;

						r = authzGroupService.getAuthzGroup(realmId);
						Site site = siteService.getSite(siteId);

						// whether saving site is needed because some groups need updates
						boolean needSave = false;

						for (Group g : site.getGroups())
						{
							ResourceProperties properties = g.getProperties();
							if (properties.getProperty(Group.GROUP_PROP_WSETUP_CREATED) != null && properties.getProperty(SiteConstants.GROUP_PROP_ROLE_PROVIDERID) != null)
							{
								needSave = true;

								// this is a role provided group, need to sync with realm user now.
								String roleString = properties.getProperty(SiteConstants.GROUP_PROP_ROLE_PROVIDERID);
								if (roleString != null && roleString.length() > 0)
								{
									Set<Member> members = g.getMembers();
									Collection<String> roles = SiteGroupHelper.unpack(roleString);
									for (String role : roles)
									{
										// remove those provided members by role
										for(Member m : members)
										{
											if (m.getRole().getId().equals(role))
											{
												try
												{
													g.deleteMember(m.getUserId());
												}
												catch (AuthzRealmLockException e)
												{
													log.warn("update: User with id {} cannot be deleted from group with id {} because the group is locked. Aborting deletions for this group.", m.getUserId(), g.getId());
													break;
												}
											}
										}

										// update the group member with current realm users
										Set<String> roleUserSet = r.getUsersHasRole(role.trim());
										if (roleUserSet != null && !roleUserSet.isEmpty())
										{
											for(String userId : roleUserSet)
											{
												try
												{
													g.insertMember(userId, role.trim(), true, false);
												}
												catch (AuthzRealmLockException e)
												{
													log.warn("update: User with id {} cannot be inserted in group with id {} because the group is locked. Aborting insertions for this group.", userId, g.getId());
													break;
												}
											}
										}
									}
								}
							}
						}
						if (needSave)
						{
							// Save only site group membership updates to this site
							siteService.saveGroupMembership(site);
						}
					}
				}
				catch (GroupNotDefinedException | IdUnusedException | PermissionException e)
				{
					log.warn("update: {}: {}", e, event.getResource());
				}
				catch (Exception e)
				{
					log.warn("update: {}: {}", e, event.getResource());
				}
				finally
				{
					securityService.popAdvisor(advisor);
				}

				// reset
				threadLocalManager.set("current.event.resource.ref", null);
			}
		}
	} // update
} // RoleGroupEventWatcher
