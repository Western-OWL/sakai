/**********************************************************************************
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.api.app.messageforums.Area;
import org.sakaiproject.api.app.messageforums.BulkPermission;
import org.sakaiproject.api.app.messageforums.DBMembershipItem;
import org.sakaiproject.api.app.messageforums.DiscussionForum;
import org.sakaiproject.api.app.messageforums.DiscussionTopic;
import org.sakaiproject.api.app.messageforums.MembershipItem;
import org.sakaiproject.api.app.messageforums.PermissionLevelManager;
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
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.sakaiproject.api.app.messageforums.Message;
import org.sakaiproject.user.api.UserNotDefinedException;

/**
 * @author <a href="mailto:rshastri@iupui.edu">Rashmi Shastri</a>
 */
@Slf4j
public class UIPermissionsManagerImpl implements UIPermissionsManager {

    private static final Predicate<DBMembershipItem> ifChangeSettings = item -> item.getPermissionLevel().getChangeSettings();
    private static final Predicate<DBMembershipItem> ifDeleteAny = item -> item.getPermissionLevel().getDeleteAny();
    private static final Predicate<DBMembershipItem> ifDeleteOwn = item -> item.getPermissionLevel().getDeleteOwn();
    private static final Predicate<DBMembershipItem> ifMarkAsRead = item -> item.getPermissionLevel().getMarkAsRead();
    private static final Predicate<DBMembershipItem> ifModeratePostings = item -> item.getPermissionLevel().getModeratePostings();
    private static final Predicate<DBMembershipItem> ifMovePosting = item -> item.getPermissionLevel().getMovePosting();
    private static final Predicate<DBMembershipItem> ifNewResponse = item -> item.getPermissionLevel().getNewResponse();
    private static final Predicate<DBMembershipItem> ifNewResponseToResponse = item -> item.getPermissionLevel().getNewResponseToResponse();
    private static final Predicate<DBMembershipItem> ifPostToGradebook = item -> item.getPermissionLevel().getPostToGradebook();
    private static final Predicate<DBMembershipItem> ifRead = i -> i.getPermissionLevel().getRead();
    private static final Predicate<DBMembershipItem> ifReviseAny = item -> item.getPermissionLevel().getReviseAny();
    private static final Predicate<DBMembershipItem> ifReviseOwn = item -> item.getPermissionLevel().getReviseOwn();


    @Setter private AuthzGroupService authzGroupService;
    @Setter private DiscussionForumManager forumManager;
    @Setter private MemoryService memoryService;
    @Setter private PermissionLevelManager permissionLevelManager;
    @Setter private SecurityService securityService;
    @Setter private SessionManager sessionManager;
    @Setter private SiteService siteService;
    @Setter private ToolManager toolManager;
    @Setter private UserDirectoryService userDirectoryService;

    private Cache<String, Set<DBMembershipItem>> membershipItemCache;
    private Cache<String, Set<String>> userGroupMembershipCache;

    public void init() {
        log.info("init()");
        userGroupMembershipCache = memoryService.getCache("org.sakaiproject.component.app.messageforums.ui.UIPermissionsManagerImpl.userGroupMembershipCache");
        membershipItemCache = memoryService.getCache("org.sakaiproject.component.app.messageforums.ui.UIPermissionsManagerImpl.membershipItemCache");
        forumManager.setUiPermissionsManager(this);
    }

    @Override
    public boolean isNewForum() {
        if (isSuperUser()) return true;

        Predicate<DBMembershipItem> ifNewForum = item -> item.getPermissionLevel().getNewForum();
        return getAreaItemsByCurrentUser().stream().anyMatch(ifNewForum);
    }

    @Override
    public boolean isChangeSettings(DiscussionForum forum) {
        return isChangeSettings(forum, forumManager.getSiteIdForForum(forum));
    }

    /**
   * Determines if the current user is allowed to change forum settings.
   * This method is private because it trusts the forum and siteid match, do not call this without first validating this is true
   * @param forum the forum in question
   * @param siteId the site the forum belongs to
   * @return true if the user is admin/instructor/owner, or has change settings permission
   */
    private boolean isChangeSettings(DiscussionForum forum, String siteId) {
        if (isSuperUser()) return true;
        // if restricted or instructor belongs to group or is forum owner
        if (isInstructor(siteId)
                && (!forum.getRestrictPermissionsForGroups()
                || isInstructorForAllowedGroup(forum.getId(), true, siteId, getCurrentUserId())
                || forumManager.isForumOwner(forum, getCurrentUserId(), siteId))) { // this allows a brand new forum object that doesn't even have an id or area yet to pass this check
            return true;
        }

        return getForumItemsByCurrentUser(forum).stream().anyMatch(ifChangeSettings);
    }

    private boolean isInstructorForAllowedGroup(Long forumId, boolean isForum, String siteId, String userId) {
        if (forumId == null || !isInstructor(siteId)) return false;

        List<String> groupTitle;
        if (isForum) {
            groupTitle = forumManager.getAllowedGroupForRestrictedForum(forumId, PermissionLevelManager.PERMISSION_LEVEL_NAME_CONTRIBUTOR);
        } else {
            groupTitle = forumManager.getAllowedGroupForRestrictedTopic(forumId, PermissionLevelManager.PERMISSION_LEVEL_NAME_CONTRIBUTOR);
        }
        try {
            Site site = siteService.getSite(siteId);
            Set<String> groups = getGroupsWithMember(site, userId);
            return groups.stream().map(site::getGroup).anyMatch(g -> groupTitle.contains(g.getTitle()));
        } catch (IdUnusedException iue) {
            log.warn("Could not fetch site {}, {}", siteId, iue.toString());
        }
        return false;
    }

    @Override
    public boolean isNewTopic(DiscussionForum forum) {
		return isNewTopic(forum, forumManager.getSiteIdForForum(forum));
	}

	private boolean isNewTopic(DiscussionForum forum, String siteId) {
        if (isSuperUser()) return true;
        if (isInstructor(siteId)
                && forum.getRestrictPermissionsForGroups()
                && isInstructorForAllowedGroup(forum.getId(), true, siteId, getCurrentUserId())) {
            return true;
        }
        Predicate<DBMembershipItem> ifNewTopic = item -> item.getPermissionLevel().getNewTopic();
        return getForumItemsByCurrentUser(forum).stream().anyMatch(ifNewTopic);
    }

    @Override
    public boolean isNewResponse(DiscussionTopic topic, DiscussionForum forum) {
        return isNewResponse(topic, forum, getCurrentUserId(), forumManager.getSiteIdForForum(forum));
    }

    @Override
    public boolean isNewResponse(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId) {
        if (checkBaseConditions(topic, forum, userId, contextId)) return true;

        if (forum != null
                && !forum.getDraft()
                && !forum.getLocked()
                && !topic.getDraft()
                && !topic.getLocked()) {
            return getTopicItemsByUser(topic, userId, contextId).stream().anyMatch(ifNewResponse);
        }
        return false;
    }

    @Override
    public boolean isNewResponseToResponse(DiscussionTopic topic, DiscussionForum forum) {
        return isNewResponseToResponse(topic, forum, getCurrentUserId(), forumManager.getSiteIdForForum(forum));
    }

    @Override
    public boolean isNewResponseToResponse(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId) {
        if (checkBaseConditions(topic, forum, userId, contextId)) return true;

        if (forum != null
                && !forum.getDraft()
                && !forum.getLocked()
                && !topic.getDraft()
                && !topic.getLocked()) {
            return getTopicItemsByUser(topic, userId, contextId).stream().anyMatch(ifNewResponseToResponse);
        }
        return false;
    }

    @Override
    public boolean isMovePostings(DiscussionTopic topic, DiscussionForum forum) {
        if (checkBaseConditions(topic, forum)) return true;

        if (forum != null
                && !forum.getDraft()
                && !forum.getLocked()
                && !topic.getDraft()
                && !topic.getLocked()) {

            return getTopicItemsByCurrentUser(topic).stream().anyMatch(ifMovePosting.or(ifReviseAny).or(ifReviseOwn));
        }
        return false;
    }

    @Override
    public boolean isChangeSettings(DiscussionTopic topic, DiscussionForum forum) {
        return isChangeSettings(topic, forum, getCurrentUserId());
    }

    @Override
    public boolean isChangeSettings(DiscussionTopic topic, DiscussionForum forum, String userId) {
		return isChangeSettings(topic, forum, userId, forumManager.getSiteIdForForum(forum));
	}

	private boolean isChangeSettings(DiscussionTopic topic, DiscussionForum forum, String userId, String siteId) {
        if (isSuperUser(userId)) return true;

		// as a failsafe, make sure the topic belongs to the forum, as we likely used the forum to get the site id
		// we will derive the forum from the topic to check it. this is overall not the most efficient, but we're playing it safe
		// this could be eliminated if all paths are shown to have already validated the topic/forum connection,
		// but then we'd also have to trust that any future callers also will validate
		Optional<DiscussionForum> topicForum = forumManager.getDiscussionForumForTopic(topic);
		if (!topicForum.isPresent() || !forum.getId().equals(topicForum.get().getId()))
		{
			log.error("Given topic {} does not belong to given forum {}", topic.getId(), forum.getId());
			return false;
		}

        if (isInstructor(siteId)
                && ((!forum.getRestrictPermissionsForGroups() && !topic.getRestrictPermissionsForGroups())
                || (forum.getRestrictPermissionsForGroups() && isInstructorForAllowedGroup(forum.getId(), true, siteId, userId))
                || (topic.getRestrictPermissionsForGroups() && isInstructorForAllowedGroup(topic.getId(), false, siteId, userId)))) {
            return true;
        }
        // if owner then allow change of settings on the topic or on forum.
        if (forumManager.isTopicOwner(topic, userId)) return true;
        return getTopicItemsByUser(topic, userId).stream().anyMatch(ifChangeSettings);
    }

    @Override
    public boolean isPostToGradebook(DiscussionTopic topic, DiscussionForum forum) {
        return isPostToGradebook(topic, forum, getCurrentUserId());
    }

    @Override
    public boolean isPostToGradebook(DiscussionTopic topic, DiscussionForum forum, String userId) {
        return isPostToGradebook(topic, forum, userId, forumManager.getSiteIdForForum(forum));
    }

    @Override
    public boolean isPostToGradebook(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId) {
        if (checkBaseConditions(topic, forum, userId, contextId)) return true;

        if (!forum.getDraft() && !topic.getDraft()) {
            return getTopicItemsByUser(topic, userId, contextId).stream().anyMatch(ifPostToGradebook);
        }
        return false;
    }

    @Override
    public boolean isRead(DiscussionTopic topic, DiscussionForum forum) {
        return isRead(topic, forum, getCurrentUserId());
    }

    @Override
    public boolean isRead(DiscussionTopic topic, DiscussionForum forum, String userId) {
        return isRead(topic, forum, userId, forumManager.getSiteIdForForum(forum));
    }

    @Override
    public boolean isRead(DiscussionTopic topic, DiscussionForum forum, String userId, String siteId) {

        if (userId == null) {
            userId = sessionManager.getCurrentSessionUserId();
        }

        if (checkBaseConditions(topic, forum, userId, siteId)) return true;
        if (forum.getDraft() || topic.getDraft()) return false;

        List<DBMembershipItem> items = getTopicItemsByUser(topic, userId, siteId);
        return items.stream().anyMatch(ifRead);
    }

    @Override
    public boolean isRead(Long topicId, Boolean isTopicDraft, Boolean isForumDraft, String userId, String siteId) {
        if (checkBaseConditions(null, null, userId, siteId)) return true;
        if (isForumDraft || isTopicDraft) return false;

        DiscussionTopic topic = forumManager.getTopicById(topicId);
        return getTopicItemsByUser(topic, userId, siteId).stream().anyMatch(ifRead);
    }

    @Override
    public boolean isReviseAny(DiscussionTopic topic, DiscussionForum forum) {
        return isReviseAny(topic, forum, getCurrentUserId(), forumManager.getSiteIdForForum(forum));
    }

    @Override
    public boolean isReviseAny(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId) {
        if (checkBaseConditions(topic, forum, userId, contextId)) return true;

        return (forum.getDraft() == null || !forum.getDraft())
                && (forum.getLocked() == null || !forum.getLocked())
                && (topic.getDraft() == null || !topic.getDraft())
                && (topic.getLocked() == null || !topic.getLocked())
                && getTopicItemsByUser(topic, userId, contextId).stream().anyMatch(ifReviseAny);
    }

    @Override
    public boolean isReviseOwn(DiscussionTopic topic, DiscussionForum forum) {
        return isReviseOwn(topic, forum, getCurrentUserId(), forumManager.getSiteIdForForum(forum));
    }

    @Override
    public boolean isReviseOwn(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId) {
        if (checkBaseConditions(topic, forum, userId, contextId)) return true;

        if (topic.getLocked() == null || topic.getLocked()) return false;

        if (!forum.getDraft()
                && !forum.getLocked()
                && !topic.getDraft()
                && !topic.getLocked()) {
            return getTopicItemsByUser(topic, userId, contextId).stream().anyMatch(ifReviseOwn);
        }
        return false;
    }

    @Override
    public boolean isDeleteAny(DiscussionTopic topic, DiscussionForum forum) {
        return isDeleteAny(topic, forum, getCurrentUserId(), forumManager.getSiteIdForForum(forum));
    }

    @Override
    public boolean isDeleteAny(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId) {
        if (checkBaseConditions(topic, forum, userId, contextId)) return true;

        if (topic.getLocked() == null || topic.getLocked()) return false;

        if (!forum.getDraft()
                && !forum.getLocked()
                && !topic.getDraft()
                && !topic.getLocked()) {
            return getTopicItemsByUser(topic, userId, contextId).stream().anyMatch(ifDeleteAny);
        }
        return false;
    }

    @Override
    public boolean isDeleteOwn(DiscussionTopic topic, DiscussionForum forum) {
        return isDeleteOwn(topic, forum, getCurrentUserId(), forumManager.getSiteIdForForum(forum));
    }

    @Override
    public boolean isDeleteOwn(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId) {
        if (checkBaseConditions(topic, forum, userId, contextId)) return true;

        if (topic.getLocked() == null || topic.getLocked().equals(Boolean.TRUE)) return false;

        if (!forum.getDraft()
                && !forum.getLocked()
                && !topic.getDraft()
                && !topic.getLocked()) {
            return getTopicItemsByUser(topic, userId, contextId).stream().anyMatch(ifDeleteOwn);
        }
        return false;
    }

    @Override
    public boolean isMarkAsRead(DiscussionTopic topic, DiscussionForum forum) {
        if (checkBaseConditions(topic, forum)) return true;

        if (topic.getLocked() == null || topic.getLocked().equals(Boolean.TRUE)) return false;

        if (!forum.getDraft()
                && !forum.getLocked()
                && !topic.getDraft()
                && !topic.getLocked()) {
            return getTopicItemsByCurrentUser(topic).stream().anyMatch(ifMarkAsRead);
        }
        return false;

    }

    @Override
    public boolean isModeratePostings(DiscussionTopic topic, DiscussionForum forum) {
        return isModeratePostings(topic, forum, getCurrentUserId());
    }

    @Override
    public boolean isModeratePostings(DiscussionTopic topic, DiscussionForum forum, String userId) {
        return isModeratePostings(topic, forum, userId, forumManager.getSiteIdForForum(forum));
    }

    @Override
    public boolean isModeratePostings(DiscussionTopic topic, DiscussionForum forum, String userId, String siteId) {
        // NOTE: the forum or topic being locked should not affect a user's ability to moderate,
        // so logic related to the locked status was removed
        if (checkBaseConditions(topic, forum, userId, "/site/" + siteId)) return true;

        return ((forum.getDraft() == null || !forum.getDraft())
                && (topic.getDraft() == null || !topic.getDraft())
                && getTopicItemsByUser(topic, userId, siteId).stream().anyMatch(ifModeratePostings));
    }

    public boolean isModeratePostings(Long topicId, Boolean isForumLocked, Boolean isForumDraft, Boolean isTopicLocked, Boolean isTopicDraft, String userId, String siteId) {
        if (checkBaseConditions(null, null, userId, siteId)) return true;
        DiscussionTopic topic = forumManager.getTopicById(topicId);
        return !isForumDraft && !isTopicDraft && getTopicItemsByUser(topic, userId, siteId).stream().anyMatch(ifModeratePostings);
    }

    @Override
    public boolean isIdentifyAnonAuthors(DiscussionTopic topic) {
        String currentUserId = getCurrentUserId();

        if (isSuperUser(currentUserId)) return true;

        Predicate<DBMembershipItem> ifIdentifyANonAuthors = i -> i.getPermissionLevel().getIdentifyAnonAuthors();
        return getTopicItemsByUser(topic, currentUserId, forumManager.getSiteIdForTopic(topic)).stream().anyMatch(ifIdentifyANonAuthors);
    }

    @Override
    public List<String> getCurrentUserMemberships(String siteId) {
        List<String> userMemberships = new ArrayList<>();
        // first, add the user's role
        String currentUserRole = getCurrentUserRole(siteId);
        if (StringUtils.isNotBlank(currentUserRole)) {
            userMemberships.add(currentUserRole);
        }
        // now, add any groups the user is a member of
        try {
            Site site = siteService.getSite(siteId);
            Set<String> groups = getGroupsWithMember(site, getCurrentUserId());
            groups.stream().map(site::getGroup).filter(Objects::nonNull).map(Group::getTitle).forEach(userMemberships::add);
        } catch (IdUnusedException iue) {
            log.warn("Could not fetch site {}, {}", siteId, iue.toString());
        }

        return userMemberships;
    }

    private List<DBMembershipItem> getAreaItemsByCurrentUser() {
        List<DBMembershipItem> areaItems = new ArrayList<>();

		// this method is called from isNewForum() which figures out if you can create forums in the site...
		// this appears to be one situation where we don't have any site reference available from a forums object,
		// so we have to rely on getCurrentPlacement()
		String siteId = toolManager.getCurrentPlacement().getContext();

        Set<DBMembershipItem> areaMemberships = getAreaMemberships(siteId);
        areaItems.add(forumManager.getDBMember(areaMemberships, getCurrentUserRole(siteId), MembershipItem.TYPE_ROLE, toSiteRef(siteId)));

        // for group awareness
        try {
            Site currentSite = siteService.getSite(siteId);
            getGroupsWithMember(currentSite, getCurrentUserId()).stream().map(currentSite::getGroup)
                    .map(g -> forumManager.getDBMember(areaMemberships, g.getTitle(), MembershipItem.TYPE_GROUP, toSiteRef(siteId)))
                    .forEach(areaItems::add);
        } catch (IdUnusedException iue) {
            log.warn("Could not fetch site {}, {}", siteId, iue.toString());
        }

        return areaItems;
    }

    @Override
    public Set<DBMembershipItem> getAreaItemsSet(Area area) {
        Set<DBMembershipItem> areaItems = new HashSet<>();
        Set<DBMembershipItem> allAreaSet = getAreaMemberships(area.getContextId());

        Predicate<DBMembershipItem> ifSameArea = item -> ((DBMembershipItemImpl) item).getArea() != null
                && area.getId() != null
                && area.getId().equals(((DBMembershipItemImpl) item).getArea().getId());
        allAreaSet.stream().filter(ifSameArea).forEach(areaItems::add);
        return areaItems;
    }

    private List<DBMembershipItem> getForumItemsByCurrentUser(DiscussionForum forum) {
        List<DBMembershipItem> forumItems = new ArrayList<>();

		String siteId = forumManager.getSiteIdForForum(forum);

        Set<DBMembershipItem> forumItemsInThread = getForumMemberships(forum.getArea());
        Set<DBMembershipItem> thisForumItemSet = new HashSet<>();

        Predicate<DBMembershipItem> ifSameForum = item -> ((DBMembershipItemImpl)item).getForum() != null
                && forum.getId() != null
                && forum.getId().equals(((DBMembershipItemImpl)item).getForum().getId());
        forumItemsInThread.stream().filter(ifSameForum).forEach(thisForumItemSet::add);

        if (thisForumItemSet.isEmpty() && forum.getTopicsSet() == null && ".anon".equals(forum.getCreatedBy()) && getAnonRole(toSiteRef(siteId))) {
            forum.getMembershipItemSet().stream().filter(item -> ".anon".equals(item.getName())).forEach(thisForumItemSet::add);
        }

        forumItems.add(forumManager.getDBMember(thisForumItemSet, getCurrentUserRole(siteId), MembershipItem.TYPE_ROLE, toSiteRef(siteId)));

        //  for group awareness
        try {
            Site site = siteService.getSite(siteId);
            Set<String> groups = getGroupsWithMember(site, getCurrentUserId());

            if (groups != null) {
                groups.stream().map(site::getGroup)
                        .map(g -> forumManager.getDBMember(thisForumItemSet, g.getTitle(), MembershipItem.TYPE_GROUP, toSiteRef(siteId)))
                        .filter(Objects::nonNull)
                        .forEach(forumItems::add);
            }
        } catch (IdUnusedException iue) {
            log.warn("Could not fetch site {} when attempting to add group information for forum {}, {}", siteId, forum.getId(), iue.toString());
        }
        return forumItems;
    }

    public Set<DBMembershipItem> getForumItemsSet(DiscussionForum forum) {
        Set<DBMembershipItem> forumItems = new HashSet<>();
        Set<DBMembershipItem> allForumSet = getForumMemberships(forum.getArea());
        Predicate<DBMembershipItem> ifSameForum = item -> ((DBMembershipItemImpl) item).getForum() != null
                && forum.getId() != null
                && forum.getId().equals(((DBMembershipItemImpl) item).getForum().getId());
        allForumSet.stream().filter(ifSameForum).forEach(forumItems::add);
        return forumItems;
    }

    private Area getTopicForumArea(DiscussionTopic topic) {
        return topic.getBaseForum() != null ? topic.getBaseForum().getArea() : topic.getOpenForum().getArea();
    }
    
    private List<DBMembershipItem> getTopicItemsByCurrentUser(DiscussionTopic topic) {
        return getTopicItemsByUser(topic, getCurrentUserId());
    }

    private List<DBMembershipItem> getTopicItemsByUser(DiscussionTopic topic, String userId) {
        return getTopicItemsByUser(topic, userId, forumManager.getSiteIdForTopic(topic));
    }

    private List<DBMembershipItem> getTopicItemsByUser(DiscussionTopic topic, String userId, String siteId) {
        List<DBMembershipItem> topicItems = new ArrayList<>();

        Set<DBMembershipItem> topicItemsInThread = getTopicMemberships(getTopicForumArea(topic));
        Set<DBMembershipItem> thisTopicItemSet = new HashSet<>();

        Predicate<DBMembershipItem> ifTopicIsNonNullAndEqualsTopicId = item -> ((DBMembershipItemImpl) item).getTopic() != null
                && ((DBMembershipItemImpl) item).getTopic().getId().equals(topic.getId());
        topicItemsInThread.stream().filter(ifTopicIsNonNullAndEqualsTopicId).forEach(thisTopicItemSet::add);

        topicItems.add(forumManager.getDBMember(thisTopicItemSet, getUserRole(siteId, userId), MembershipItem.TYPE_ROLE, "/site/" + siteId));

        //for group awareness
        try {
            Site currentSite = siteService.getSite(siteId);
            Set<String> groups = getGroupsWithMember(currentSite, userId);
            if (groups != null) {
                groups.stream().map(currentSite::getGroup)
                        .map(g -> forumManager.getDBMember(thisTopicItemSet, g.getTitle(), MembershipItem.TYPE_GROUP, "/site/" + siteId))
                        .filter(Objects::nonNull)
                        .forEach(topicItems::add);
            }
        } catch (Exception iue) {
            log.warn("Could not fetch site {} when attempting to add group information for topic {}, {}", siteId, topic.getId(), iue.toString());
        }

        return topicItems;
    }



    @Override
    public Set<DBMembershipItem> getTopicItemsSet(DiscussionTopic topic) {
        Set<DBMembershipItem> topicItems = new HashSet<>();
        Set<DBMembershipItem> allTopicSet = getTopicMemberships(getTopicForumArea(topic));
        Predicate<DBMembershipItem> ifSameTopic = item -> ((DBMembershipItemImpl) item).getTopic() != null
                && topic.getId() != null
                && topic.getId().equals(((DBMembershipItemImpl) item).getTopic().getId());
        allTopicSet.stream().filter(ifSameTopic).forEach(topicItems::add);
        return topicItems;
    }

    @Override
    public BulkPermission getBulkPermissions(DiscussionTopic topic, DiscussionForum forum) {
        BulkPermission permission = new BulkPermission();

        String userId = getCurrentUserId();
        String siteId = forumManager.getSiteIdForForum(forum);

        boolean ifBaseConditions = checkBaseConditions(topic, forum, userId, toSiteRef(siteId));
        if (ifBaseConditions) {
            permission.setAllPermissions(true);
            return permission;
        }

        boolean ifTopicOwner = topic != null && forumManager.isTopicOwner(topic, userId);
        boolean ifLockedTopic = topic != null && (topic.getLocked() == null || topic.getLocked());
        boolean ifLockedForum = forum != null && (forum.getLocked() == null || forum.getLocked());
        boolean ifDraftTopic = topic != null && (topic.getDraft() != null && topic.getDraft());
        boolean ifDraftForum = forum != null && (forum.getDraft() != null && forum.getDraft());

        Collection<DBMembershipItem> topicItemsByUser = getTopicItemsByUser(topic, userId);

        permission.setChangeSettings(ifTopicOwner || topicItemsByUser.stream().anyMatch(ifChangeSettings));
        permission.setDeleteAny(!ifLockedTopic && !ifLockedForum && !ifDraftForum && !ifDraftTopic && topicItemsByUser.stream().anyMatch(ifDeleteAny));
        permission.setDeleteOwn(!ifLockedTopic && !ifLockedForum && !ifDraftForum && !ifDraftTopic && topicItemsByUser.stream().anyMatch(ifDeleteOwn));
        permission.setMarkAsRead(!ifLockedTopic && !ifLockedForum && !ifDraftForum && !ifDraftTopic && topicItemsByUser.stream().anyMatch(ifMarkAsRead));
        permission.setModeratePostings(!ifDraftForum && !ifDraftTopic && topicItemsByUser.stream().anyMatch(ifModeratePostings));
        permission.setMovePostings(!ifLockedTopic && !ifLockedForum && !ifDraftForum && !ifDraftTopic && topicItemsByUser.stream().anyMatch(ifMovePosting));
        permission.setNewResponse(!ifLockedTopic && !ifLockedForum && !ifDraftForum && !ifDraftTopic && topicItemsByUser.stream().anyMatch(ifNewResponse));
        permission.setNewResponseToResponse(!ifLockedTopic && !ifLockedForum && !ifDraftForum && !ifDraftTopic && topicItemsByUser.stream().anyMatch(ifNewResponseToResponse));
        permission.setPostToGradebook(!ifDraftForum && !ifDraftTopic && topicItemsByUser.stream().anyMatch(ifPostToGradebook));
        permission.setRead(!ifDraftForum && !ifDraftTopic && topicItemsByUser.stream().anyMatch(ifRead));
        permission.setReviseAny(!ifLockedTopic && !ifLockedForum && !ifDraftForum && !ifDraftTopic && topicItemsByUser.stream().anyMatch(ifReviseAny));
        permission.setReviseOwn(!ifLockedTopic && (!ifLockedForum && !ifDraftForum && !ifDraftTopic) && topicItemsByUser.stream().anyMatch(ifReviseOwn));

        return permission;
    }

    @Override
    public BulkPermission getBulkPermissions(DiscussionForum forum) {
        BulkPermission permission = new BulkPermission();
        permission.setChangeSettings(isChangeSettings(forum));
        permission.setNewTopic(isNewTopic(forum));
        return permission;
    }

	@Override
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

    public boolean isInstructor(String siteId) {
        return isInstructor(userDirectoryService.getCurrentUser(), siteId);
    }

    private boolean isInstructor(User user, String siteId) {
        if (user != null && StringUtils.isNotBlank(siteId)) return securityService.unlock(user, SiteService.SECURE_UPDATE_SITE, toSiteRef(siteId));
        return false;
    }

	@Override
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

	@Override
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

    private String getCurrentUserId() {
        if (TestUtil.isRunningTests()) return "test-user";
        String userId = sessionManager.getCurrentSessionUserId();
        if (StringUtils.isBlank(userId) && getAnonRole()) return ".anon";
        return userId;
    }

    private String getCurrentUserRole(String siteId) {
        if (authzGroupService.getUserRole(getCurrentUserId(), "/site/" + siteId) == null
                && sessionManager.getCurrentSessionUserId() == null
                && getAnonRole(siteId)) {
            return ".anon";
        }
        return authzGroupService.getUserRole(getCurrentUserId(), "/site/" + siteId);
    }

    private String getUserRole(String siteId, String userId) {
        String userRole = authzGroupService.getUserRole(userId, "/site/" + siteId);

        // if user role is still null at this point, check for .anon
        if (userRole == null && userId == null && getAnonRole("/site/" + siteId)) {
            return ".anon";
        }

        return userRole;
    }

    public boolean getAnonRole() {
        return forumManager.getAnonRole();
    }

    public boolean getAnonRole(String contextSiteId) {
        return forumManager.getAnonRole(contextSiteId);
    }

    private boolean isSuperUser() {
        return isSuperUser(getCurrentUserId());
    }


    private boolean isSuperUser(String userId) {
        return securityService.isSuperUser(userId);
    }


    private boolean checkBaseConditions(DiscussionTopic topic, DiscussionForum forum) {
        return checkBaseConditions(topic, forum, getCurrentUserId(), forumManager.getSiteIdForForum(forum));
    }

	/**
	 * Checks the "base conditions" for access. Returns true if the user is an admin or, in the case where the given
	 * topic or forum is group restricted, the user is an instructor in an allowed group. If both topic/forum are null,
	 * this is just an admin check.
	 * @param topic the topic to check group restriction, may be null
	 * @param forum the forum to check group restriction, may be null (assumed to contain the topic)
	 * @param userId the user
	 * @param siteId the site the topic/forum belong to (assumed to be accurate)
	 * @return true if the given user meets the conditions
	 */
    private boolean checkBaseConditions(DiscussionTopic topic, DiscussionForum forum, String userId, String siteId) {
        if (isSuperUser(userId)) return true;

        // if restricted and belongs to group
        return (forum != null && forum.getRestrictPermissionsForGroups() && isInstructorForAllowedGroup(forum.getId(), true, siteId, userId))
                || (topic != null && topic.getRestrictPermissionsForGroups() && isInstructorForAllowedGroup(topic.getId(), false, siteId, userId));
    }

    public void clearMembershipsFromCacheForArea(Area area) {
        if (area == null || area.getId() == null) return;
        String areaId = area.getId().toString();
        membershipItemCache.remove("area_" + areaId);
        membershipItemCache.remove("forum_" + areaId);
        membershipItemCache.remove("topic_" + areaId);
    }

    private Set<DBMembershipItem> getTopicMemberships(Area area) {
        if (area == null) return Collections.emptySet();
        String topicCacheKey = "topic_" + area.getId();
        Set<DBMembershipItem> cachedTopicMemberships = membershipItemCache.get(topicCacheKey);
        if (cachedTopicMemberships == null) {
            cachedTopicMemberships = new HashSet<>(permissionLevelManager.getAllMembershipItemsForTopicsForSite(area.getId()));
            membershipItemCache.put(topicCacheKey, cachedTopicMemberships);
        }
        return cachedTopicMemberships;
    }

    private Set<DBMembershipItem> getForumMemberships(Area area) {
        if (area == null) return Collections.emptySet();
        String forumCacheKey = "forum_" + area.getId();
        Set<DBMembershipItem> cachedForumMemberships = membershipItemCache.get(forumCacheKey);
        if (cachedForumMemberships == null) {
            cachedForumMemberships = new HashSet<>(permissionLevelManager.getAllMembershipItemsForForumsForSite(area.getId()));
            membershipItemCache.put(forumCacheKey, cachedForumMemberships);
        }
        return cachedForumMemberships;
    }

    private Set<DBMembershipItem> getAreaMemberships(String siteId) {
        if (StringUtils.isNotBlank(siteId)) {
            Area area = forumManager.getDiscussionForumArea(siteId);
            if (area != null) {
                String areaSiteCacheKey = "area_" + area.getId();
                Set<DBMembershipItem> cachedAreaMemberships = membershipItemCache.get(areaSiteCacheKey);
                if (cachedAreaMemberships == null) {
                    cachedAreaMemberships = area.getMembershipItemSet();
                    membershipItemCache.put(areaSiteCacheKey, cachedAreaMemberships);
                }
                return cachedAreaMemberships;
            }
        }
        return Collections.emptySet();
    }

    public Set<String> getGroupsWithMember(Site site, String userId) {
        Set<String> groupIds = new HashSet<>();
        if (site != null && StringUtils.isNotBlank(userId)) {
            String cacheKey = site.getReference() + "/" + userId;
            Set<String> cachedGroupIds = userGroupMembershipCache.get(cacheKey);
            if (cachedGroupIds == null) {
                Collection<Group> groups = site.getGroupsWithMember(userId);
                groupIds = groups.stream().map(Group::getId).collect(Collectors.toSet());
                userGroupMembershipCache.put(cacheKey, Collections.unmodifiableSet(groupIds));
            } else {
                groupIds.addAll(cachedGroupIds);
            }
        }
        return groupIds;
    }

	@Override
	public boolean hasAccessPrivileges(DiscussionForum forum)
	{
		String userId = getCurrentUserId();
		String siteId = forumManager.getSiteIdForForum(forum);
		// at this stage we can technically check if the user is even in the site, but
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
		List<DiscussionTopic> topics = forum.getTopics() == null ? Collections.emptyList() : (List<DiscussionTopic>) forum.getTopics();
		// users who can access at least one topic in the forum need access
		// this also prevents access to forums with no topics, as only users with isNewTopic should be allowed, and this was checked earlier
		return topics.stream().anyMatch(t -> hasNonInstructorAccessPrivileges(t, forum, userId, siteId));
	}

	private boolean isAdminOrInstructor(Optional<DiscussionTopic> topic, DiscussionForum forum, String userId, String siteId)
	{
		if (checkBaseConditions(topic.orElse(null), forum, userId, siteId))
		{
			return true;
		}
		try
		{
			return isInstructor(userDirectoryService.getUser(userId), siteId);
		}
		catch (UserNotDefinedException e)
		{
			return false;
		}
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
		// at this stage we can technically check if the user is even in the site, but
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

		return hasNonInstructorAccessPrivileges(topic, forum, userId, siteId); // possible optimization opportunity here because the call above may have already checked this topic's permissions (see impl)
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
		// this call is simple but inefficient for two reasons: it gets the userid/site id again, and it does the admin/instructor check again.
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
			if (topic.getModerated() && !BooleanUtils.toBooleanDefaultIfNull(msg.getApproved(), false) && !isModerator && !userId.equals(msg.getAuthorId()))
			{
				continue; // skip pending or denied messages you can't moderate and didn't author
			}
			allowedMessages.add(msg.getId());
		}

		return allowedMessages;
	}
}
