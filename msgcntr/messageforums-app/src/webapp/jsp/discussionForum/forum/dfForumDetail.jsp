<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://sakaiproject.org/jsf2/sakai" prefix="sakai" %>
<%@ taglib uri="http://sakaiproject.org/jsf/messageforums" prefix="mf" %>
<jsp:useBean id="msgs" class="org.sakaiproject.util.ResourceLoader" scope="session">
   <jsp:setProperty name="msgs" property="baseName" value="org.sakaiproject.api.app.messagecenter.bundle.Messages"/>
</jsp:useBean>
<f:view>
<sakai:view toolCssHref="/messageforums-tool/css/msgcntr.css">
    <h:form id="msgForum" rendered="#{!ForumTool.selectedForum.forum.draft || ForumTool.selectedForum.forum.createdBy == ForumTool.userId}">
		<script>includeLatestJQuery("msgcntr");</script>
		<script src="/messageforums-tool/js/sak-10625.js"></script>
		<script src="/messageforums-tool/js/forum.js"></script>
		<script src="/webcomponents/rubrics/sakai-rubrics-utils.js<h:outputText value="#{ForumTool.CDNQuery}" />"></script>
		<script type="module" src="/webcomponents/rubrics/rubric-association-requirements.js<h:outputText value="#{ForumTool.CDNQuery}" />"></script>
		<script>
			$(document).ready(function() {
				var menuLink = $('#forumsMainMenuLink');
				var menuLinkSpan = menuLink.closest('span');
				menuLinkSpan.addClass('current');
				menuLinkSpan.html(menuLink.text());

				setupLongDesc();
				setupdfAIncMenus();
			});
		</script>
<!--jsp/discussionForum/forum/dfForumDetail.jsp-->

		<%@ include file="/jsp/discussionForum/menu/forumsMenu.jsp" %>
			<h:outputText styleClass="showMoreText"  style="display:none" value="#{msgs.cdfm_show_more_full_description}"  />


		<h3 class="specialLink" style="margin-bottom:1em">
          		<%-- Display the proper home page link: either Messages & Forums OR Forums --%>
			      <h:commandLink action="#{ForumTool.processActionHome}" value="#{msgs.cdfm_message_forums}" title=" #{msgs.cdfm_message_forums}"
			      		rendered="#{ForumTool.messagesandForums}" />
			      <h:commandLink action="#{ForumTool.processActionHome}" value="#{msgs.cdfm_discussion_forums}" title=" #{msgs.cdfm_discussion_forums}"
			      		rendered="#{ForumTool.forumsTool}" />
			      <h:outputText value=" " /><h:outputText value=" / " /><h:outputText value=" " />
			      <h:outputText value="#{ForumTool.selectedForum.forum.title}" />
		</h3>

		<h:dataTable id="forums" value="#{ForumTool.selectedForumAsList}" rendered="#{!empty ForumTool.selectedForumAsList}" role="presentation" width="100%" var="forum" cellpadding="0" cellspacing="0" styleClass="specialLink" border="0">
			<%@ include file="/jsp/discussionForum/includes/singleForum.jspf"%>
		</h:dataTable>

		<h:inputHidden id="mainOrForumOrTopic" value="dfForumDetail" />
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
	 </h:form>
	 <h:outputText value="#{msgs.cdfm_insufficient_privileges_view_forum}" rendered="#{ForumTool.selectedForum.forum.draft && ForumTool.selectedForum.forum.createdBy != ForumTool.userId}" />
    </sakai:view>
</f:view>
