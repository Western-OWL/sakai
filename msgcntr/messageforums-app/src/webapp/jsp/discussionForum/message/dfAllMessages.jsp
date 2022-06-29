<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>
<%@ taglib uri="http://sakaiproject.org/jsf2/sakai" prefix="sakai" %>
<%@ taglib uri="http://sakaiproject.org/jsf/messageforums" prefix="mf" %>
<jsp:useBean id="msgs" class="org.sakaiproject.util.ResourceLoader" scope="session">
   <jsp:setProperty name="msgs" property="baseName" value="org.sakaiproject.api.app.messagecenter.bundle.Messages"/>
</jsp:useBean>

<f:view>
	<sakai:view title="Forums">
<link rel="stylesheet" href="/library/webjars/jquery-ui/1.12.1/jquery-ui.min.css" type="text/css" />
<link rel="stylesheet" href="/messageforums-tool/css/msgcntr.css" type="text/css" />
<link rel="stylesheet" href="/messageforums-tool/css/msgcntr_move_thread.css" type="text/css" />

<!-- messageforums-app/src/webapp/jsp/discussionForum/message-->

<script>includeLatestJQuery("msgcntr");</script>
<script src="/messageforums-tool/js/json2.js"></script>
<script src="/messageforums-tool/js/fluidframework-min.js"></script>
<script src="/messageforums-tool/js/Scroller.js"></script>
<script src="/messageforums-tool/js/forum.js"></script>
<script src="/messageforums-tool/js/frameAdjust.js"></script>
<script src="/messageforums-tool/js/forum_movethread.js"></script>

<script src="/messageforums-tool/js/sak-10625.js"></script>
<script src="/webcomponents/rubrics/sakai-rubrics-utils.js<h:outputText value="#{ForumTool.CDNQuery}" />"></script>
<script type="module" src="/webcomponents/rubrics/rubric-association-requirements.js<h:outputText value="#{ForumTool.CDNQuery}" />"></script>

<!--jsp/discussionForum/message/dfAllMessages.jsp-->
		<link rel="stylesheet" type="text/css" href="../../css/TableSorter.css" />
		<script>includeWebjarLibrary('jquery.tablesorter');</script>
		<script src="/messageforums-tool/js/forumTopicThreadsSorter.js"></script>
		<script>
 		jQuery(document).ready(function(){
 			//sort forum threads
 			$('#msgForum\\:messagesInHierDataTable').threadsSorter();
			//add handles to list for thread operat
			instrumentThreads('msgForum\\:messagesInHierDataTable');

			var menuLink = $('#forumsMainMenuLink');
			var menuLinkSpan = menuLink.closest('span');
			menuLinkSpan.addClass('current');
			menuLinkSpan.html(menuLink.text());

			removeMess();
 		});

        function disableMoveLink() {
            var linkid = "msgForum:df_move_message_commandLink";
            var movelink = document.getElementById(linkid);
            movelink.classList.add("disabled");
            movelink.classList.add("disabledMoveLink");
        }

        function enableMoveLink() {
            var linkid = "msgForum:df_move_message_commandLink";
            var movelink = document.getElementById(linkid);
            movelink.classList.remove("disabled");
            movelink.classList.remove("disabledMoveLink");
        }
 
        // this is  called from messageforums-app/src/java/org/sakaiproject/tool/messageforums/jsf/HierDataTableRender.java. 
        // checkbox is encoded there.  
        function enableDisableMoveThreadLink() {
            //function to check total number of CheckBoxes that are checked in a form
            //initialize total count to zero
            var totalChecked = 0;
            //get total number of CheckBoxes in form
            if (typeof document.forms['msgForum'].moveCheckbox.length === 'undefined') {
                // when there is just one checkbox moveCheckbox is not an array,  document.forms['msgForum'].moveCheckbox.length returns undefined
                if (document.forms['msgForum'].moveCheckbox.checked == true ) {
                    totalChecked += 1;
                }
            } else {
                // more than one checkbox is checked.
                var chkBoxCount = document.forms['msgForum'].moveCheckbox.length;
                //loop through each CheckBox
                for (var i = 0; i < chkBoxCount; i++) {
                    //check the state of each CheckBox
                    if (eval("document.forms['msgForum'].moveCheckbox[" + i + "].checked") == true)
                    {
                        //it's checked so increment the counter
                        totalChecked += 1;
                    }
                }
            }
            if (totalChecked >0) {
                // enable the move link
                enableMoveLink();
            }
            else {
                disableMoveLink();
            }
        
         }

 		</script>
		<h:outputText styleClass="showMoreText" style="display:none" value="#{msgs.cdfm_show_more_full_description}"  />

	<h:form id="msgForum" rendered="#{!ForumTool.selectedTopic.topic.draft || ForumTool.selectedTopic.topic.createdBy == ForumTool.userId}">
		<%@ include file="/jsp/discussionForum/menu/forumsMenu.jsp" %>
		<f:subview id="picker2">
			<%@ include file="moveThreadPicker.jsp" %>
		</f:subview>

		<div class="forumsNavBar">
			<h:panelGroup layout="block" styleClass="suppressTopicCrumbLink hideThreadCrumb">
				<%@ include file="/jsp/discussionForum/includes/crumbs/standard.jspf" %>
			</h:panelGroup>
			<%@ include file="/jsp/discussionForum/includes/topicPrevNext.jspf"%>
		</div>

		<%@ include file="/jsp/discussionForum/includes/topicHeader/singletonTopicHeaderList.jspf"%>	
 		<h:messages styleClass="sak-banner-error" id="errorMessages" rendered="#{! empty facesContext.maximumSeverity}" />
		<%@ include file="/jsp/discussionForum/includes/topicViewActions.jspf" %>

		<%--//designNote: need a rendered attribute here that will toggle the display of the table (if messages) or a textblock (class="instruction") if there are no messages--%>
		<h:outputText styleClass="sak-banner-warn" value="#{msgs.cdfm_postFirst_warning}" rendered="#{ForumTool.selectedTopic != null && ForumTool.needToPostFirst}"/>
		<h:outputText value="#{msgs.cdfm_no_messages}" rendered="#{ForumTool.selectedTopic == null || (empty ForumTool.selectedTopic.messages && !ForumTool.needToPostFirst)}"  styleClass="sak-banner-info" style="display:block"/>
		<%--//gsilver: need a rendered attribute here that will toggle the display of the table (if messages) or a textblock (class="instruction") if there are no messages--%>
		<div id="checkbox">
			<mf:hierDataTable styleClass="table-hover allMessages" id="messagesInHierDataTable" rendered="#{!empty ForumTool.messages}"  value="#{ForumTool.messages}" var="message" expanded="#{ForumTool.expanded}"
					columnClasses="attach, attach,messageTitle,messagesCountColumn,unreadCountColumn,attach,bogus,bogus">
				<h:column id="_checkbox">
				</h:column>
				<h:column id="_toggle">
					<f:facet name="header">
						<h:graphicImage value="/images/expand-collapse.gif" alt="#{msgs.expandAll}" title="#{msgs.expandAll}" />
					</f:facet>
				</h:column>
				<h:column id="_msg_subject">
					<f:facet name="header">
						<h:outputLink value="#" title="#{msgs.sort_thread}">
							<h:outputText value="#{msgs.cdfm_thread}" /> 
						</h:outputLink>
					</f:facet>
					<%-- moved--%>
					<h:panelGroup rendered="#{message.moved}">
						<h:outputText styleClass="textPanelFooter" escape="false" value="| #{message.message.title} - " />
						<h:commandLink action="#{ForumTool.processActionDisplayTopic}" id="topic_title" styleClass="title">
							<h:outputText value="#{msgs.moved}" />
							<f:param value="#{message.message.topic.id}" name="topicId"/>
							<f:param value="#{ForumTool.selectedForum.forum.id}" name="forumId"/>
						</h:commandLink>
						<h:outputText escape="false" styleClass="textPanelFooter"  value="#{message.message.topic.title}" />
					</h:panelGroup>
					<%-- NOT moved--%>
					<h:panelGroup rendered="#{!message.moved}">
						<h:outputText escape="false" value="<a id=\"#{message.message.id}\" name=\"#{message.message.id}\"></a>" />
						<%-- display deleted message linked if any child messages (not deleted)
							displays the message "this message has been deleted" if the message has been, um deleted, leaves reply children in place --%>
						<h:panelGroup styleClass="inactive firstChild" rendered="#{message.deleted && message.depth == 0 && message.childCount > 0}">
							<h:commandLink action="#{ForumTool.processActionDisplayThread}" immediate="true" title="#{msgs.cdfm_msg_deleted_label}" >
								<h:outputText value="#{msgs.cdfm_msg_deleted_label}" />
								<f:param value="#{message.message.id}" name="messageId"/>
								<f:param value="#{ForumTool.selectedTopic.topic.id}" name="topicId"/>
								<f:param value="#{ForumTool.selectedTopic.topic.baseForum.id}" name="forumId"/>
							</h:commandLink>
						</h:panelGroup>
				
						<h:panelGroup rendered="#{!message.deleted}" styleClass="firstChild">
							<%-- Rendered to view current thread only --%>
							<%--//designNote:  not sure what this controls - seems to affect all threads except the deleted, pending and denied--%>
							<h:commandLink styleClass="messagetitlelink" action="#{ForumTool.processActionDisplayThread}" immediate="true" title="#{message.message.title}"
								rendered="#{message.depth == 0}">
								<h:outputText value="#{message.message.title}" rendered="#{message.read && message.childUnread == 0 }" />
								<h:outputText styleClass="unreadMsg" value="#{message.message.title}" rendered="#{!message.read || message.childUnread > 0}"/>
								<f:param value="#{message.message.id}" name="messageId"/>
								<f:param value="#{ForumTool.selectedTopic.topic.id}" name="topicId"/>
								<f:param value="#{ForumTool.selectedTopic.topic.baseForum.id}" name="forumId"/>
							</h:commandLink>
							<h:commandLink styleClass="messagetitlelink" action="#{ForumTool.processActionDisplayMessage}" immediate="true" title=" #{message.message.title}"
								rendered="#{message.depth != 0 && !message.deleted}" >
								<h:outputText value="#{message.message.title}" rendered="#{message.read}" />
								<h:outputText styleClass="unreadMsg" value="#{message.message.title}" rendered="#{!message.read}"/>
								<f:param value="#{message.message.id}" name="messageId"/>
								<f:param value="#{ForumTool.selectedTopic.topic.id}" name="topicId"/>
								<f:param value="#{ForumTool.selectedTopic.topic.baseForum.id}" name="forumId"/>
							</h:commandLink>
							<h:outputText styleClass="messageNew" value=" #{msgs.cdfm_newflag}" rendered="#{!message.read}"/>	
							<%-- message has been submitted and is pending approval by moderator --%>
							<h:outputText value="#{msgs.cdfm_msg_pending_label}" styleClass="messagePending" rendered="#{message.msgPending}" />
							<%-- message has been submitted and has bene denied  approval by moderator --%>
							<h:outputText value="#{msgs.cdfm_msg_denied_label}"  styleClass="messageDenied"  rendered="#{message.msgDenied}" />
						</h:panelGroup>

					<%-- Rendered to view current message only --%>
						<%-- shows the message "This message has been deleted" if the message has been deleted --%>
						<h:outputText styleClass="inactive firstChild" rendered="#{message.deleted && (message.depth != 0 || message.childCount == 0)}" value="#{msgs.cdfm_msg_deleted_label}" />
					<%--  thread metadata (count) --%>
					</h:panelGroup>
				</h:column>
				<h:column id="_messageCount">
					<f:facet name="header">
						<h:outputLink value="#" title="#{msgs.sort_num_msgs}">
							<h:outputText value="#{msgs.cdfm_total}" />
						</h:outputLink>
					</f:facet>
					<%-- // display singular ('message') if total message is 1--%>
					<h:outputText id="topic_msg_count58" value="#{message.childCount + 1} #{msgs.cdfm_lowercase_msg}"
						rendered="#{message.depth == 0 && message.childCount ==0}" />
					<%-- // display singular ('message') if total message is 0 or more than 1--%>
					<h:outputText id="topic_msg_count59" value="#{message.childCount + 1} #{msgs.cdfm_lowercase_msgs}"
						rendered="#{message.depth == 0 && message.childCount >=1}"  />
				</h:column>
				<h:column id="_unreadMessageCount">
					<f:facet name="header">
						<h:outputLink value="#" title="#{msgs.sort_num_unread}">
							<h:outputText value="#{msgs.cdfm_uppercase_unread_msg}" />
						</h:outputLink>
					</f:facet>
					<%-- // display  ('unread ') if unread message is>= 1 --%>
					<h:outputText styleClass="badge-new childrenNew childrenNewNumber" id="topic_msg_count55" value="#{(message.childUnread) + (message.read ? 0 : 1)} #{msgs.cdfm_lowercase_unread_msg}"
						rendered="#{message.depth == 0 && ((message.childUnread) + (message.read ? 0 : 1)) >= 1}"/>
					<%-- // display ('unread ') with different style sheet if unread message is 0 --%>  
					<h:outputText id="topic_msg_count57" value="  #{(message.childUnread) + (message.read ? 0 : 1)} #{msgs.cdfm_lowercase_unread_msg}" 
						rendered="#{message.depth == 0 && ((message.childUnread) + (message.read ? 0 : 1)) == 0}"/> 
				</h:column>
					
				<%-- mark as unread column --%>
				<h:column rendered="#{ForumTool.selectedTopic.isMarkAsRead}">
					<f:facet name="header"><h:outputText value="#{msgs.cdfm_mark_as_read}" escape="false"/></f:facet>
					<h:outputLink rendered="#{!message.read}" value="javascript:void(0);" title="#{msgs.cdfm_mark_as_read}" styleClass="markAsReadIcon button"
						onclick="doAjax(#{message.message.id}, #{ForumTool.selectedTopic.topic.id}, this);">
						<h:outputText value="#{msgs.cdfm_mark_as_read}"/>
					</h:outputLink>
				</h:column>
				<%-- author column --%>
				<h:column>
					<f:facet name="header">
					<h:outputLink value="#" title="#{msgs.sort_author}">
						<h:outputText value="#{msgs.cdfm_authoredby}" />
					</h:outputLink>
					</f:facet>
					<h:panelGroup rendered="#{!message.deleted}" >
						<h:outputText value="#{message.anonAwareAuthor}" rendered="#{!ForumTool.instructor || message.useAnonymousId}" styleClass="#{message.useAnonymousId ? 'anonymousAuthor' : ''}" />
						<h:outputText value=" #{msgs.cdfm_me}" rendered="#{message.currentUserAndAnonymous}" />
						<h:commandLink action="#{mfStatisticsBean.processActionStatisticsUser}" immediate="true" title=" #{message.anonAwareAuthor}" rendered="#{ForumTool.instructor && !message.useAnonymousId}" styleClass="#{message.useAnonymousId ? 'anonymousAuthor' : ''}">
							<f:param value="#{message.authorEid}" name="siteUserId"/>
							<h:outputText value="#{message.anonAwareAuthor}" />
						</h:commandLink>
					</h:panelGroup>
				</h:column>
				<%-- date column --%>
				<h:column>
					<f:facet name="header">
					<h:outputLink value="#" title="#{msgs.sort_date}">
						<h:outputText value="#{msgs.cdfm_date}" />
					</h:outputLink>
					</f:facet>
					<h:panelGroup rendered="#{!message.deleted}" >
						<h:outputText styleClass="#{!message.read ? 'unreadMsg' : ''}" value="#{message.message.created}">
							<f:convertDateTime pattern="#{msgs.date_format}" timeZone="#{ForumTool.userTimeZone}" locale="#{ForumTool.userLocale}"/>
						</h:outputText>
					</h:panelGroup>
				</h:column> 
			</mf:hierDataTable>
		</div>
		<div class="forumsTableFooter">
			<h:panelGroup layout="block" styleClass="post_move_links" rendered="#{ForumTool.selectedTopic.isMovePostings}">
				<%-- hidden link to call ForumTool.processMoveThread  --%>
				<h:commandLink value="" action="#{ForumTool.processMoveThread}" id="hidden_move_message_commandLink" ></h:commandLink>
				<h:commandLink value="" action="$('.topic-picker').dialog('close');" id="hidden_close_move_thread" ></h:commandLink>

				<%-- link for Move Thread(s)  --%>
				<a class="button display-topic-picker disabled disabledMoveLink" id="msgForum:df_move_message_commandLink" onclick="resizeFrameForDialog();" href="#">
					<h:outputText value="#{msgs.move_thread}" />
				</a>
			</h:panelGroup>
			<%@ include file="/jsp/discussionForum/includes/topicPrevNext.jspf" %>
		</div>

	<input type="hidden" id="selectedTopicid" name="selectedTopicid" class="selectedTopicid" value="0" />
	<input type="hidden" id="moveReminder" name="moveReminder" class="moveReminder" value="false" />
	<h:inputHidden id="mainOrForumOrTopic" value="dfAllMessages" />
		<%
  String thisId = request.getParameter("panel");
  if (thisId == null) 
  {
    thisId = "Main" + org.sakaiproject.tool.cover.ToolManager.getCurrentPlacement().getId();
  }
%>
	<script>
		function resize(){
			mySetMainFrameHeight('<%= org.sakaiproject.util.Web.escapeJavascript(thisId)%>');
		}
	</script> 
	<h:outputText escape="false" value="<script>$(document).ready(function() {setupLongDesc()});</script>"  rendered="#{!ForumTool.showShortDescription}"/>
	</h:form>

	<h:outputText value="#{msgs.cdfm_insufficient_privileges_view_topic}" rendered="#{ForumTool.selectedTopic.topic.draft && ForumTool.selectedTopic.topic.createdBy != ForumTool.userId}" />
		
</sakai:view>
</f:view>
