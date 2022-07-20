<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>
<%@ taglib uri="http://sakaiproject.org/jsf2/sakai" prefix="sakai" %>
<%@ taglib uri="http://sakaiproject.org/jsf/messageforums" prefix="mf" %>
<jsp:useBean id="msgs" class="org.sakaiproject.util.ResourceLoader" scope="session">
   <jsp:setProperty name="msgs" property="baseName" value="org.sakaiproject.api.app.messagecenter.bundle.Messages"/>
</jsp:useBean>

<f:view>
<sakai:view>

	<h:form id="msgForum" styleClass="dfFlatViewForm specialLink">

	<!--jsp/discussionForum/message/dfFlatView.jsp-->
		<script>includeLatestJQuery("msgcntr");</script>
		<link rel="stylesheet" href="/messageforums-tool/css/msgcntr.css" type="text/css" />
		<link rel="stylesheet" type="text/css" href="/messageforums-tool/css/dialog.css" />
		<script>includeWebjarLibrary("qtip2");</script>
		<script src="/messageforums-tool/js/sak-10625.js"></script>
		<script src="/messageforums-tool/js/forum.js"></script>
		<script src="/messageforums-tool/js/dialog.js"></script>
        <script>
            $(document).ready(function(){
                var menuLink = $('#forumsMainMenuLink');
                var menuLinkSpan = menuLink.closest('span');
                menuLinkSpan.addClass('current');
                menuLinkSpan.html(menuLink.text());

                setupMessageNav('messagePending');
                setupMessageNav('messageNew');

            });
        </script>
		<%@ include file="/jsp/discussionForum/menu/forumsMenu.jsp" %>

		<div id="dialogDiv" title="Grade Messages" style="display:none">
			<iframe id="dialogFrame" name="dialogFrame" width="100%" height="100%" frameborder="0"></iframe>
		</div>

		<div class="forumsNavBar">
			<div class="suppressTopicCrumbLink hideThreadCrumb">
				<%@ include file="/jsp/discussionForum/includes/crumbs/standard.jspf" %>
			</div>
			<%@ include file="/jsp/discussionForum/includes/topicPrevNext.jspf" %>
 		</div>
	
		<%--rjlowe: Expanded View to show the message bodies, threaded --%>
	<span class="skip" id="firstNewItemTitleHolder"><h:outputText value="#{msgs.cdfm_gotofirstnewtitle}" /></span>
	<span class="skip" id="firstPendingItemTitleHolder"><h:outputText value="#{msgs.cdfm_gotofirstpendingtitle}" /></span>
	<span class="skip" id="nextPendItemTitleHolder"><h:outputText value="#{msgs.cdfm_gotopendtitle}" /></span>
	<span class="skip" id="lastPendItemTitleHolder"><h:outputText value="#{msgs.cdfm_lastpendtitle}" /></span>
	<span class="skip" id="nextNewItemTitleHolder"><h:outputText value="#{msgs.cdfm_gotonewtitle}" /></span>
	<span class="skip" id="lastNewItemTitleHolder"><h:outputText value="#{msgs.cdfm_lastnewtitle}" /></span>


		<%@ include file="/jsp/discussionForum/includes/topicHeader/singletonTopicHeaderList.jspf"%>
		<%@ include file="/jsp/discussionForum/includes/topicViewActions.jspf" %>

		<h:outputText  value="#{msgs.cdfm_no_messages}" rendered="#{empty ForumTool.messages}"   styleClass="sak-banner-info" style="display:block" />
		<mf:hierDataTable id="expandedThreadedMessages" value="#{ForumTool.messages}" var="message" noarrows="true" styleClass="table table-hover table-striped table-bordered messagesThreaded" columnClasses="bogus">
			<h:column id="_msg_subject">
				<%@ include file="dfViewThreadBodyInclude.jsp" %>
			</h:column>
		</mf:hierDataTable>
		<%@ include file="/jsp/discussionForum/includes/topicPrevNext.jspf" %>
				
		<h:inputHidden id="mainOrForumOrTopic" value="dfFlatView" />
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

</sakai:view>
</f:view>
