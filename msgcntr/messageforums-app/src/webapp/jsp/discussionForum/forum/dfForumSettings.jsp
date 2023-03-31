<%@ page import="java.util.*, javax.faces.context.*, javax.faces.application.*,
                 javax.faces.el.*, org.sakaiproject.tool.messageforums.*"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>
<%@ taglib uri="http://sakaiproject.org/jsf2/sakai" prefix="sakai" %>
<%@ taglib uri="http://sakaiproject.org/jsf/messageforums" prefix="mf" %>
<jsp:useBean id="msgs" class="org.sakaiproject.util.ResourceLoader" scope="session">
   <jsp:setProperty name="msgs" property="baseName" value="org.sakaiproject.api.app.messagecenter.bundle.Messages"/>
</jsp:useBean>
<%
String thisId = request.getParameter("panel");
if (thisId == null) 
{
	thisId = "Main" + org.sakaiproject.tool.cover.ToolManager.getCurrentPlacement().getId();
}
%>
<f:view>
	<sakai:view>
      <h:form id="revise">
		<script>includeLatestJQuery("msgcntr");</script>
		<link rel="stylesheet" href="/messageforums-tool/css/msgcntr.css<h:outputText value="#{ForumTool.CDNQuery}" />" type="text/css" />
		<script src="/messageforums-tool/js/forum.js<h:outputText value="#{ForumTool.CDNQuery}" />"></script>
		<script src="/messageforums-tool/js/sak-10625.js<h:outputText value="#{ForumTool.CDNQuery}" />"></script>
		<script src="/messageforums-tool/js/messages.js<h:outputText value="#{ForumTool.CDNQuery}" />"></script>
		<%@ include file="/jsp/discussionForum/includes/rubrics/rubricsJs.jspf" %>

		<script>
			$(document).ready(function(){
				//fade permission block 
				// $('#permissionReadOnly').fadeTo("fast", 0.50);
				// and then disable all the inputs/selects in the permission include so as not to confuse people
				// cannot seem to be able to submit this if these inputs are disabled :-(
				// $('#permissionReadOnly input, #permissionReadOnly select').attr('disabled', 'disabled');
				//toggle the long description, hiding the hide link, then toggling the hide, show links and description
				$('a#hide').hide();
				$('#toggle').hide();
				$('a#show,a#hide').click(function(){
					$('#toggle,a#hide,a#show').toggle();
					resizeFrame('grow');
					return false;
				});
				var menuLink = $('#forumsMainMenuLink');
				var menuLinkSpan = menuLink.closest('span');
				menuLinkSpan.addClass('current');
				menuLinkSpan.html(menuLink.text());
			});
		</script>
		<%@ include file="/jsp/discussionForum/menu/forumsMenu.jsp" %>
<!--jsp/discussionForum/forum/dfForumSettings.jsp-->
		<%--<sakai:tool_bar_message value="#{msgs.cdfm_delete_forum_title}" />--%>
		<%--//designNote: this just feels weird - presenting somehting that sort of looks like the form used to create the forum (with an editable permissions block!) to comfirm deletion --%>
		<h:messages styleClass="sak-banner-error" id="errorMessages" rendered="#{! empty facesContext.maximumSeverity}" />
		<t:dataList id="forumHeader" value="#{ForumTool.selectedForumAsList}" rendered="#{!empty ForumTool.selectedForumAsList}" var="forum">
			<t:div styleClass="singleForumHeader suppressLinkTitleDisplay page-header">
				<%@ include file="/jsp/discussionForum/includes/singleForum/forumHeader.jspf"%>
			</t:div>
		</t:dataList>
		<t:div id="alert-delete" styleClass="sak-banner-warn" rendered="#{ForumTool.selectedForum.markForDeletion}">
			<h:outputText value="#{ForumTool.confirmDeleteSelectedForumWarning}"/>
		</t:div>
		<t:div styleClass="sak-banner-warn" rendered="#{ForumTool.selectedForum.markForDuplication}">
			<h:outputText value="#{msgs.cdfm_duplicate_forum_confirm}" />
		</t:div>

       <div class="act">
          <h:commandButton id ="revise" rendered="#{!ForumTool.selectedForum.markForDeletion && !ForumTool.selectedForum.markForDuplication}" 
                           immediate="true"  action="#{ForumTool.processActionReviseForumSettings}" 
                           value="#{msgs.cdfm_button_bar_revise}" accesskey="r"> 
    	 	  	<f:param value="#{ForumTool.selectedForum.forum.id}" name="forumId"/>    	 	  	
          </h:commandButton>
          
          <h:commandButton id="delete_confirm" action="#{ForumTool.processActionDeleteForumConfirm}" 
                           value="#{msgs.cdfm_button_bar_delete_forum}" rendered="#{!ForumTool.selectedForum.markForDeletion && !ForumTool.selectedForum.markForDuplication}"
                           accesskey="" styleClass="blockMeOnClick">
	        	<f:param value="#{ForumTool.selectedForum.forum.id}" name="forumId"/>
          </h:commandButton>
          
          <h:commandButton id="delete" action="#{ForumTool.processActionDeleteForum}" 
                           value="#{msgs.cdfm_button_bar_delete_forum}" rendered="#{ForumTool.selectedForum.markForDeletion}"
                           accesskey="" styleClass="active blockMeOnClick">
	        	<f:param value="#{ForumTool.selectedForum.forum.id}" name="forumId"/>
          </h:commandButton>
          
          <h:commandButton id="duplicate" action="#{ForumTool.processActionDuplicateForum}" 
                           value="#{msgs.cdfm_duplicate_forum}" rendered="#{ForumTool.selectedForum.markForDuplication}"
                           accesskey="s" styleClass="active blockMeOnClick">
	        	<f:param value="#{ForumTool.selectedForum.forum.id}" name="forumId"/>
          </h:commandButton>
          
          <h:commandButton id="cancel" immediate="true" action="#{ForumTool.processReturnToOriginatingPage}" 
                           value="#{msgs.cdfm_button_bar_cancel}" accesskey="x" />
         
         <h:outputText styleClass="sak-banner-info" style="display:none" value="#{msgs.cdfm_processing_submit_message}" />
       </div>
	 </h:form>
    </sakai:view>
</f:view>
