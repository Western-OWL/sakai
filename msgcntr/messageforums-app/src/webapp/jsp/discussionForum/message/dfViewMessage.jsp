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
					var msgBody = document.querySelector(".messageBody .textPanel").innerHTML;
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
			<div class="forumsNavBar">
				<h:panelGroup layout="block" styleClass="">
					<%@ include file="/jsp/discussionForum/includes/crumbs/standard.jspf" %>
				</h:panelGroup>
				<%@ include file="/jsp/discussionForum/includes/threadPrevNext.jspf"%>
			</div>
			<h:messages globalOnly="true" infoClass="success" errorClass="alertMessage" rendered="#{! empty facesContext.maximumSeverity}"/>

			<h:panelGroup layout="block" id="permalinkHolder">
				<h:outputLink styleClass="closeMe" value="#"><h:panelGroup styleClass="icon-sakai--delete"></h:panelGroup></h:outputLink>
				<h:outputText value="#{msgs.cdfm_button_bar_permalink_message}" style="display:block" styleClass="textPanelFooter"/>
				<h:inputTextarea value="" />
			</h:panelGroup>

			<h:outputText value="#{msgs.cdfm_postFirst_warning}" rendered="#{ForumTool.needToPostFirst}" styleClass="messageAlert"/>
			<t:div rendered="#{!ForumTool.needToPostFirst}"><%@ include file="/jsp/discussionForum/includes/singletonMessageList.jspf"%></t:div>
		
			<h:panelGroup rendered="#{ForumTool.deleteMsg && ForumTool.errorSynch}">
				<h:outputText styleClass="alertMessage" value="#{msgs.cdfm_msg_del_has_reply}" />
			</h:panelGroup>
		
			<%-- If deleting, tells where to go back to --%>	
			<h:inputHidden value="#{ForumTool.fromPage}" />

			<%@ include file="/jsp/discussionForum/includes/dfViewMessage/msgPrevNext.jspf"%>
			<h:panelGroup layout="block" rendered="#{ForumTool.deleteMsg && ForumTool.selectedMessage.userCanDelete}" styleClass="act">
				<h:commandButton id="post" action="#{ForumTool.processDfMsgDeleteConfirmYes}" value="#{msgs.cdfm_button_bar_delete}" accesskey="s" styleClass="active blockMeOnClick" />
				<h:commandButton id="cancelDelete" action="#{ForumTool.processDfMsgDeleteConfirmNo}" value="#{msgs.cdfm_button_bar_cancel}" immediate="true" accesskey="x" />
				<h:outputText styleClass="sak-banner-info" style="display:none" value="#{msgs.cdfm_processing_submit_message}" />
			</h:panelGroup>
		</h:form>
	</sakai:view>
</f:view>
