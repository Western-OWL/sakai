/**********************************************************************************
 * $URL: https://source.sakaiproject.org/svn/msgcntr/trunk/messageforums-api/src/java/org/sakaiproject/api/app/messageforums/ui/UIPermissionsManager.java $
 * $Id: UIPermissionsManager.java 9227 2006-05-15 15:02:42Z cwen@iupui.edu $
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
package org.sakaiproject.api.app.messageforums.ui;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.sakaiproject.api.app.messageforums.DiscussionForum;
import org.sakaiproject.api.app.messageforums.DiscussionTopic;
import org.sakaiproject.api.app.messageforums.Area;
import org.sakaiproject.api.app.messageforums.Message;
import org.sakaiproject.api.app.messageforums.Topic;

/**
 * @author <a href="mailto:rshastri@iupui.edu">Rashmi Shastri</a>
 */
public interface UIPermissionsManager
{

  /**
   * @return
   */
  public boolean isNewForum();  // OWLTODO: probably safe, but double check it once everything else is fixed, it assumes current placement
  
  /**
   * @return
   */
  public boolean isChangeSettings(DiscussionForum forum);
  
  /**     
   * @param forum
   * @return
   */
  public boolean isNewTopic(DiscussionForum forum);

  /**
   * @param topic
   * @return
   */
  public boolean isNewResponse(DiscussionTopic topic, DiscussionForum forum);
  
  /**
   * 
   * @param topic
   * @param forum
   * @param userId
   * @param contextId
   * @return
   */
  // OWLTODO: find callers of this and other methods here that accept a siteid
  // if not all callers are deriving site id from the forum,
  // check the method implementation and if it is blindly trusting the siteid, we should
  // either validate (wasteful because we need to derive site id from forum and also string compare)
  // or remove the method from the public api to force use of overloads that derive the siteid
  // leave the method in place but private, because passing the siteid is still good for performance reasons,
  // as a scenario where deriving siteid requires a db lookup will be expensive
  public boolean isNewResponse(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId);

  /**
   * @param topic
   * @return
   */
  public boolean isNewResponseToResponse(DiscussionTopic topic, DiscussionForum forum);
  
  /**
   * 
   * @param topic
   * @param forum
   * @param userId
   * @param contextId
   * @return
   */
  public boolean isNewResponseToResponse(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId);

  /**
   * @param topic
   * @return
   */
  public boolean isMovePostings(DiscussionTopic topic, DiscussionForum forum);

  /**
   * @param topic
   * @return
   */
  public boolean isChangeSettings(DiscussionTopic topic, DiscussionForum forum);
  
  /**
   * 
   * @param topic
   * @param forum
   * @param userId
   * @return
   */
  public boolean isChangeSettings(DiscussionTopic topic, DiscussionForum forum, String userId);

  /**
   * @param topic
   * @return
   */
  public boolean isPostToGradebook(DiscussionTopic topic, DiscussionForum forum);
  public boolean isPostToGradebook(DiscussionTopic topic, DiscussionForum forum, String userId);
  public boolean isPostToGradebook(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId);

  /**
   * @param topic
   * @return
   */
  public boolean isRead(DiscussionTopic topic, DiscussionForum forum );
  public boolean isRead(DiscussionTopic topic, DiscussionForum forum, String userId);
  public boolean isRead(DiscussionTopic topic, DiscussionForum forum, String userId, String siteContextId);
  public boolean isRead(Long topicId, Boolean isTopicDraft, Boolean isForumDraft, String userId, String siteContextId);

  /**
   * @param topic
   * @return
   */
  public boolean isReviseAny(DiscussionTopic topic, DiscussionForum forum);
  
  /**
   * 
   * @param topic
   * @param forum
   * @param userId
   * @param contextId
   * @return
   */
  public boolean isReviseAny(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId);

  /**
   * @param topic
   * @return
   */
  public boolean isReviseOwn(DiscussionTopic topic, DiscussionForum forum);

  /**
   * 
   * @param topic
   * @param forum
   * @param userId
   * @param contextId
   * @return
   */
  public boolean isReviseOwn(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId);
  
  /**
   * @param topic
   * @return
   */
  public boolean isDeleteAny(DiscussionTopic topic, DiscussionForum forum);
  
  /**
   * 
   * @param topic
   * @param forum
   * @param userId
   * @param contextId
   * @return
   */
  public boolean isDeleteAny(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId);

  /**
   * @param topic
   * @return
   */
  public boolean isDeleteOwn(DiscussionTopic topic, DiscussionForum forum);
  
  /**
   * 
   * @param topic
   * @param forum
   * @param userId
   * @param contextId
   * @return
   */
  public boolean isDeleteOwn(DiscussionTopic topic, DiscussionForum forum, String userId, String contextId);

  /**
   * @param topic
   * @return
   */
  public boolean isMarkAsRead(DiscussionTopic topic, DiscussionForum forum);
  
  /**
   * Returns whether current user has perm to moderate in this situation
   * @param topic
   * @param forum
   * @return
   */
  public boolean isModeratePostings(Long topicId, Boolean isForumLocked, Boolean isForumDraft, Boolean isTopicLocked, Boolean isTopicDraft, String userId, String siteId);
  public boolean isModeratePostings(DiscussionTopic topic, DiscussionForum forum, String userId, String siteId);
  public boolean isModeratePostings(DiscussionTopic topic, DiscussionForum forum, String userId);
  public boolean isModeratePostings(DiscussionTopic topic, DiscussionForum forum);

  /**
   * Returns whether current user has perm to identify anonymous users
   */
  public boolean isIdentifyAnonAuthors(Topic topic);
  
  /**
   * Returns list of current user's memberships 
   * (role + groups/sections) 
   * @return
   */
  //public List getCurrentUserMemberships(); // OWLTODO: remove
  public List getCurrentUserMemberships(String siteId);
  
  public Set getAreaItemsSet(Area area);

  public Set getForumItemsSet(DiscussionForum forum);
  
  public Set getTopicItemsSet(DiscussionTopic topic);

  /**
   * Returns whether this user cannot view messages specifically because the topic is 'post first' and they have not yet posted.
   * @param userId
   * @param topic the result will be false unless this topic is 'post first'.
   * @return users who cannot view messages if they have not posted and this topic is 'post first'
   */
  public boolean isUserDeniedByPostFirst(String userId, DiscussionTopic topic);

  /**
   * Given a list of users, a topic, and its messages, returns the list of users who cannot view messages messages specifically because the topic is 'post first' and they have not yet posted.
   * @param userIds return value will be a subset of this list.
   * @param topic the result will be empty unless this topic is 'post first'.
   * @param messages all messages within this topic.
   * @return users who cannot view messages if they have not posted and this topic is 'post first'
   */
  public List<String> getUsersDeniedByPostFirst(List<String> userIds, DiscussionTopic topic, List<Message> messages);

  public boolean hasAccessPrivileges(DiscussionForum forum);

  // Having access to the parent forum is a requirement that is automatically also checked by these methods
  // Note also that if these methods return true, it does not necessarily imply they have read access to any message in the topic,
  // only that they have the permission to see the topic itself, perhaps only to change its settings or create a new message
  public boolean hasAccessPrivileges(DiscussionTopic topic);
  public boolean hasAccessPrivileges(DiscussionTopic topic, DiscussionForum forum);

  // Having access to the parent forum and topic is a requirement that is automatically also checked by these methods
  public boolean hasAccessPrivileges(Message msg);
  public boolean hasAccessPrivileges(Message msg, DiscussionTopic topic, DiscussionForum forum);
  public List<Long> hasAccessPrivileges(List<Message> messages, DiscussionTopic topic);
}
