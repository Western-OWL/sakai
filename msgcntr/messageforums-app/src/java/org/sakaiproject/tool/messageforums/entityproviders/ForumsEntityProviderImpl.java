/**
 * Copyright (c) 2003-2017 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.tool.messageforums.entityproviders;

import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.sakaiproject.api.app.messageforums.AnonymousManager;

import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.api.app.messageforums.Attachment;
import org.sakaiproject.api.app.messageforums.BaseForum;
import org.sakaiproject.api.app.messageforums.DiscussionForum;
import org.sakaiproject.api.app.messageforums.DiscussionTopic;
import org.sakaiproject.api.app.messageforums.Message;
import org.sakaiproject.api.app.messageforums.OpenForum;
import org.sakaiproject.api.app.messageforums.PrivateForum;
import org.sakaiproject.api.app.messageforums.Topic;
import org.sakaiproject.api.app.messageforums.ui.DiscussionForumManager;
import org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.*;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.tool.messageforums.entityproviders.sparsepojos.*;
import org.sakaiproject.tool.messageforums.entityproviders.utils.MessageUtils;

/**
 * Provides the forums entity provider. 
 * 
 * @author Adrian Fish <adrian.r.fish@gmail.com>
 */
@Slf4j
public class ForumsEntityProviderImpl extends AbstractEntityProvider implements Outputable, AutoRegisterEntityProvider, ActionsExecutable, Describeable {

	public final static String ENTITY_PREFIX = "forums";
	public static int DEFAULT_NUM_MESSAGES = 3;
	
	@Setter
	protected DiscussionForumManager forumManager;
	
	@Setter
	protected UIPermissionsManager uiPermissionsManager;
	
	@Setter
	protected SiteService siteService;
	
	@Setter
	protected ToolManager toolManager;

	@Setter
	private UserDirectoryService userDirectoryService;

	@Setter
	private SecurityService securityService;

	@Setter
	private AnonymousManager anonymousManager;

	public String getEntityPrefix() {
		return ENTITY_PREFIX;
	}
	
	public String[] getHandledOutputFormats() {
		return new String[] {Formats.JSON,Formats.XML};
	}

	/**
	 * This handles the paths:
	 * 
	 * /direct/forums/site/SITEID.json
	 * /direct/forums/site/SITEID/forum/FORUMID.json
	 * /direct/forums/site/SITEID/forum/FORUMID/topic/TOPICID.json
     * /direct/forums/site/SITEID/forum/FORUMID/topic/TOPICID/message/MESSAGEID.json
	 * 
	 * @param view
	 * @param params
	 * @return
	 */
	@EntityCustomAction(action="site",viewKey=EntityView.VIEW_LIST)
    public Object handleSite(EntityView view, Map<String, Object> params) {
		
		if(log.isDebugEnabled()) {
			log.debug("handleSite");
		}
		
		String userId = developerHelperService.getCurrentUserId();
		
		if(userId == null) {
			log.error("Not logged in");
			throw new EntityException("You must be logged in to retrieve fora.","",HttpServletResponse.SC_UNAUTHORIZED);
		}
		
        String siteId = view.getPathSegment(2);
        
        if(siteId == null) {
			log.error("Bad request. No SITEID supplied on path.");
        	throw new EntityException("Bad request: To get the fora in a site you need a url like '/direct/forum/site/SITEID.json'"
        									,"",HttpServletResponse.SC_BAD_REQUEST);
        }
        
        checkSiteAndToolAccess(siteId);
        
        String[] pathSegments = view.getPathSegments();
        
        if(pathSegments.length == 3) {
        	// This is a request for all the fora in the site
        	return getAllForaForSite(siteId,userId);
        } else if(pathSegments.length == 5) {
        	// This is a request for a particular forum in the site
        	Long forumId = -1L;
		
        	try {
        		forumId = Long.parseLong(view.getPathSegment(4));
        	} catch(NumberFormatException nfe) {
        		log.error("Bad request. FORUMID must be an integer.");
        		throw new EntityException("The forum id must be an integer.","",HttpServletResponse.SC_BAD_REQUEST);
        	}
        	return getForum(forumId,siteId,userId);
        } else if(pathSegments.length == 7) {
        	// This is a request for a particular topic in the forum
        	Long topicId = -1L;
		
        	try {
        		topicId = Long.parseLong(view.getPathSegment(6));
        	} catch(NumberFormatException nfe) {
        		log.error("Bad request. TOPICID must be an integer.");
        		throw new EntityException("The topic id must be an integer.","",HttpServletResponse.SC_BAD_REQUEST);
        	}
        	return getTopic(topicId,siteId,userId);
        } else if(pathSegments.length == 9) {
        	Long messageId = -1L;
    		
    		try {
    			messageId = Long.parseLong(view.getPathSegment(8));
    		} catch(NumberFormatException nfe) {
        		log.error("Bad request. MESSAGEID must be an integer.");
    			throw new EntityException("The message id must be an integer.","",HttpServletResponse.SC_BAD_REQUEST);
    		}
    		
    		return getMessage(messageId,siteId,userId);
        } else {
        	return null;
        }
	}
	
	private List<?> getAllForaForSite(String siteId,String userId) {
		
		if(log.isDebugEnabled()) {
			log.debug("getAllForaForSite(" + siteId + "," + userId + ")");
		}
		
		List<SparsestForum> sparseFora = new ArrayList<SparsestForum>();
		
		List<DiscussionForum> fatFora = forumManager.getDiscussionForumsWithTopics(siteId);
		
		for(DiscussionForum fatForum : fatFora) {
			
			if( ! checkAccess(fatForum,userId,siteId)) {
				log.warn("Access denied for user id '" + userId + "' to forum '" + fatForum.getId()
							+ "'. This forum will not be returned.");
				continue;
			}
				
			List<Long> topicIds = new ArrayList<Long>();
			for(Topic topic : (List<Topic>) fatForum.getTopics()) {
				topicIds.add(topic.getId());
			}
			
			List<Object[]> topicTotals = forumManager.getMessageCountsForMainPage(topicIds);
			List<Object[]> topicReadTotals = forumManager.getReadMessageCountsForMainPage(topicIds);
		
			SparsestForum sparseForum = new SparsestForum(fatForum,developerHelperService);
			
			int totalForumMessages = 0;
			for(Object[] topicTotal : topicTotals) {
				totalForumMessages += ((Long) topicTotal[1]).intValue();
			}
			sparseForum.setTotalMessages(totalForumMessages);
			
			int totalForumReadMessages = 0;
			for(Object[] topicReadTotal : topicReadTotals) {
				totalForumReadMessages += ((Long) topicReadTotal[1]).intValue();
			}
			sparseForum.setReadMessages(totalForumReadMessages);
		
			sparseFora.add(sparseForum);
		}
		
		return sparseFora;
	}

	private boolean isInstructor(String userId, DiscussionForum forum)
	{
		String realSiteId = forumManager.getSiteIdForForum(forum);
		return forumManager.isInstructor(userId, realSiteId);
	}

	private boolean isInstructor(String userId, DiscussionTopic topic)
	{
		String realSiteId = forumManager.getSiteIdForTopic(topic);
		return forumManager.isInstructor(userId, realSiteId);
	}

	private void sanitizeSparseTopic(SparsestTopic sparseTopic, String userId)
	{
		if (!userId.equals(sparseTopic.getCreator()))
		{
			sparseTopic.setCreator("");
		}
		if (!userId.equals(sparseTopic.getModifier()))
		{
			sparseTopic.setModifier("");
		}
	}

	private void sanitizeSparseMessage(SparseMessage message, String userId, boolean anon, String siteId, boolean isInstructor, boolean swapForDisplayName)
	{
		String authorId = message.getAuthorId();
		String author = message.getAuthoredBy();
		String creatorId = message.getCreatedBy();
		String modifierId = message.getModifiedBy();

		String newAuthorId = isInstructor || userId.equals(authorId) ? authorId : "";
		String newAuthor = anon ? swapForDisplayName(authorId, true, siteId) : author;
		String newCreator = swapForDisplayName ? swapForDisplayName(creatorId, anon, siteId) : (isInstructor || userId.equals(creatorId)? creatorId : "");
		String newModifier = swapForDisplayName ? swapForDisplayName(modifierId, anon, siteId) : (isInstructor || userId.equals(modifierId) ? modifierId : "");

		message.setAuthorId(newAuthorId);
		message.setAuthoredBy(newAuthor);
		message.setCreatedBy(newCreator);
		message.setModifiedBy(newModifier);
	}

	// returns the display name or anon id for the given user id
	private String swapForDisplayName(String uuid, boolean anon, String siteId)
	{
		String name = "";
		if (anon)
		{
			name = anonymousManager.getAnonId(siteId, uuid);
		}
		else
		{
			try
			{
				name =  userDirectoryService.getUser(uuid).getDisplayName();
			}
			catch (UserNotDefinedException e)
			{
				log.debug("User not defined for id '{}'.", uuid);
			}
		}

		return name;
	}
	
	/**
	 * This will return a SparseForum populated down to the topics with their
	 * attachments.
	 */
	private Object getForum(Long forumId, String siteId, String userId) {
		
		if(log.isDebugEnabled()) {
			log.debug("getForum(" + forumId + "," + siteId + "," + userId + ")");
		}
		
		DiscussionForum fatForum = forumManager.getForumByIdWithTopicsAttachmentsAndMessages(forumId);
		boolean isInstructor = isInstructor(userId, fatForum);
		
		if(checkAccess(fatForum,userId,siteId)) {
			
			SparseForum sparseForum = new SparseForum(fatForum,developerHelperService);
			if (!isInstructor && !userId.equals(sparseForum.getCreator()))
			{
				sparseForum.setCreator("");
			}
			if (!isInstructor && !userId.equals(sparseForum.getModifier()))
			{
				sparseForum.setModifier("");
			}
			
			List<DiscussionTopic> fatTopics = (List<DiscussionTopic>) fatForum.getTopics();
			
			// Gather all the topic ids so we can make the minimum number
			// of calls for the message counts.
			List<Long> topicIds = new ArrayList<Long>();
			for(DiscussionTopic topic : fatTopics) {
				topicIds.add(topic.getId());
			}
				
			List<Object[]> topicTotals = forumManager.getMessageCountsForMainPage(topicIds);
			List<Object[]> topicReadTotals = forumManager.getReadMessageCountsForMainPage(topicIds);
			
			int totalForumMessages = 0;
			for(Object[] topicTotal : topicTotals) {
				totalForumMessages += ((Long) topicTotal[1]).intValue();
			}
			sparseForum.setTotalMessages(totalForumMessages);
				
			int totalForumReadMessages = 0;
			for(Object[] topicReadTotal : topicReadTotals) {
				totalForumReadMessages += ((Long) topicReadTotal[1]).intValue();
			}
			sparseForum.setReadMessages(totalForumReadMessages);
			
			// Reduce the fat topics to sparse topics while setting the total and read
			// counts. A SparseTopic will only be created if the currrent user has access.
			List<SparsestTopic> sparseTopics = new ArrayList<SparsestTopic>();
			for(DiscussionTopic fatTopic : fatTopics) {
				
				// Only add this topic to the list if the current user has read permission
				if( ! uiPermissionsManager.hasAccessPrivileges(fatTopic, fatForum)) {
					// No read permission, skip this topic.
					continue;
				}
				
				SparsestTopic sparseTopic = new SparsestTopic(fatTopic);
				for(Object[] topicTotal : topicTotals) {
					if(topicTotal[0].equals(sparseTopic.getId())) {
						sparseTopic.setTotalMessages(((Long)topicTotal[1]).intValue());
					}
				}
				for(Object[] topicReadTotal : topicReadTotals) {
					if(topicReadTotal[0].equals(sparseTopic.getId())) {
						sparseTopic.setReadMessages(((Long)topicReadTotal[1]).intValue());
					}
				}
				
				List<SparseAttachment> attachments = new ArrayList<SparseAttachment>();
				for(Attachment attachment : (List<Attachment>) fatTopic.getAttachments()) {
					String url = developerHelperService.getServerURL() + "/access/content" + attachment.getAttachmentId();
					attachments.add(new SparseAttachment(attachment.getAttachmentName(),url));
				}
				sparseTopic.setAttachments(attachments);

				if (!isInstructor)
				{
					sanitizeSparseTopic(sparseTopic, userId);
				}
				
				sparseTopics.add(sparseTopic);
			}
			
			sparseForum.setTopics(sparseTopics);
			
			return sparseForum;
		} else {
			log.error("Not authorised to access forum '" + forumId + "'");
			throw new EntityException("You are not authorised to access this forum.","",HttpServletResponse.SC_UNAUTHORIZED);
		}
	}
	
	private Object getTopic(Long topicId,String siteId,String userId) {
		
		if(log.isDebugEnabled()) {
			log.debug("getTopic(" + topicId + "," + siteId + "," + userId + ")");
		}
		
		// This call gets the attachments for the messages but not the topic. Unexpected, yes. Cool, not.
		DiscussionTopic fatTopic = (DiscussionTopic)forumManager.getTopicByIdWithMessagesAndAttachments(topicId);
		
		if(!uiPermissionsManager.hasAccessPrivileges(fatTopic)) {
			log.error("'" + userId + "' is not authorised to read topic '" + topicId + "'.");
			throw new EntityException("You are not authorised to read this topic.","",HttpServletResponse.SC_UNAUTHORIZED);
		}
		
		SparseTopic sparseTopic = new SparseTopic(fatTopic);

		boolean isInstructor = isInstructor(userId, fatTopic);
		boolean isAnon = anonymousManager.displayAnonIdsToUser(userId, fatTopic);
		if (!isInstructor)
		{
			sanitizeSparseTopic(sparseTopic, userId);
		}
		
		// Setup the total and read message counts on the topic
		List<Long> topicIds = new ArrayList<Long>();
		topicIds.add(fatTopic.getId());
		
		List<Object[]> totalCounts = forumManager.getMessageCountsForMainPage(topicIds);
		if(totalCounts.size() > 0) {
			sparseTopic.setTotalMessages(((Long) totalCounts.get(0)[1]).intValue());
		} else {
			sparseTopic.setTotalMessages(0);
		}
		
		List<Object[]> readCounts = forumManager.getReadMessageCountsForMainPage(topicIds);
		if(readCounts.size() > 0) {
			sparseTopic.setReadMessages(((Long) readCounts.get(0)[1]).intValue());
		} else {
			sparseTopic.setReadMessages(0);
		}
		
		List<SparseMessage> messages = new ArrayList<SparseMessage>();
		List<Message> fatMessages = fatTopic.getMessages();
		List<Long> allowedMessages = uiPermissionsManager.hasAccessPrivileges(fatMessages, fatTopic);
		for(Message fatMessage : fatMessages) {
			if (allowedMessages.contains(fatMessage.getId())) {
				SparseMessage sparseMessage = new SparseMessage(fatMessage,/* readStatus = */ false,/* addAttachments = */ true,developerHelperService.getServerURL());
				sanitizeSparseMessage(sparseMessage, userId, isAnon, siteId, isInstructor, false);
				messages.add(sparseMessage);
			}
		}
		
		List<SparseThread> threads = new MessageUtils().getThreadsWithCounts(messages, forumManager, userId);
		
		sparseTopic.setThreads(threads);
		
		return sparseTopic;
		
	}
	
	private Object getMessage(Long messageId,String siteId,String userId) {
		
		if(log.isDebugEnabled()) {
			log.debug("getMessage(" + messageId + "," + siteId + "," + userId + ")");
		}
		
		Message fatMessage = forumManager.getMessageById(messageId);
		
		DiscussionTopic fatTopic = (DiscussionTopic) forumManager.getTopicByIdWithMessagesAndAttachments(fatMessage.getTopic().getId());
		
		// This sets the attachments on the message.We have to do this as
        // getMessageById doesn't populate the attachments.
		setAttachments(fatMessage,fatTopic.getMessages());
		
		if(!uiPermissionsManager.hasAccessPrivileges(fatMessage) || fatMessage.getDeleted()) {
			log.error("'" + userId + "' is not authorised to read message '" + messageId + "'.");
			throw new EntityException("You are not authorised to read this message.","",HttpServletResponse.SC_UNAUTHORIZED);
		}
		
		String realSiteId = forumManager.getSiteIdForTopic(fatTopic);
		boolean isInstructor = forumManager.isInstructor(userId, realSiteId);
		boolean isAnon = anonymousManager.displayAnonIdsToUser(userId, fatTopic);

		List<SparseMessage> messages = new ArrayList<SparseMessage>();
		List<Message> fatMessages = fatTopic.getMessages();
		List<Long> allowedMessages = uiPermissionsManager.hasAccessPrivileges(fatMessages, fatTopic);
		for(Message fm : fatMessages) {
			if (allowedMessages.contains(fm.getId()))
			{
				SparseMessage sm = new SparseMessage(fm,/* readStatus =*/ false,/* addAttachments =*/ true, developerHelperService.getServerURL());
				sanitizeSparseMessage(sm, userId, isAnon, realSiteId, isInstructor, false);
				messages.add(sm);
			}
		}
		
		SparseMessage sparseThread = new SparseMessage(fatMessage,false,/* readStatus =*/ true,developerHelperService.getServerURL());
		sanitizeSparseMessage(sparseThread, userId, isAnon, realSiteId, isInstructor, false);

		new MessageUtils().attachReplies(sparseThread,messages, forumManager, userId); // this filters out the deleted replies
		
		return sparseThread;
		
	}
	
	private boolean checkAccess(BaseForum baseForum, String userId, String siteId) {
		
		if(baseForum instanceof DiscussionForum) {
			return uiPermissionsManager.hasAccessPrivileges((DiscussionForum) baseForum);
		}
		else if(baseForum instanceof PrivateForum) {
			PrivateForum pf = (PrivateForum) baseForum;
			// If the current user is the creator, return true.
			if(pf.getCreatedBy().equals(userId)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * This is a dirty hack to set the attachments on the message. There doesn't seem
	 * to be an api for getting a single message with all attachments. If you try and retrieve
	 * them after, hibernate, wonderful framework that it is, throws a lazy exception.
	 * 
	 * @param unPopulatedMessage The message we want to set attachments on
	 * @param populatedMessages The list of populated messages retrieved from the forum manager
	 */
	private void setAttachments(Message unPopulatedMessage, List<Message> populatedMessages) {
		
		for(Message populatedMessage : populatedMessages) {
			if(populatedMessage.getId().equals(unPopulatedMessage.getId())
					&& populatedMessage.getHasAttachments()) {
				unPopulatedMessage.setAttachments(populatedMessage.getAttachments());
				break;
			}
		}
	}
	
	/**
	 * Checks whether the current user can access this site and whether they can
	 * see the forums tool.
	 * 
	 * @param siteId
	 * @throws EntityException
	 */
	private void checkSiteAndToolAccess(String siteId) throws EntityException {
        
		//check user can access this site
		Site site;
		try {
			site = siteService.getSiteVisit(siteId);
		} catch (IdUnusedException e) {
			throw new EntityException("Invalid siteId: " + siteId,"", HttpServletResponse.SC_BAD_REQUEST);
		} catch (PermissionException e) {
			throw new EntityException("No access to site: " + siteId,"",HttpServletResponse.SC_UNAUTHORIZED);
		}

		//check user can access the tool, it might be hidden
		ToolConfiguration toolConfig = site.getToolForCommonId("sakai.forums");
		if(!toolManager.isVisible(site, toolConfig)) {
			throw new EntityException("No access to tool in site: " + siteId, "",HttpServletResponse.SC_UNAUTHORIZED);
		}
	}

	/**
	 * Handles requests /direct/forums/messages/SITEID.json?n=10 and returns latest threads for a site
	 * @param view
	 * @param params
	 * @return
	 */
	@EntityCustomAction(action="messages",viewKey=EntityView.VIEW_LIST)
	public Object displayMessages(EntityView view, Map<String, Object> params) {

		if(log.isDebugEnabled()) {
			log.debug("handleSite");
		}

		//get number of messages to display from the URL params, validate and set to 0 if not set or conversion fails
		int numberOfMessages = NumberUtils.toInt((String)params.get("n"), 0);
		if(numberOfMessages == 0){
			numberOfMessages = DEFAULT_NUM_MESSAGES;
		}
		String userId = developerHelperService.getCurrentUserId();

		if(userId == null) {
			log.error("Not logged in");
			throw new EntityException("You must be logged in view conversations.","",HttpServletResponse.SC_UNAUTHORIZED);
		}

		String siteId = view.getPathSegment(2);

		if(siteId == null) {
			log.error("Bad request. No SITEID supplied on path.");
			throw new EntityException("Bad request: To get the fora in a site you need a url like '/direct/forums/site/SITEID/latestmessages.json?n=10'"
					,"",HttpServletResponse.SC_BAD_REQUEST);
		}

		checkSiteAndToolAccess(siteId);
		boolean isInstructor = forumManager.isInstructor(userId, siteId);
		List<SparseMessage> messages = new ArrayList<SparseMessage>();
		//List of topicIds from all forums which are accessible to the user
		List<Long> topicIds = new ArrayList<Long>();

		//get all forums for the site
		List<DiscussionForum> forums = forumManager.getDiscussionForumsWithTopics(siteId);
		for(DiscussionForum forum : forums){
			if( ! checkAccess(forum, userId, siteId)) {
				log.debug("Access denied for user id '" + userId + "' to forum '" + forum.getId()
						+ "'. Topic ids will not be added for this forum .");
				continue;
			}

			for(Topic topic : (List<Topic>) forum.getTopics()) {
				//check if user can see this topic
				if(!uiPermissionsManager.hasAccessPrivileges((DiscussionTopic) topic, forum)) {
					//user has no permission so skip adding this topicId into the list.
					continue;
				}
				topicIds.add(topic.getId());
			}
		}
		//For given 'topicIds' fetch recently updated threads
		List<Message> recentThreads = forumManager.getRecentDiscussionForumThreadsByTopicIds(topicIds, numberOfMessages); // this filters out deleted messages
		Map<DiscussionTopic, List<Message>> partitionedThreads = new HashMap<>();
		for (Message t : recentThreads)
		{
			DiscussionTopic topic = (DiscussionTopic) t.getTopic();
			if (partitionedThreads.get(topic) == null)
			{
				partitionedThreads.put(topic, new ArrayList<>());
			}
			partitionedThreads.get(topic).add(t);
		}
		for (DiscussionTopic topic : partitionedThreads.keySet())
		{
			List<Message> topicThreads = partitionedThreads.get(topic);
			List<Long> allowedThreadIds = uiPermissionsManager.hasAccessPrivileges(topicThreads, topic);
			List<Message> filteredThreads = topicThreads.stream().filter(t -> allowedThreadIds.contains(t.getId())).collect(Collectors.toList());
			boolean isAnon = anonymousManager.displayAnonIdsToUser(userId, topic);
			for (Message fm : filteredThreads)
			{
				SparseMessage sm = new SparseMessage(fm,/* readStatus =*/ false,/* addAttachments =*/ true, developerHelperService.getServerURL());
				sanitizeSparseMessage(sm, userId, isAnon, siteId, isInstructor, true);
				sm.setForumId(fm.getTopic().getOpenForum().getId());
				messages.add(sm);
			}
		}
		return messages;
	}
}
