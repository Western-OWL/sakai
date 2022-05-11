<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>
<%@ taglib uri="http://sakaiproject.org/jsf2/sakai" prefix="sakai" %>
<%@ taglib uri="http://sakaiproject.org/jsf/messageforums" prefix="mf" %>
<jsp:useBean id="msgs" class="org.sakaiproject.util.ResourceLoader" scope="session">
   <jsp:setProperty name="msgs" property="baseName" value="org.sakaiproject.api.app.messagecenter.bundle.Messages"/>
</jsp:useBean>

<f:view>
	<sakai:view toolCssHref="/messageforums-tool/css/msgcntr.css">
		<h:form id="msgForum" styleClass="specialLink" prependId = "false">
			<h:inputHidden id="currentMessageId" value="#{ForumTool.selectedMessage.message.id}"/>
			<h:inputHidden id="currentTopicId" value="#{ForumTool.selectedTopic.topic.id}"/>
			<h:inputHidden id="currentForumId" value="#{ForumTool.selectedForum.forum.id}"/>
			<script>includeLatestJQuery("msgcntr");</script>
			<script>includeWebjarLibrary("qtip2");</script>
			<script src="/messageforums-tool/js/forum.js"></script>
			<script src="/messageforums-tool/js/sak-10625.js"></script>
			<script src="/messageforums-tool/js/messages.js"></script>
			
			<!--jsp/discussionForum/message/dfViewMessage.jsp-->
			<script>
				$(document).ready(function() {
					if ($('table.messageActions a').length==0){
						$('.messageActions').hide();
					}
						$('.permaLink').click(function(event){
							event.preventDefault();
                            var url = $(this).attr('href');
                            if (!url)
							    url = this.href;
							$('#permalinkHolder textarea').val(url);
							$('#permalinkHolder').css({
								'top': $(this).position().top,
								'left': $(this).position().left
							});
							$('#permalinkHolder').fadeIn('fast');
							$('#permalinkHolder input').focus().select();
						});
					$('#permalinkHolder .closeMe').click(function(event){
						event.preventDefault();
						$('#permalinkHolder').fadeOut('fast');
					});
					var msgBody = document.getElementById("messageBody").innerHTML;
					msgBody = msgBody.replace(/\n/g,',').replace(/\s/g,' ').replace(/  ,/g,',');
					fckeditor_word_count_fromMessage(msgBody, "counttotal");

					var menuLink = $('#forumsMainMenuLink');
					var menuLinkSpan = menuLink.closest('span');
					menuLinkSpan.addClass('current');
					menuLinkSpan.html(menuLink.text());

					});
			</script>
            <%@ include file="/jsp/discussionForum/menu/forumsMenu.jsp" %>

			<%--breadcrumb and thread nav grid--%>
			<%@ include file="/jsp/discussionForum/includes/crumbs/standard.jspf" %>
			
			<%@ include file="/jsp/discussionForum/includes/threadPrevNext.jspf"%>

			<%-- topic short description and long description --%>
			<h:panelGroup layout="block" styleClass="topicBloc topicBlocLone">
				<h:panelGroup layout="block" styleClass="textPanel">
					<h:graphicImage url="/images/silk/date_delete.png" title="#{msgs.topic_restricted_message}" alt="#{msgs.topic_restricted_message}" rendered="#{ForumTool.selectedTopic.availability == 'false'}" style="margin-right:.5em"/>
					<h:graphicImage url="/images/silk/lock.png" alt="#{msgs.cdfm_forum_locked}" 
						 rendered="#{ForumTool.selectedTopic.locked =='true'}" style="margin-right:.5em"/>
					<h:outputText value="#{ForumTool.selectedForum.forum.title} /  #{ForumTool.selectedTopic.topic.title}"  styleClass="title"/> 
				</h:panelGroup>
				<%-- link to open and close long desc. --%>
				<h:panelGroup layout="block" styleClass="textPanel" rendered="#{!empty ForumTool.selectedTopic.topic.shortDescription}">
					<h:outputText value="#{ForumTool.selectedTopic.topic.shortDescription}" />
				</h:panelGroup>
				<h:panelGroup layout="block" rendered="#{!empty ForumTool.selectedTopic.topic.extendedDescription}">
					<p id="openLinkBlock" class="toggleParent openLinkBlock display-none">
						<a href="#" id="showMessage" class="toggle show">
							<h:graphicImage url="/images/expand.gif" alt=""/>
							<h:outputText value=" #{msgs.cdfm_hide_full_description}" />
						</a>
					</p>
					<p id="hideLinkBlock" class="toggleParent hideLinkBlock">
						<a href="#" id="hideMessage" class="toggle show">
							<h:graphicImage url="/images/collapse.gif" alt="" />
							<h:outputText value=" #{msgs.cdfm_read_full_description}"/>
						</a>
					</p>
					<h:panelGroup id="fullTopicDescription" layout="block" styleClass="textPanel fullTopicDescription">
						<h:outputText escape="false" value="#{ForumTool.selectedTopic.topic.extendedDescription}" />
					</h:panelGroup>
				</h:panelGroup>
			</h:panelGroup>
			<h:messages globalOnly="true" infoClass="success" errorClass="alertMessage" rendered="#{! empty facesContext.maximumSeverity}"/>
			<h:panelGroup styleClass="margin-left:1em;">
				<h:graphicImage url="/../../library/image/silk/table_add.png" alt="#{msgs.cdfm_message_count}" />&nbsp;<h:outputText value="#{msgs.cdfm_message_count}" />:&nbsp;
				<h:panelGroup id="counttotal"></h:panelGroup>
			</h:panelGroup>
			<h:panelGrid columns="2" 
					width="100%" 
					columnClasses="specialLink, specialLink otherOtherActions"
					cellpadding="0" cellspacing="0"
					rendered="#{!ForumTool.deleteMsg && !ForumTool.selectedMessage.message.deleted}" 
					styleClass="messageActions"
					style="margin:1em 0 0 0">
				<h:panelGroup style="display:block">
					<h:commandLink styleClass="button" title="#{msgs.cdfm_button_bar_reply_to_msg}" action="#{ForumTool.processDfMsgReplyMsg}" 
							rendered="#{ForumTool.selectedTopic.isNewResponseToResponse && ForumTool.selectedMessage.msgApproved && !ForumTool.selectedTopic.locked && !ForumTool.selectedForum.locked == 'true'}">
						<h:graphicImage value="/../../library/image/silk/email_go.png" alt="#{msgs.cdfm_button_bar_reply_to_msg}" rendered="#{ForumTool.selectedTopic.isNewResponseToResponse}" />
						<h:outputText value=" #{msgs.cdfm_button_bar_reply_to_msg}" />
					</h:commandLink>
					
					<h:commandLink styleClass="button"  title="#{msgs.cdfm_button_bar_reply_to_thread}" action="#{ForumTool.processDfMsgReplyThread}" 
							rendered="#{ForumTool.selectedTopic.isNewResponseToResponse && ForumTool.selectedThreadHead.msgApproved && !ForumTool.selectedTopic.locked && !ForumTool.selectedForum.locked == 'true'}">
						<h:graphicImage value="/../../library/image/silk/folder_go.png" alt="#{msgs.cdfm_button_bar_reply_to_thread}" />
						<h:outputText value=" #{msgs.cdfm_button_bar_reply_to_thread}" />
					</h:commandLink>
					
					<h:commandLink styleClass="button"  title="#{msgs.cdfm_button_bar_delete_msg}" action="#{ForumTool.processDfMsgDeleteConfirm}" rendered="#{ForumTool.selectedMessage.userCanDelete}" >
						<h:graphicImage value="/../../library/image/silk/email_delete.png" alt="#{msgs.cdfm_button_bar_delete_msg}" />
						<h:outputText value=" #{msgs.cdfm_button_bar_delete_msg}" />
					</h:commandLink>
					
					<h:commandLink styleClass="button"  title="#{msgs.cdfm_button_bar_revise}" action="#{ForumTool.processDfMsgRvs}" 
							rendered="#{ForumTool.selectedMessage.revise}">
						<h:graphicImage value="/../../library/image/silk/email_edit.png" alt="#{msgs.cdfm_button_bar_revise}" />
						<h:outputText value=" #{msgs.cdfm_button_bar_revise}" />
					</h:commandLink>
					
					<h:commandLink styleClass="button"  title="#{msgs.cdfm_button_bar_grade}" action="#{ForumTool.processDfMsgGrd}" 
							rendered="#{ForumTool.selectedTopic.isPostToGradebook && ForumTool.gradebookExist}">
						<h:graphicImage value="/../../library/image/silk/award_star_gold_1.png" alt="#{msgs.cdfm_button_bar_grade}" />
						<h:outputText value=" #{msgs.cdfm_button_bar_grade}" />
					</h:commandLink>
					<%-- Email --%>
					<h:outputLink styleClass="button"  id="createEmail1" value="mailto:#{ForumTool.selectedMessage.authorEmail}" rendered="#{ForumTool.selectedMessage.userCanEmail && ForumTool.selectedMessage.authorEmail != '' && ForumTool.selectedMessage.authorEmail != null}"> 
						<f:param value="Feedback on #{ForumTool.selectedMessage.message.title}" name="subject" />
						<h:graphicImage value="/../../library/image/silk/email_edit.png" alt="#{msgs.cdfm_button_bar_email}" />
  						<h:outputText value=" #{msgs.cdfm_button_bar_email}"/>
					</h:outputLink>
					<%-- premalink --%>
					<h:outputLink id="permalink1" value="#{ForumTool.messageURL}" styleClass="button permaLink" title="#{msgs.cdfm_button_bar_permalink_message}"> 
						<h:graphicImage value="/../../library/image/silk/folder_go.png" alt="#{msgs.cdfm_button_bar_permalink}" />
  						<h:outputText value=" #{msgs.cdfm_button_bar_permalink}"/>
					</h:outputLink>
				</h:panelGroup>
				<h:panelGroup style="display:block;white-space:nowrap;">
					<h:commandLink styleClass="button" title="#{msgs.cdfm_button_bar_deny}" action="#{ForumTool.processDfMsgDeny}" 
							rendered="#{ForumTool.allowedToDenyMsg}">
						<h:graphicImage value="/../../library/image/silk/cross.png" alt="#{msgs.cdfm_button_bar_deny}" />
						<h:outputText value=" #{msgs.cdfm_button_bar_deny}" />
					</h:commandLink>
					<h:commandLink styleClass="button" title="#{msgs.cdfm_button_bar_add_comment}" action="#{ForumTool.processDfMsgAddComment}" 
							rendered="#{ForumTool.allowedToApproveMsg && ForumTool.selectedMessage.msgDenied}">
						<h:graphicImage value="/../../library/image/silk/comment.png" alt="#{msgs.cdfm_button_bar_add_comment}" />
						<h:outputText value=" #{msgs.cdfm_button_bar_add_comment}" />
					</h:commandLink>
					<h:commandLink styleClass="button" title="#{msgs.cdfm_button_bar_approve}" action="#{ForumTool.processDfMsgApprove}" 
							rendered="#{ForumTool.allowedToApproveMsg}">
						<h:graphicImage value="/../../library/image/silk/tick.png" alt="#{msgs.cdfm_button_bar_approve}" />
						<h:outputText value=" #{msgs.cdfm_button_bar_approve}" />
					</h:commandLink>
				</h:panelGroup>
			</h:panelGrid>
			

			<h:panelGroup layout="block" id="permalinkHolder">
				<h:outputLink styleClass="closeMe" value="#"><h:panelGroup styleClass="icon-sakai--delete"></h:panelGroup></h:outputLink>
				<h:outputText value="#{msgs.cdfm_button_bar_permalink_message}" style="display:block" styleClass="textPanelFooter"/>
				<h:inputTextarea value="" />
			</h:panelGroup>

			<%--navigation cell --%>
			<%@ include file="/jsp/discussionForum/includes/dfViewMessage/msgPrevNext.jspf"%>

			<h:outputText value="#{msgs.cdfm_postFirst_warning}" rendered="#{ForumTool.needToPostFirst}" styleClass="messageAlert"/>
			<t:div rendered="#{!ForumTool.needToPostFirst}"><%@ include file="/jsp/discussionForum/includes/singletonMessageList.jspf"%></t:div>
		
			<h:panelGroup rendered="#{ForumTool.deleteMsg && ForumTool.errorSynch}">
				<h:outputText styleClass="alertMessage" 
				value="#{msgs.cdfm_msg_del_has_reply}" />
			</h:panelGroup>
		
			<%-- If deleting, tells where to go back to --%>	
			<h:inputHidden value="#{ForumTool.fromPage}" />

			<p style="padding:0" class="act">
				<h:commandButton id="post" action="#{ForumTool.processDfMsgDeleteConfirmYes}" value="#{msgs.cdfm_button_bar_delete}" accesskey="x" styleClass="active blockMeOnClick" rendered="#{ForumTool.selectedMessage.userCanDelete}" />
                <h:outputText styleClass="sak-banner-info" style="display:none" value="#{msgs.cdfm_processing_submit_message}" />
			</p>

			<%@ include file="/jsp/discussionForum/includes/dfViewMessage/msgPrevNext.jspf"%>
			<h:panelGroup><br /></h:panelGroup>
			<%@ include file="/jsp/discussionForum/includes/threadPrevNext.jspf"%>
		</h:form>
	</sakai:view>
</f:view>
