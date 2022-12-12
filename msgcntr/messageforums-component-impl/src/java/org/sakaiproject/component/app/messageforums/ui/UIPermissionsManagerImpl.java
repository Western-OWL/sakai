/**********************************************************************************
 * $URL: https://source.sakaiproject.org/svn/msgcntr/trunk/messageforums-component-impl/src/java/org/sakaiproject/component/app/messageforums/ui/UIPermissionsManagerImpl.java $
 * $Id: UIPermissionsManagerImpl.java 9227 2006-05-15 15:02:42Z cwen@iupui.edu $
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007, 2008 The Sakai Foundation
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
package org.sakaiproject.component.app.messageforums.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import org.sakaiproject.api.app.messageforums.Area;
import org.sakaiproject.api.app.messageforums.AreaManager;
import org.sakaiproject.api.app.messageforums.DBMembershipItem;
import org.sakaiproject.api.app.messageforums.DiscussionForum;
import org.sakaiproject.api.app.messageforums.DiscussionTopic;
import org.sakaiproject.api.app.messageforums.Message;
import org.sakaiproject.api.app.messageforums.MessageForumsTypeManager;
import org.sakaiproject.api.app.messageforums.PermissionLevelManager;
import org.sakaiproject.api.app.messageforums.PermissionManager;
import org.sakaiproject.api.app.messageforums.Topic;
import org.sakaiproject.api.app.messageforums.ui.DiscussionForumManager;
import org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.app.messageforums.TestUtil;
import org.sakaiproject.component.app.messageforums.dao.hibernate.DBMembershipItemImpl;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

/**
 * @author <a href="mailto:rshastri@iupui.edu">Rashmi Shastri</a>
 */
@Slf4j
public class UIPermissionsManagerImpl implements UIPermissionsManager {

  // dependencies
  private AuthzGroupService authzGroupService;
  private SessionManager sessionManager;
  private ToolManager toolManager;
  private PermissionManager permissionManager;
  private PermissionLevelManager permissionLevelManager;
  private MessageForumsTypeManager typeManager;
  private SecurityService securityService;
  private DiscussionForumManager forumManager;
  private AreaManager areaManager;
  private MemoryService memoryService;
  private Cache<String, Set<String>> userGroupMembershipCache;
  private UserDirectoryService userDirectoryService;
  private SiteService siteService;
  private ThreadLocalManager threadLocalManager;
  
  public void init()
  {
     log.info("init()");
     userGroupMembershipCache = memoryService.getCache("org.sakaiproject.component.app.messageforums.ui.UIPermissionsManagerImpl.userGroupMembershipCache");
  }

  /**
   * @param areaManager
   *          The areaManager to set.
   */
  public void setAreaManager(AreaManager areaManager)
  {
    this.areaManager = areaManager;
  }

  /**
   * @param forumManager
   *          The forumManager to set.
   */
  public void setForumManager(DiscussionForumManager forumManager)
  {
    this.forumManager = forumManager;
  }

  /**
   * @param authzGroupService
   *          The authzGroupService to set.
   */
  public void setAuthzGroupService(AuthzGroupService authzGroupService)
  {
    log.debug("setAuthzGroupService(AuthzGroupService {})", authzGroupService);
    this.authzGroupService = authzGroupService;
  }

  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  public void setSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  public void setThreadLocalManager(ThreadLocalManager threadLocalManager) {
    this.threadLocalManager = threadLocalManager;
  }

/**
   * @param sessionManager
   *          The sessionManager to set.
   */
  public void setSessionManager(SessionManager sessionManager)
  {
    log.debug("setSessionManager(SessionManager {})", sessionManager);
    this.sessionManager = sessionManager;
  }

  /**
   * @param toolManager
   *          The toolManager to set.
   */
  public void setToolManager(ToolManager toolManager)
  {
    log.debug("setToolManager(ToolManager {})", toolManager);
    this.toolManager = toolManager;
  }

  /**
   * @param permissionManager
   *          The permissionManager to set.
   */
  public void setPermissionManager(PermissionManager permissionManager)
  {
    log.debug("setPermissionManager(PermissionManager {})", permissionManager);
    this.permissionManager = permissionManager;
  }

  /**
   * @param typeManager
   *          The typeManager to set.
   */
  public void setTypeManager(MessageForumsTypeManager typeManager)
  {
    log.debug("setTypeManager(MessageForumsTypeManager {})", typeManager);
    this.typeManager = typeManager;
  }

  /**
   * @param securityService
   *          The securityService to set.
   */
  public void setSecurityService(SecurityService securityService)
  {
    log.debug("setSecurityService(SecurityService {})", securityService);
    this.securityService = securityService;
  }

  // end dependencies
  /**
   * @see org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager#isNewForum()
   */
  @Override
  public boolean isNewForum()
  {
    log.debug("isNewForum()");
    if (isSuperUser())
    {
      return true;
    }
    
    try
    {
		/* OWLTODO: Q: sometimes this can be called in a scenario where we can derive the correct site id,
		 * such as when duplicating a forum. if we get the forum's area can we replace the getAreaItems call?
		 * A: No need, isAccessPrivileges is checked against the target for such cases.*/
      Iterator iter = getAreaItemsByCurrentUser(); // OWLTODO: what is an area item exactly? Answered!:
      /*
       * It's a DBMembershipItem (mfr_membership_item_t).
       *     -These are essentially the roles and groups that show under 'Permissions' when editing stuff.
       *     -They can be associated with topics (t_surrogateKey), forums (of_surrogateKey), or areas (a_surrogateKey).
       *     -They link via (PERMISSION_LEVEL) to a mfr_permission_level_t - these 'Author', 'Contributor', etc., and every 'Custom' combination.
       *
       * But what's an *area* item?: It's from 'Template Settings'. Evidence: as an instructor, turn off 'New Forum' from your own Instructor permissions; notice the 'New Forum' tab goes away on reset.
       * Of course, you can just turn that permission back on for yourself.
       */
      while (iter.hasNext())
      {
        DBMembershipItem item = (DBMembershipItem) iter.next();
        if (item.getPermissionLevel().getNewForum().booleanValue())
        {
          return true;
        }
      }

    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      return false;
    }
    return false;
  }

  /**
   * @see org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager#isChangeSettings(org.sakaiproject.api.app.messageforums.DiscussionForum)
   */
  public boolean isChangeSettings(DiscussionForum forum)
  {
	  return isChangeSettings(forum, forumManager.getSiteIdForForum(forum));
  }

  /**
   * Determines if the current user is allowed to change forum settings.
   * This method is private because it trusts the forum and siteid match.
   * OWLTODO: This method exists to eliminate the chance that deriving
   * a siteId from the forum will hit the db. If this turns out to be highly
   * unlikely due to hierarchy stability or caching, all these methods that
   * take a siteId explicitly can and probably should be eliminated.
   * However, there seems to be a problem with newly created forums vs existing
   * forums and we may need to keep this method around to work around the issue.
   * @param forum the forum in question
   * @param siteId the site the forum belongs to
   * @return true if the user is admin/instructor/owner, or has change settings permission
   */
  private boolean isChangeSettings(DiscussionForum forum, String siteId)
  {

    log.debug("isChangeSettings(DiscussionForum {})", forum);
    if (isSuperUser())
    {
      return true;
    }
    if (isInstructor(siteId)){
      if (!forum.getRestrictPermissionsForGroups()){
        return true;
      }
      //if restricted && belongs to group
      if(isInstructorForAllowedGroup(forum.getId(), siteId, true)){
        return true;
      }
    }
    if (forumManager.isForumOwner(forum, getCurrentUserId(), siteId)) // this allows a brand new forum object that doesn't even have an id or area yet to pass this check
    {
      return true;
    }
    
    try
    {
      Iterator iter = getForumItemsByCurrentUser(forum);
      while (iter.hasNext())
      {
        DBMembershipItem item = (DBMembershipItem) iter.next();
        if (item.getPermissionLevel().getChangeSettings().booleanValue())
        {
          return true;
        }
      }
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      return false;
    }
    return false;
  }

  public boolean isInstructorForAllowedGroup(Long objectId, String siteId, boolean isForum){

    if(objectId == null || !isInstructor(siteId)){
        return false;
    }

        List<String> groupTitle;
    if(isForum){
      groupTitle = forumManager.getAllowedGroupForRestrictedForum(objectId, PermissionLevelManager.PERMISSION_LEVEL_NAME_CONTRIBUTOR);
    } else {
      groupTitle = forumManager.getAllowedGroupForRestrictedTopic(objectId, PermissionLevelManager.PERMISSION_LEVEL_NAME_CONTRIBUTOR);
    }
    log.debug("Allowed group title {} for object {}", groupTitle, objectId);
    try {
	  Site site = siteService.getSite(siteId);
      Set<String> groups = getGroupsWithMember(site, getCurrentUserId());
            return groups.stream().map(site::getGroup).anyMatch(g -> groupTitle.contains(g.getTitle()));
    } catch(Exception e){
      log.error("isInstructorForAllowedGroup error: exception {} in forum {}", e.getMessage(), objectId);
    }
    return false;
  }

  /**   
   * @see org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager#isNewTopic(org.sakaiproject.api.app.messageforums.DiscussionForum)
   */
  public boolean isNewTopic(DiscussionForum forum)
  {
	  return isNewTopic(forum, forumManager.getSiteIdForForum(forum));
  }

  private boolean isNewTopic(DiscussionForum forum, String siteId)
  {
    log.debug("isNewTopic(DiscussionForum {})", forum);
    if (isSuperUser())
    {
      return true;
    }
    if (isInstructor(siteId)){
      if (forum.getRestrictPermissionsForGroups() && isInstructorForAllowedGroup(forum.getId(), siteId, true)){
        return true;
      }
    }
    try
    {
      Iterator iter = getForumItemsByCurrentUser(forum);
      while (iter.hasNext())
      {
        DBMembershipItem item = (DBMembershipItem) iter.next();
        if (item.getPermissionLevel().getNewTopic().booleanValue())
        {
          return true;
        }
      }
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      return false;
    }
    return false;
  }

  /** 
   * @see org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager#isNewResponse(org.sakaiproject.api.app.messageforums.DiscussionTopic,
   *      org.sakaiproject.api.app.messageforums.DiscussionForum)
   */
  public boolean isNewResponse(DiscussionTopic topic, DiscussionForum forum)
  {
	  return isNewResponse(topic, forum, getCurrentUserId(), forumManager.getSiteIdForForum(forum));
  }
  
  public boolean isNewResponse(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId){
    log.debug("isNewResponse(DiscussionTopic {}), DiscussionForum {}", topic, forum);

    try
    {
      if (checkBaseConditions(topic, forum, userId, contextId))
      {
        return true;
      }
      Iterator iter = getTopicItemsByUser(topic, userId, contextId);
      while (iter.hasNext())
      {
        DBMembershipItem item = (DBMembershipItem) iter.next();
        if (item.getPermissionLevel().getNewResponse().booleanValue()
        	&& forum != null
            && forum.getDraft().equals(Boolean.FALSE)
            && forum.getLocked().equals(Boolean.FALSE)
            && topic.getDraft().equals(Boolean.FALSE)
            && topic.getLocked().equals(Boolean.FALSE))
        {
          return true;
        }
      }
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      return false;
    }
    return false;
  }

  /**
   * @see org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager#isNewResponseToResponse(org.sakaiproject.api.app.messageforums.DiscussionTopic,
   *      org.sakaiproject.api.app.messageforums.DiscussionForum)
   */
  @Override
  public boolean isNewResponseToResponse(DiscussionTopic topic, DiscussionForum forum)
  {
	return isNewResponseToResponse(topic, forum, getCurrentUserId(), forumManager.getSiteIdForForum(forum));
  }

  @Override
  public boolean isNewResponseToResponse(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId) {
    log.debug("isNewResponseToResponse(DiscussionTopic {}, DiscussionForum {})", topic, forum);

    try
    {
      if (checkBaseConditions(topic, forum, userId, contextId))
      {
        return true;
      }
      Iterator iter = getTopicItemsByUser(topic, userId, contextId);
      while (iter.hasNext())
      {
        DBMembershipItem item = (DBMembershipItem) iter.next();
        if (item.getPermissionLevel().getNewResponseToResponse().booleanValue()
            && forum.getDraft().equals(Boolean.FALSE)
            && forum.getLocked().equals(Boolean.FALSE)
            && topic.getDraft().equals(Boolean.FALSE)
            && topic.getLocked().equals(Boolean.FALSE))
        {
          return true;
        }
      }

    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      return false;
    }
    return false;
  }

  /**   
   * @see org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager#isMovePostings(org.sakaiproject.api.app.messageforums.DiscussionTopic,
   *      org.sakaiproject.api.app.messageforums.DiscussionForum)
   */
  public boolean isMovePostings(DiscussionTopic topic, DiscussionForum forum)
  {
    log.debug("isMovePostings(DiscussionTopic {}), DiscussionForum {}", topic, forum);

    try
    {
      if (checkBaseConditions(topic, forum))
      {
        return true;
      }
      Iterator iter = getTopicItemsByCurrentUser(topic);
      while (iter.hasNext())
      {
        DBMembershipItem item = (DBMembershipItem) iter.next();
        if ((item.getPermissionLevel().getMovePosting().booleanValue()
            || item.getPermissionLevel().getReviseAny().booleanValue()
            || item.getPermissionLevel().getReviseOwn().booleanValue())
            && forum.getDraft().equals(Boolean.FALSE)
            && forum.getLocked().equals(Boolean.FALSE)
            && topic.getDraft().equals(Boolean.FALSE)
            && topic.getLocked().equals(Boolean.FALSE))
        {
          return true;
        }
      }

    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      return false;
    }
    return false;
  }

  /**
   * @see org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager#isChangeSettings(org.sakaiproject.api.app.messageforums.DiscussionTopic,
   *      org.sakaiproject.api.app.messageforums.DiscussionForum)
   */
  @Override
  public boolean isChangeSettings(DiscussionTopic topic, DiscussionForum forum)
  {
	  return isChangeSettings(topic, forum, getCurrentUserId());
  }

  @Override
  public boolean isChangeSettings(DiscussionTopic topic, DiscussionForum forum, String userId)
  {
	  return isChangeSettings(topic, forum, userId, forumManager.getSiteIdForForum(forum));
  }

  private boolean isChangeSettings(DiscussionTopic topic, DiscussionForum forum, String userId, String siteId)
  {
    log.debug("isChangeSettings(DiscussionTopic {}), DiscussionForum {}", topic, forum);
    if (isSuperUser(userId))
    {
      return true;
    }
	// make sure the topic belongs to the forum, as we use the forum to get the site id
	// OWLTODO: null checks here, getBaseForum can return null, perhaps getOpenForum can as well
	// what to do in the case there topic's forum is null? reject?
	// OWLTODO: probably should have similar validation in other methods that take topic and forum, extract
	// validation logic to method?
	if (!forum.getId().equals(topic.getOpenForum().getId()))
	{
		log.error("Given topic {} does not belong to given forum {}", topic.getId(), forum.getId());
		return false;
	}
    if (isInstructor(userId, siteId)){
      if (!forum.getRestrictPermissionsForGroups() && !topic.getRestrictPermissionsForGroups()){
        return true;
      }
      //if restricted && belongs to group
      if ((forum.getRestrictPermissionsForGroups() && isInstructorForAllowedGroup(forum.getId(), siteId, true)) || (topic.getRestrictPermissionsForGroups() && isInstructorForAllowedGroup(topic.getId(), siteId, false))){
        return true;
      }
    }
    try
    {
      // Change Settings on Topic allowed even if the forum is locked
      // if (forum.getLocked() == null || forum.getLocked().equals(Boolean.TRUE))
      // {
      // log.debug("This Forum is Locked");
      // return false;
      // }
      // if owner then allow change of settings on the topic or on forum.
      if (forumManager.isTopicOwner(topic, userId))
      {
        return true; // OWLTODO: this is questionable, a user can create a topic and be demoted to a role that can't, however it might be required for new topics to pass this check
        // OWLTODO: I would tend to agree, but I'd argue in Forums it's too easy for a person to demote their own role. If we changed this, then nobody could fix the issue except an admin = helpdesk ticket. -Brian
      }
      Iterator iter = getTopicItemsByUser(topic, userId);
      while (iter.hasNext())
      {
        DBMembershipItem item = (DBMembershipItem) iter.next();
        if (item.getPermissionLevel().getChangeSettings().booleanValue())
           // && forum.getDraft().equals(Boolean.FALSE)  SAK-9230
           // && forum.getLocked().equals(Boolean.FALSE)
           // && topic.getDraft().equals(Boolean.FALSE)
           // && topic.getLocked().equals(Boolean.FALSE))
        {
          return true;
        }
      }

    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      return false;
    }
    return false;
  }

  /** 
   * @see org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager#isPostToGradebook(org.sakaiproject.api.app.messageforums.DiscussionTopic,
   *      org.sakaiproject.api.app.messageforums.DiscussionForum)
   */
  @Override
  public boolean isPostToGradebook(DiscussionTopic topic, DiscussionForum forum){
	  return isPostToGradebook(topic, forum, getCurrentUserId());
  }

  @Override
  public boolean isPostToGradebook(DiscussionTopic topic, DiscussionForum forum, String userId)
  {
	  return isPostToGradebook(topic, forum, userId, forumManager.getSiteIdForForum(forum));
  }

  @Override
  public boolean isPostToGradebook(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId)
  {
    log.debug("isPostToGradebook(DiscussionTopic {}, DiscussionForum {})", topic, forum);

    try
    {
      if (checkBaseConditions(topic, forum, userId, contextId))
      {
        return true;
      }
      Iterator iter = getTopicItemsByUser(topic, userId, contextId);
      while (iter.hasNext())
      {
        DBMembershipItem item = (DBMembershipItem) iter.next();
        if (item.getPermissionLevel().getPostToGradebook().booleanValue()
            && forum.getDraft().equals(Boolean.FALSE)
            && topic.getDraft().equals(Boolean.FALSE))
        {
          return true;
        }
      }

    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      return false;
    }
    return false;
  }

  /**   
   * @see org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager#isRead(org.sakaiproject.api.app.messageforums.DiscussionTopic,
   *      org.sakaiproject.api.app.messageforums.DiscussionForum)
   */

  @Override
  public boolean isRead(DiscussionTopic topic, DiscussionForum forum){
	  return isRead(topic, forum, getCurrentUserId());
  }

  @Override
  public boolean isRead(DiscussionTopic topic, DiscussionForum forum, String userId){
	  String contextId = null;
	  try{
		  //context could be null b/c of external queries... first check
		  //since its faster than a DB lookup
		  contextId = forumManager.getSiteIdForForum(forum);
	  }catch (Exception e) {
		  contextId = forumManager.getContextForForumById(forum.getId()); // OWLTODO: getSiteIdForForum will do this as a fallback eventually, so remove it later when it becomes redundant?
	}
	  return isRead(topic, forum, userId, contextId);
  }
  
  @Override
  public boolean isRead(DiscussionTopic topic, DiscussionForum forum, String userId, String siteId)
  {
      log.debug("isRead(DiscussionTopic {}, DiscussionForum {})", topic, forum);
	  return isRead(topic.getId(), topic.getDraft(), forum.getDraft(), userId, siteId);
  }
  
  @Override
  public boolean isRead(Long topicId, Boolean isTopicDraft, Boolean isForumDraft, String userId, String siteId)
  {
    
    try
    {
      if (checkBaseConditions(null, null, userId, "/site/" + siteId)) // OWLTODO: this looks wrong, doesn't take a site ref, just a site id
      {
        return true;
      }
      Iterator iter = getTopicItemsByUser(topicId, userId, siteId);
      while (iter.hasNext())
      {
        DBMembershipItem item = (DBMembershipItem) iter.next();
        if (item.getPermissionLevel().getRead().booleanValue()
            && isForumDraft.equals(Boolean.FALSE)
//            && forum.getLocked().equals(Boolean.FALSE)
            && isTopicDraft.equals(Boolean.FALSE))
//            && topic.getLocked().equals(Boolean.FALSE))
        {
          return true;
        }
      }

    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      return false;
    }
    return false;
  }

  /**   
   * @see org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager#isReviseAny(org.sakaiproject.api.app.messageforums.DiscussionTopic,
   *      org.sakaiproject.api.app.messageforums.DiscussionForum)
   */
  public boolean isReviseAny(DiscussionTopic topic, DiscussionForum forum)
  {
	  return isReviseAny(topic, forum, getCurrentUserId(), forumManager.getSiteIdForForum(forum));
  }
  
  public boolean isReviseAny(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId){
      log.debug("isReviseAny(DiscussionTopic {}, DiscussionForum {})", topic, forum);
    try
    {
      if (checkBaseConditions(topic, forum, userId, contextId))
      {
        return true;
      }
       if (topic.getLocked() == null || topic.getLocked().equals(Boolean.TRUE))
    {
      log.debug("This topic is locked {}", topic);
      return false;
    }
    if (topic.getDraft() == null || topic.getDraft().equals(Boolean.TRUE))
    {
      log.debug("This topic is at draft stage {}", topic);
    }
      Iterator iter = getTopicItemsByUser(topic, userId, contextId);
      while (iter.hasNext())
      {
        DBMembershipItem item = (DBMembershipItem) iter.next();
        if (item.getPermissionLevel().getReviseAny().booleanValue()
            && forum.getDraft().equals(Boolean.FALSE)
            && forum.getLocked().equals(Boolean.FALSE)
            && topic.getDraft().equals(Boolean.FALSE)
            && topic.getLocked().equals(Boolean.FALSE))
        {
          return true;
        }
      }

    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      return false;
    }
    return false;
  }

  /**   
   * @see org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager#isReviseOwn(org.sakaiproject.api.app.messageforums.DiscussionTopic,
   *      org.sakaiproject.api.app.messageforums.DiscussionForum)
   */
  public boolean isReviseOwn(DiscussionTopic topic, DiscussionForum forum)
  {
	  return isReviseOwn(topic, forum, getCurrentUserId(), forumManager.getSiteIdForForum(forum));
  }
  
  public boolean isReviseOwn(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId){
    log.debug("isReviseOwn(DiscussionTopic {}, DiscussionForum {})", topic, forum);
    try
    {
      if (checkBaseConditions(topic, forum,  userId, contextId))
      {
        return true;
      }
      
       if (topic.getLocked() == null || topic.getLocked().equals(Boolean.TRUE))
    {
      log.debug("This topic is locked {}", topic);
      return false;
    }
    if (topic.getDraft() == null || topic.getDraft().equals(Boolean.TRUE))
    {
      log.debug("This topic is at draft stage {}", topic);
    }
      Iterator iter = getTopicItemsByUser(topic, userId, contextId);
      while (iter.hasNext())
      {
        DBMembershipItem item = (DBMembershipItem) iter.next();
        if (item.getPermissionLevel().getReviseOwn().booleanValue()
            && forum.getDraft().equals(Boolean.FALSE)
            && forum.getLocked().equals(Boolean.FALSE)
            && topic.getDraft().equals(Boolean.FALSE)
            && topic.getLocked().equals(Boolean.FALSE))
        {
          return true;
        }
      }

    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      return false;
    }
    return false;
  }

  /**   
   * @see org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager#isDeleteAny(org.sakaiproject.api.app.messageforums.DiscussionTopic,
   *      org.sakaiproject.api.app.messageforums.DiscussionForum)
   */
  @Override
  public boolean isDeleteAny(DiscussionTopic topic, DiscussionForum forum)
  {
	return isDeleteAny(topic, forum, getCurrentUserId(), forumManager.getSiteIdForForum(forum));
  }
  
  @Override
  public boolean isDeleteAny(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId){
    log.debug("isDeleteAny(DiscussionTopic {}, DiscussionForum {})", topic, forum);
    try
    {
      if (checkBaseConditions(topic, forum, userId, contextId))
      {
        return true;
      }
        if (topic.getLocked() == null || topic.getLocked().equals(Boolean.TRUE))
    {
      log.debug("This topic is locked {}", topic);
      return false;
    }
    if (topic.getDraft() == null || topic.getDraft().equals(Boolean.TRUE))
    {
      log.debug("This topic is at draft stage {}", topic);
    }
      Iterator iter = getTopicItemsByUser(topic, userId, contextId);
      while (iter.hasNext())
      {
        DBMembershipItem item = (DBMembershipItem) iter.next();
        if (item.getPermissionLevel().getDeleteAny().booleanValue()
            && forum.getDraft().equals(Boolean.FALSE)
            && forum.getLocked().equals(Boolean.FALSE)
            && topic.getDraft().equals(Boolean.FALSE)
            && topic.getLocked().equals(Boolean.FALSE))
        {
          return true;
        }
      }

    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      return false;
    }
    return false;
  }

  /**   
   * @see org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager#isDeleteOwn(org.sakaiproject.api.app.messageforums.DiscussionTopic,
   *      org.sakaiproject.api.app.messageforums.DiscussionForum)
   */
  @Override
  public boolean isDeleteOwn(DiscussionTopic topic, DiscussionForum forum)
  {
	  return isDeleteOwn(topic, forum, getCurrentUserId(), forumManager.getSiteIdForForum(forum));
  }
  
  @Override
  public boolean isDeleteOwn(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId){
    log.debug("isDeleteOwn(DiscussionTopic {}, DiscussionForum {})", topic, forum);
    try
    {
      if (checkBaseConditions(topic, forum, userId, contextId))
      {
        return true;
      }
        if (topic.getLocked() == null || topic.getLocked().equals(Boolean.TRUE))
    {
      log.debug("This topic is locked {}", topic);
      return false;
    }
    if (topic.getDraft() == null || topic.getDraft().equals(Boolean.TRUE))
    {
      log.debug("This topic is at draft stage {}", topic);
    }
      Iterator iter = getTopicItemsByUser(topic, userId, contextId);
      while (iter.hasNext())
      {
        DBMembershipItem item = (DBMembershipItem) iter.next();
        if (item.getPermissionLevel().getDeleteOwn().booleanValue()
            && forum.getDraft().equals(Boolean.FALSE)
            && forum.getLocked().equals(Boolean.FALSE)
            && topic.getDraft().equals(Boolean.FALSE)
            && topic.getLocked().equals(Boolean.FALSE))
        {
          return true;
        }
      }

    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      return false;
    }
    return false;
  }

  /**   
   * @see org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager#isMarkAsRead(org.sakaiproject.api.app.messageforums.DiscussionTopic,
   *      org.sakaiproject.api.app.messageforums.DiscussionForum)
   */
  public boolean isMarkAsRead(DiscussionTopic topic, DiscussionForum forum)
  {
      log.debug("isMarkAsRead(DiscussionTopic {}, DiscussionForum {})", topic, forum);
    try
    {
      if (checkBaseConditions(topic, forum))
      {
        return true;
      }
        if (topic.getLocked() == null || topic.getLocked().equals(Boolean.TRUE))
    {
      log.debug("This topic is locked {}", topic);
      return false;
    }
    if (topic.getDraft() == null || topic.getDraft().equals(Boolean.TRUE))
    {
      log.debug("This topic is at draft stage {}", topic);
    }
      Iterator iter = getTopicItemsByCurrentUser(topic);
      while (iter.hasNext())
      {
        DBMembershipItem item = (DBMembershipItem) iter.next();
        if (item.getPermissionLevel().getMarkAsRead().booleanValue()
            && forum.getDraft().equals(Boolean.FALSE)
            && forum.getLocked().equals(Boolean.FALSE)
            && topic.getDraft().equals(Boolean.FALSE)
            && topic.getLocked().equals(Boolean.FALSE))
        {
          return true;
        }
      }

    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      return false;
    }
    return false;

  }
  
  /**   
   * @see org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager#isModerate(org.sakaiproject.api.app.messageforums.DiscussionTopic,
   *      org.sakaiproject.api.app.messageforums.DiscussionForum)
   */
  @Override
  public boolean isModeratePostings(DiscussionTopic topic, DiscussionForum forum){
	  return isModeratePostings(topic, forum, getCurrentUserId());
  }
  
  @Override
  public boolean isModeratePostings(DiscussionTopic topic, DiscussionForum forum, String userId)
  {
	return isModeratePostings(topic, forum, userId, forumManager.getSiteIdForForum(forum));
  }
  
  @Override
  public boolean isModeratePostings(DiscussionTopic topic, DiscussionForum forum, String userId, String siteId)
  {
	  return isModeratePostings(topic.getId(), forum.getLocked(), forum.getDraft(), topic.getLocked(), topic.getDraft(), userId, siteId);
  }
  
  @Override
  public boolean isModeratePostings(Long topicId, Boolean isForumLocked, Boolean isForumDraft, Boolean isTopicLocked, Boolean isTopicDraft, String userId, String siteId)
  {
    // NOTE: the forum or topic being locked should not affect a user's ability to moderate,
    // so logic related to the locked status was removed
    try
    {
      if (checkBaseConditions(null, null, userId, siteId))
      {
        return true;
      }
      
    if (isTopicDraft == null || isTopicDraft.equals(Boolean.TRUE))
    {
      log.debug("This topic is at draft stage {}", topicId);
    }
      Iterator iter = getTopicItemsByUser(topicId, userId, siteId);
      while (iter.hasNext())
      {
        DBMembershipItem item = (DBMembershipItem) iter.next();
        if (item.getPermissionLevel().getModeratePostings().booleanValue()
            && isForumDraft.equals(Boolean.FALSE)
            && isTopicDraft.equals(Boolean.FALSE))
        {
          return true;
        }
      }

    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      return false;
    }
    return false;
  }

  public boolean isIdentifyAnonAuthors(Topic topic)
  {
    String currentUserId = getCurrentUserId();
    if (isSuperUser(currentUserId))
    {
      return true;
    }

    try
    {
		// OWLTODO: is this always DiscussionTopic? should the getSiteId method take just a Topic instead? We wrote this method so perhaps Topic is unnecessarily broad, usually DiscussionTopic is used in this class
      Iterator iter = getTopicItemsByUser(topic.getId(), currentUserId, forumManager.getSiteIdForTopic((DiscussionTopic) topic));
      while (iter.hasNext())
      {
        DBMembershipItem item = (DBMembershipItem) iter.next();
        if (item.getPermissionLevel().getIdentifyAnonAuthors())
        {
          return true;
        }
      }
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }

    return false;
  }
  
  @Override
  public List<String> getCurrentUserMemberships(String siteId)
  {
	  List<String> userMemberships = new ArrayList<>();
	  // first, add the user's role
	  final String currRole = getCurrentUserRole(siteId);
	  if (currRole != null && !currRole.isEmpty()) {
		  userMemberships.add(currRole);
	  }
	  // now, add any groups the user is a member of
	  try {
		  Site site = siteService.getSite(siteId);
		  Set<String> groups = getGroupsWithMember(site, getCurrentUserId());
		  groups.stream().map(site::getGroup).filter(Objects::nonNull).map(Group::getTitle).forEach(userMemberships::add);
	  } catch (IdUnusedException iue) {
		  log.debug("No memberships found");
	  }
	  
	  return userMemberships;
  }
  
  private Iterator<DBMembershipItem> getAreaItemsByCurrentUser()
  { 
    log.debug("getAreaItemsByCurrentUser()");

  	List<DBMembershipItem> areaItems = new ArrayList<>();

	String siteId = toolManager.getCurrentPlacement().getContext();
  	
		if (threadLocalManager.get("message_center_permission_set") == null || !((Boolean)threadLocalManager.get("message_center_permission_set")).booleanValue())
		{
			initMembershipForSite(siteId);
			// OWLTODO: this appears to be one situation where we don't have any site reference available
			// from a forums object, so we likely have to rely on getCurrentPlacement() here
			// come back after all the holes are plugged and make sure this is not exploitable
		}

	Set areaItemsInThread = (Set) threadLocalManager.get("message_center_membership_area");
	DBMembershipItem item = forumManager.getDBMember(areaItemsInThread, getCurrentUserRole(siteId),
			DBMembershipItem.TYPE_ROLE, toSiteRef(siteId));
    
    if (item != null){
        areaItems.add(item);
    }
    
    // for group awareness
    try {
		// OWLTODO: this method is called from isNewForum() which figures out if you can create forums in the site...
		// it has nothing to reference (I think) so it has to use the current placement (see above)
    	//Site currentSite = siteService.getSite(getContextId());
		Site currentSite = siteService.getSite(siteId);
    	Set<String> groups = getGroupsWithMember(currentSite, getCurrentUserId());
    	if (groups != null) {
    	    groups.stream().map(currentSite::getGroup)
                    .map(g -> forumManager.getDBMember(areaItemsInThread, g.getTitle(), DBMembershipItem.TYPE_GROUP, toSiteRef(siteId)))
                    .filter(Objects::nonNull)
                    .forEach(areaItems::add);
    	}
    }
    catch (Exception iue) {
    	log.error("Error pulling users group memberships", iue);
    }
    
    return areaItems.iterator();
  }

  public Set getAreaItemsSet(Area area)
  {
		if (threadLocalManager.get("message_center_permission_set") == null || !((Boolean)threadLocalManager.get("message_center_permission_set")).booleanValue())
		{
			initMembershipForSite(area.getContextId());
		}
		Set allAreaSet = (Set) threadLocalManager.get("message_center_membership_area");
		Set returnSet = new HashSet();
		if(allAreaSet != null)
		{
			Iterator iter = allAreaSet.iterator();
			while(iter.hasNext())
			{
				DBMembershipItemImpl thisItem = (DBMembershipItemImpl)iter.next();
				if(thisItem.getArea() != null && area.getId() != null && area.getId().equals(thisItem.getArea().getId()))
				{
					returnSet.add((DBMembershipItem)thisItem);
				}
			}
		}

		return returnSet;
  }
  
  private Iterator getForumItemsByCurrentUser(DiscussionForum forum)
  {
    List<DBMembershipItem> forumItems = new ArrayList<>();
    //Set membershipItems = forum.getMembershipItemSet();

	String siteId = forumManager.getSiteIdForForum(forum);

		if (threadLocalManager.get("message_center_permission_set") == null || !((Boolean)threadLocalManager.get("message_center_permission_set")).booleanValue())
		{
			initMembershipForSite(siteId);
		}

		Set forumItemsInThread = (Set) threadLocalManager.get("message_center_membership_forum");
		Set thisForumItemSet = new HashSet();
		Iterator iter = forumItemsInThread.iterator();
		while(iter.hasNext())
		{
			DBMembershipItemImpl thisItem = (DBMembershipItemImpl)iter.next();
			if(thisItem.getForum() != null && forum.getId()!=null&&forum.getId().equals(thisItem.getForum().getId()))
			{
				thisForumItemSet.add((DBMembershipItem)thisItem);
			}
		}
		if(thisForumItemSet.size()==0&&getAnonRole(toSiteRef(siteId))==true&&".anon".equals(forum.getCreatedBy())&&forum.getTopicsSet()==null){
			Set newForumMembershipset=forum.getMembershipItemSet();
	        Iterator iterNewForum = newForumMembershipset.iterator();
	        while (iterNewForum.hasNext())
	        {
	          DBMembershipItem item = (DBMembershipItem)iterNewForum.next();
	          if (".anon".equals(item.getName()))
	          {
	        	  thisForumItemSet.add(item);
	          }       
	        }			
		}
    
//    DBMembershipItem item = forumManager.getDBMember(membershipItems, getCurrentUserRole(),
//        DBMembershipItem.TYPE_ROLE);
		DBMembershipItem item = forumManager.getDBMember(thisForumItemSet, getCurrentUserRole(siteId),
			DBMembershipItem.TYPE_ROLE, toSiteRef(siteId));
    
    if (item != null){
      forumItems.add(item);
    }
    
	//  for group awareness
    try {
		Site currentSite = siteService.getSite(siteId);
    	Set<String> groups = getGroupsWithMember(currentSite, getCurrentUserId());

    	if(groups != null) {
            groups.stream().map(currentSite::getGroup)
                    .map(g -> forumManager.getDBMember(thisForumItemSet, g.getTitle(), DBMembershipItem.TYPE_GROUP, toSiteRef(siteId)))
                    .filter(Objects::nonNull)
                    .forEach(forumItems::add);
    	}
    }
    catch(Exception iue)
    {
    	log.error(iue.getMessage(), iue);
    }

//    Iterator iter = membershipItems.iterator();
//    while (iter.hasNext())
//    {
//      DBMembershipItem membershipItem = (DBMembershipItem) iter.next();
//      if (membershipItem.getType().equals(DBMembershipItem.TYPE_ROLE)
//          && membershipItem.getName().equals(getCurrentUserRole()))
//      {
//        forumItems.add(membershipItem);
//      }
//      if (membershipItem.getType().equals(DBMembershipItem.TYPE_GROUP)
//          && isGroupMember(membershipItem.getName()))
//      {
//        forumItems.add(membershipItem);
//      }
//    }
    return forumItems.iterator();
  }

  public Set getForumItemsSet(DiscussionForum forum)
  {
		if (threadLocalManager.get("message_center_permission_set") == null || !((Boolean)threadLocalManager.get("message_center_permission_set")).booleanValue())
		{
			// OWLTODO: special case handling when the forum is new, it has no id and we can't retrieve anything from it
			// resort to current placement. If we end up having to do this in many places, consider a method for it
			String siteId = forum.getId() != null ? forumManager.getSiteIdForForum(forum) : toolManager.getCurrentPlacement().getContext();
			// OWLTODO: is this strictly necessary? it was done to avoid NPE but now getSiteIdForForum() returns empty string in this case
			// determine if empty string for siteId here will cause any real issues and avoid current placement if possible
			initMembershipForSite(siteId);
		}

		Set allForumSet = (Set) threadLocalManager.get("message_center_membership_forum");
		Set returnSet = new HashSet();
		Iterator iter = allForumSet.iterator();
		while(iter.hasNext())
		{
			DBMembershipItemImpl thisItem = (DBMembershipItemImpl)iter.next();
			if(thisItem.getForum() != null && forum.getId() != null && forum.getId().equals(thisItem.getForum().getId()))
			{
				returnSet.add((DBMembershipItem)thisItem);
			}
		}

		return returnSet;
  }
  
  private Iterator getTopicItemsByCurrentUser(DiscussionTopic topic){
	  return getTopicItemsByUser(topic, getCurrentUserId());
  }
  
  private Iterator getTopicItemsByUser(DiscussionTopic topic, String userId){
	  return getTopicItemsByUser(topic, userId, forumManager.getSiteIdForTopic(topic));
  }
  
  private Iterator getTopicItemsByUser(DiscussionTopic topic, String userId, String siteId)
  {
	  return getTopicItemsByUser(topic.getId(), userId, siteId);
  }
  
  private Iterator<DBMembershipItem> getTopicItemsByUser(Long topicId, String userId, String siteId)
  {
	  List<DBMembershipItem> topicItems = new ArrayList<>();
    
		if (threadLocalManager.get("message_center_permission_set") == null || !((Boolean)threadLocalManager.get("message_center_permission_set")).booleanValue())
		{
			initMembershipForSite(siteId, userId);
		}

		Set topicItemsInThread = (Set) threadLocalManager.get("message_center_membership_topic");
		Set thisTopicItemSet = new HashSet();
		Iterator iter = topicItemsInThread.iterator();
		while(iter.hasNext())
		{
			DBMembershipItemImpl thisItem = (DBMembershipItemImpl)iter.next();
			if(thisItem.getTopic() != null && topicId.equals(thisItem.getTopic().getId()))
			{
				thisTopicItemSet.add((DBMembershipItem)thisItem);
			}
		}
    
//    Set membershipItems = topic.getMembershipItemSet();
    DBMembershipItem item = forumManager.getDBMember(thisTopicItemSet, getUserRole(siteId, userId),
        DBMembershipItem.TYPE_ROLE, "/site/" + siteId);

    if (item != null){
      topicItems.add(item);
    }

    //for group awareness
    try {
    	Site currentSite = siteService.getSite(siteId);
    	Set<String> groups = getGroupsWithMember(currentSite, userId);
    	if (groups != null) {
            groups.stream().map(currentSite::getGroup)
                    .map(g -> forumManager.getDBMember(thisTopicItemSet, g.getTitle(), DBMembershipItem.TYPE_GROUP, "/site/" + siteId))
                    .filter(Objects::nonNull)
                    .forEach(topicItems::add);
    	}
    }
    catch(Exception iue)
    {
    	log.error(iue.getMessage(), iue);
    }
    
//    Iterator iter = membershipItems.iterator();
//    while (iter.hasNext())
//    {
//      DBMembershipItem membershipItem = (DBMembershipItem) iter.next();
//      if (membershipItem.getType().equals(DBMembershipItem.TYPE_ROLE)
//          && membershipItem.getName().equals(getCurrentUserRole()))
//      {
//        topicItems.add(membershipItem);
//      }
//      if (membershipItem.getType().equals(DBMembershipItem.TYPE_GROUP)
//          && isGroupMember(membershipItem.getName()))
//      {
//        topicItems.add(membershipItem);
//      }
//    }
    return topicItems.iterator();
  }
  
  public Set getTopicItemsSet(DiscussionTopic topic)
  {
		if (threadLocalManager.get("message_center_permission_set") == null || !((Boolean)threadLocalManager.get("message_center_permission_set")).booleanValue())
		{
			// OWLTODO: when creating a topic this is called with a topic that has not yet been persisted and therefore doesn't have
			// a hierarchy yet, or even an id. We have to support this scenario by using the current placement...
			// for the create topic workflow we have to permission check early, and we might need an overload for this method
			// need to check usages, there may be other workflows that involve topics that are not yet persisted
			// I suspect there is "create" code that works with unpersisted objects and regular code that call the same methods
			// and in the past relied on the current placement to get site ids...this might be tricky to untangle...
			// perhaps in the create topic scenario this method doesn't actually do anything? need to confirm...
			String topicSiteId = topic.getId() == null ? toolManager.getCurrentPlacement().getContext() : forumManager.getSiteIdForTopic(topic);
			initMembershipForSite(topicSiteId);
		}

		Set allTopicSet = (Set) threadLocalManager.get("message_center_membership_topic");
		Set returnSet = new HashSet();
		Iterator iter = allTopicSet.iterator();
		while(iter.hasNext())
		{
			DBMembershipItemImpl thisItem = (DBMembershipItemImpl)iter.next();
			if(thisItem.getTopic() != null && topic.getId() != null && topic.getId().equals(thisItem.getTopic().getId()))
			{
				returnSet.add((DBMembershipItem)thisItem);
			}
		}
		
		return returnSet;
  }

  public boolean isUserDeniedByPostFirst(String userId, DiscussionTopic topic) {
    if (topic == null) {
        log.warn("topic null in isUserDeniedByPostFirst");
        return true;
    }
    return !getUsersDeniedByPostFirst(Collections.singletonList(userId), topic, topic.getMessages()).isEmpty();
  }

  @Override
  public List<String> getUsersDeniedByPostFirst(List<String> userIds, DiscussionTopic topic, List<Message> messages) {
    if (topic == null || !topic.getPostFirst()) {
        return Collections.emptyList();
    }
    Optional<DiscussionForum> forumOpt = forumManager.getDiscussionForumForTopic(topic);
    if (!forumOpt.isPresent()) {
      log.error("Unable to find the forum for topic {}. Forced to deny all users.", topic.getId());
      return userIds;
    }
	DiscussionForum forum = forumOpt.get();
    List<String> deniedUsers = new ArrayList<>();
    boolean needToPost;
    for (String userId : userIds) {
      needToPost = true;
      for (Message message : messages) {
        if(message != null && message.getCreatedBy().equals(userId) && 
            !message.getDraft() && 
            ((message.getApproved() != null && message.getApproved()) || !topic.getModerated()) &&
            !message.getDeleted()){
          needToPost = false;
          break;
        }
      }
      if(needToPost && !(isChangeSettings(topic, forum, userId)
           || isPostToGradebook(topic, forum, userId)
           || isModeratePostings(topic, forum, userId))){
        deniedUsers.add(userId);
      }
    }
    return deniedUsers;
  }
  
  /**
   * @see org.sakaiproject.api.app.messageforums.ui.DiscussionForumManager#isInstructor()
   */
  public boolean isInstructor(String siteId)
  {
	  return isInstructor(userDirectoryService.getCurrentUser(), siteId);
  }

  /**
   * Check if the given user has site.upd access
   * 
   * @param user
   * @return
   */
  private boolean isInstructor(String userId, String siteId)
  {
	  try
	  {
		  return isInstructor(userDirectoryService.getUser(userId), siteId);
	  }
	  catch (UserNotDefinedException e)
	  {
		  return false;
	  }
  }

  private boolean isInstructor(User user, String siteId)
  {
    log.debug("isInstructor(User {})", user);
    if (user == null || StringUtils.isBlank(siteId))
	{
		return false;
	}

    return securityService.unlock(user, SiteService.SECURE_UPDATE_SITE, toSiteRef(siteId));
  }

  public boolean hasSiteVisit(String userId, String siteId)
  {
	  try
	  {
		  return hasSiteVisit(userDirectoryService.getUser(userId), siteId);
	  }
	  catch (UserNotDefinedException e)
	  {
		  return false;
	  }
  }

  public boolean hasSiteVisit(User user, String siteId)
  {
	if (user == null || StringUtils.isBlank(siteId))
	{
		return false;
	}

	return securityService.unlock(user, SiteService.SITE_VISIT, toSiteRef(siteId));
  }

  private String toSiteRef(String siteId)
  {
	  return "/site/" + siteId;
  }

  public void setPermissionLevelManager(
      PermissionLevelManager permissionLevelManager)
  {
    this.permissionLevelManager = permissionLevelManager;
  }
  
  /**
   * @return
   */
  private String getCurrentUserId()
  {
    log.debug("getCurrentUserId()");
    if (TestUtil.isRunningTests())
    {
      return "test-user";
    }
    if(sessionManager.getCurrentSessionUserId()==null&&getAnonRole()==true){
    	return ".anon";
    }    	

    return sessionManager.getCurrentSessionUserId();
  }

  /**
   * @return
   */  
  private String getCurrentUserRole(String siteId)
  {
	  log.debug("getCurrentUserRole()");
	  if(authzGroupService.getUserRole(getCurrentUserId(), "/site/" + siteId)==null&&sessionManager.getCurrentSessionUserId()==null&&getAnonRole(siteId)==true){
		  return ".anon";
	  }
	  return authzGroupService.getUserRole(getCurrentUserId(), "/site/" + siteId);
  }

  private String getUserRole(String siteId, String userId)
  {
    log.debug("getCurrentUserRole()");
    Map roleMap = (Map) threadLocalManager.get("message_center_user_role_map");
    if(roleMap == null){
    	roleMap = new HashMap();
    }
    String userRole = (String) roleMap.get(siteId + "-" + userId);
    if(userRole == null){
    	userRole = authzGroupService.getUserRole(userId, "/site/" + siteId);
    	roleMap.put(siteId + "-" + userId, userRole);
    	threadLocalManager.set("message_center_user_role_map", roleMap);
    }
    
    // if user role is still null at this point, check for .anon
    if(userRole == null && userId == null && getAnonRole("/site/" + siteId) == true){
        return ".anon";
    }
    
    return userRole;
  }
   
   public boolean  getAnonRole()
    {
	 return  forumManager.getAnonRole();	   
    }
   
   public boolean  getAnonRole(String contextSiteId) // this should be a site ref ie /site/id
   {
	 return  forumManager.getAnonRole(contextSiteId);	   
   }
  /**
   * @return
   */
   // OWLTODO: this was commented out and broke a lot of things, they should be fixed now and this method should be deleted, but see comment below about tests first
  /*private String getContextId()
  {
    log.debug("getContextId()");
    if (TestUtil.isRunningTests())  // OWLTODO: what does this even do? is it safe to remove this method or do we have to keep it for tests? So far it looks like tests still run fine...
    {
      return "test-context";
    }
    Placement placement = toolManager.getCurrentPlacement();
    String presentSiteId = placement.getContext();
    return presentSiteId;
  }*/

  /**
   * @return
   */
  
  
  private boolean isSuperUser(){
	  return isSuperUser(getCurrentUserId());
  }
  
  
  private boolean isSuperUser(String userId)
  {
    log.debug(" isSuperUser()");
    return securityService.isSuperUser(userId);
  }

  
  /**
   * @param topic
   * @param forum
   * @return
   */
  private boolean checkBaseConditions(DiscussionTopic topic, DiscussionForum forum){
	  return checkBaseConditions(topic, forum, getCurrentUserId(), forumManager.getSiteIdForForum(forum));
  }
  

  // OWLTODO: it is assumed that the forum belongs to the given site, add comments outlining this assumption
  // this needs javadocs in general, topic or forum can be null and it will still work as intended...
  private boolean checkBaseConditions(DiscussionTopic topic, DiscussionForum forum, String userId, String siteId)
  {
    log.debug("checkBaseConditions(DiscussionTopic {}, DiscussionForum {})", topic, forum);
    if (isSuperUser(userId))
    {
      return true;
    }

    return (forum != null && forum.getRestrictPermissionsForGroups() && isInstructorForAllowedGroup(forum.getId(), siteId, true)) ||
			(topic != null && topic.getRestrictPermissionsForGroups() && isInstructorForAllowedGroup(topic.getId(), siteId, false));
  }
  
  private void initMembershipForSite(String contextSiteId){
	  initMembershipForSite(contextSiteId, getCurrentUserId());
  }
  
  private void initMembershipForSite(String siteId, String userId)
  {
		if (threadLocalManager.get("message_center_permission_set") != null && ((Boolean)threadLocalManager.get("message_center_permission_set")).booleanValue())
		{
			return;
		}
		Area dfa = forumManager.getDiscussionForumArea(siteId);
    Set areaItems = dfa.getMembershipItemSet();
  	List forumItemsList = permissionLevelManager.getAllMembershipItemsForForumsForSite(dfa.getId());
  	List topicItemsList = permissionLevelManager.getAllMembershipItemsForTopicsForSite(dfa.getId());

  	Set forumItems = new HashSet();
  	for(Iterator i = forumItemsList.iterator(); i.hasNext();) {
  		DBMembershipItem forumItem = (DBMembershipItemImpl)i.next();
  		forumItems.add(forumItem);
  	}
  	
  	Set topicItems = new HashSet();
  	for(Iterator i = topicItemsList.iterator(); i.hasNext();) {
  		DBMembershipItem topicItem = (DBMembershipItemImpl)i.next();
  		topicItems.add(topicItem);
  	}
  
  	Set<String> groups = null;
  	try
  	{
  		Site currentSite = siteService.getSite(siteId);
  		groups = getGroupsWithMember(currentSite, userId);
  	}
    catch(IdUnusedException iue)
    {
    	log.error(iue.getMessage(), iue);
    }

   	threadLocalManager.set("message_center_current_member_groups", groups);
  	threadLocalManager.set("message_center_membership_area", areaItems);
  	threadLocalManager.set("message_center_membership_forum", forumItems);
  	threadLocalManager.set("message_center_membership_topic", topicItems);
	threadLocalManager.set("message_center_permission_set", Boolean.valueOf(true));
  }
  
  public Set<String> getGroupsWithMember(Site site, String userId){
	  String id = site.getReference() + "/" + userId;
	  Set<String> el = userGroupMembershipCache.get(id);
	  if (el == null) {
		  Collection<Group> groups = site.getGroupsWithMember(userId);
		  el = groups.stream().map(Group::getId).collect(Collectors.toSet());
		  userGroupMembershipCache.put(id, el);
	  }
	  return el;
	}
	
	public MemoryService getMemoryService() {
		return memoryService;
	}
	
	public void setMemoryService(MemoryService memoryService) {
		this.memoryService = memoryService;
	}

	@Override
	public boolean hasAccessPrivileges(DiscussionForum forum)
	{
		String userId = getCurrentUserId();
		String siteId = forumManager.getSiteIdForForum(forum);
		// OWLTODO: at this stage we can technically check if the user is even in the site, but
		// 1. virtually all requests will be for a site the user has access to, so checking prematurely is wasteful
		// 2. the final forum/topic permission checks (ie. isRead) should fail for anyone not in the site
		
		return isAdminOrInstructor(Optional.empty(), forum, userId, siteId) || hasNonInstructorAccessPrivileges(forum, userId, siteId);
	}

	private boolean hasNonInstructorAccessPrivileges(DiscussionForum forum, String userId, String siteId)
	{
		// can you change the settings for this forum? regardless of other forum settings and permissions, someone
		// with this permission needs to be able to see the forum itself (seeing topics in the forum is separate)
		if (isChangeSettings(forum, siteId))
		{
			return true;
		}

		// any user who would be able to see draft or unavailable forums has already been let in
		if (forum.getDraft() || !forum.getAvailability()) 
		{
			return false;
		}

		// at this point we have a non-draft, available forum so any user with the ability to create topics needs access
		if (isNewTopic(forum, siteId))
		{
			return true;
		}

		// if we made it this far we can access the forum itself based on its own settings, but topic settings also have to be considered
		List<DiscussionTopic> topics = (List<DiscussionTopic>) forum.getTopics(); // OWLTODO: can this be trusted? better eventually add null check at least...but what to do if null? assume no topics, or assume bug?
		// users who can access at least one topic in the forum need access
		// this also prevents access to forums with no topics, as only users with isNewTopic should be allowed, and this was checked earlier
		return topics.stream().anyMatch(t -> hasNonInstructorAccessPrivileges(t, forum, userId, siteId));
	}

	private boolean isAdminOrInstructor(Optional<DiscussionTopic> topic, DiscussionForum forum, String userId, String siteId)
	{
		return checkBaseConditions(topic.orElse(null), forum, userId, siteId) || isInstructor(userId, siteId);
	}

	@Override
	public boolean hasAccessPrivileges(DiscussionTopic topic)
	{
		Optional<DiscussionForum> forum = forumManager.getDiscussionForumForTopic(topic);
		if (forum.isPresent())
		{
			return hasAccessPrivileges(topic, forum.get());
		}

		return false;
	}

	/**
	 * Call this one if you already have the forum. This method assumes the topic belongs to the given forum.
	 * @param topic the topic to check access to
	 * @param forum the topic's forum
	 * @return true if the current user has access to the topic and forum
	 */
	@Override
	public boolean hasAccessPrivileges(DiscussionTopic topic, DiscussionForum forum)
	{
		String userId = getCurrentUserId();
		String siteId = forumManager.getSiteIdForTopic(topic);
		// OWLTODO: at this stage we can technically check if the user is even in the site, but
		// 1. virtually all requests will be for a site the user has access to, so checking prematurely is wasteful
		// 2. the assumed final topic/message permission checks (ie. isRead) should fail for anyone not in the site

		if (isAdminOrInstructor(Optional.of(topic), forum, userId, siteId))
		{
			return true;
		}

		if (!hasNonInstructorAccessPrivileges(forum, userId, siteId))
		{
			return false;
		}
		
		return hasNonInstructorAccessPrivileges(topic, forum, userId, siteId); // OWLTODO: possible optimization opportunity here because the call above may have already checked this topic's permissions (see impl)
	}

	private boolean hasNonInstructorAccessPrivileges(DiscussionTopic topic, DiscussionForum forum, String userId, String siteId)
	{
		// can you change the settings for this topic? regardless of other topic settings and permissions, someone
		// with this permission needs to be able to see the topic itself (seeing messages in the topic is separate)
		if (isChangeSettings(topic, forum, userId, siteId))
		{
			return true; // note the isChangeSettings() check covers topic owners and grants access for them always regardless of their current status in the site
		}

		// any user who would be able to see draft or unavailable topics has already been let in
		if (topic.getDraft() || !topic.getAvailability())
		{
			return false;
		}

		// at this point we have a non-draft, available topic and a user who is not a maintainer
		return isRead(topic, forum, userId, siteId) || isNewResponse(topic, forum, userId, siteId);
	}

	// this method will acquire the topic and forum from the message itself. use if you have no
	// need of the topic/forum objects after making this call, otherwise prefer the other overload
	// to avoid getting things twice.
	@Override
	public boolean hasAccessPrivileges(Message msg)
	{

		Optional<DiscussionTopic> topic = forumManager.getDiscussionTopicForMessage(msg);
		if (!topic.isPresent())
		{
			log.error("Unable to find topic for message {}", msg.getId());
			return false;
		}

		return hasAccessPrivileges(msg, topic.get());
	}

	// call this one if you already have the topic, this method assumes everything passed in matches up
	@Override
	public boolean hasAccessPrivileges(Message msg, DiscussionTopic topic)
	{
		return !hasAccessPrivileges(Collections.singletonList(msg), topic).isEmpty();
	}

	/**
	 * Checks access on multiple messages at the same time, for efficiency as many checks actually rely on the topic rather
	 * than individual messages. It is assumed that all passed in messages below to the given topic (this is NOT validated here).
	 * @param messages the messages to check access to
	 * @param topic the topic all of the messages belong to
	 * @return the message ids from the messages that the current user has access to
	 */
	@Override
	public List<Long> hasAccessPrivileges(List<Message> messages, DiscussionTopic topic)
	{
		String userId = getCurrentUserId();
		Optional<DiscussionForum> forum = forumManager.getDiscussionForumForTopic(topic);
		if (!forum.isPresent())
		{
			log.error("Can't find forum for topic {}", topic.getId());
			return Collections.emptyList();
		}
		String siteId = forumManager.getSiteIdForForum(forum.get());

		// check admin/instructor and let them in regardless of any other factors
		if (isAdminOrInstructor(Optional.of(topic), forum.get(), userId, siteId))
		{
			return messages.stream().map(Message::getId).collect(Collectors.toList());
		}

		// check prerequisite forum/topic access perms
		// OWLTODO: this call is simple but inefficient for two reasons: it gets the userid/site id again, and it does the admin/instructor check again.
		// Consider refactoring to avoid these duplicate checks but try not to make things overly complicated with tons of boolean params
		if (!hasAccessPrivileges(topic, forum.get()))
		{
			return Collections.emptyList();
		}

		// has isRead in topic - this is a recheck but they may have other access to the topic so it is required as prerequiste to everything else
		if (!isRead(topic, forum.get(), userId, siteId))
		{
			return Collections.emptyList();
		}

		if (isUserDeniedByPostFirst(userId, topic))
		{
			return Collections.emptyList();
		}

		// now that we know we have access to the forum and topic, check message-level stuff
		List<Long> allowedMessages = new ArrayList<>(messages.size());
		boolean isModerator = isModeratePostings(topic, forum.get(), userId, siteId);
		for (Message msg : messages)
		{
			// OWLTODO : thread has moved...how does this work? do services still return the full message details in this scenario? need to investigate this further. see ForumTool.threadMoved perhaps...
			// What if the message was moved to a thread you don't have access to? Does this matter for the main UI vs REST endpoints?

			if (topic.getModerated() && !BooleanUtils.toBooleanDefaultIfNull(msg.getApproved(), false) && !isModerator && !userId.equals(msg.getAuthorId()))
			{
				continue; // skip pending or denied messages you can't moderate and didn't author
			}
			allowedMessages.add(msg.getId());
		}

		return allowedMessages;
	}



}
