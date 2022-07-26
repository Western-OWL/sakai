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
	<span class="skip" id="firstPendingItemTitleHolder"><h:outputText value="#{msgs.cdfm_gotofirstpendingtitle}" /></span>
	<span class="skip" id="nextPendingItemTitleHolder"><h:outputText value="#{msgs.cdfm_gotopendtitle}" /></span>
	<span class="skip" id="lastPendingItemTitleHolder"><h:outputText value="#{msgs.cdfm_lastpendtitle}" /></span>
	<span class="skip" id="firstNewItemTitleHolder"><h:outputText value="#{msgs.cdfm_gotofirstnewtitle}" /></span>
	<span class="skip" id="nextNewItemTitleHolder"><h:outputText value="#{msgs.cdfm_gotonewtitle}" /></span>
	<span class="skip" id="lastNewItemTitleHolder"><h:outputText value="#{msgs.cdfm_lastnewtitle}" /></span>
	<h:form id="msgForum" styleClass="dfViewThreadForm" rendered="#{!ForumTool.selectedTopic.topic.draft || ForumTool.selectedTopic.topic.createdBy == ForumTool.userId}">

		<!--jsp/discussionForum/message/dfViewThread.jsp-->
       		<script>includeLatestJQuery("msgcntr");</script>
  			<script src="/messageforums-tool/js/dialog.js"></script>
			<script>includeWebjarLibrary("qtip2");</script>
			<link rel="stylesheet" href="/messageforums-tool/css/msgcntr.css" type="text/css" />
  			<link rel="stylesheet" type="text/css" href="/messageforums-tool/css/dialog.css" />	
       		<script src="/messageforums-tool/js/sak-10625.js"></script>
		<script src="/messageforums-tool/js/forum.js"></script>
		<script>
			$(document).ready(function () {
				var menuLink = $('#forumsMainMenuLink');
				var menuLinkSpan = menuLink.closest('span');
				menuLinkSpan.addClass('current');
				menuLinkSpan.html(menuLink.text());

				setupMessageNav('messagePending');
				setupMessageNav('messageNew');
			});
		</script>
		<%@ include file="/jsp/discussionForum/menu/forumsMenu.jsp" %>
	<%--//
		//plugin required below
		<script src="/messageforums-tool/js/pxToEm.js"></script>

		/*
		gsilver: get a value representing max indents
	 	from the server configuraiton service or the language bundle, parse 
		all the indented items, and if the item indent goes over the value, flatten to the value 
		*/
		<script>
		$(document).ready(function() {
			// pick value from element (that gets it from language bundle)
			maxThreadDepth =$('#maxthreaddepth').text()
			// double check that this is a number
			if (isNaN(maxThreadDepth)){
				maxThreadDepth=10
			}
			// for each message, if the message is indented more than the value above
			// void that and set the new indent to the value
			$(".messagesThreaded td").each(function (i) {
				paddingDepth= $(this).css('padding-left').split('px');
				if ( paddingDepth[0] > parseInt(maxThreadDepth.pxToEm ({scope:'body', reverse:true}))){
					$(this).css ('padding-left', maxThreadDepth + 'em');
				}
			});
		});
		</script>	
		
		// element into which the value gets insert and retrieved from
		<span class="highlight"  id="maxthreaddepth" class="skip"><h:outputText value="#{msgs.cdfm_maxthreaddepth}" /></span>
//--%>
	<div class="forumsNavBar">
		<h:panelGroup layout="block" styleClass="suppressThreadCrumbLink">
			<%@ include file="/jsp/discussionForum/includes/crumbs/standard.jspf" %>
		</h:panelGroup>
		<%@ include file="/jsp/discussionForum/includes/threadPrevNext.jspf"%>
	</div>

	<div class="page-header">
		<h1><h:outputText value="#{ForumTool.selectedThreadHead.message.title}"/></h1>
	</div>

	<div id="dialogDiv" title="Grade Messages" style="display:none">
		<iframe id="dialogFrame" name="dialogFrame" width="100%" height="100%" frameborder="0"></iframe>
	</div>
	<h:messages styleClass="sak-banner-error" id="errorMessages" rendered="#{! empty facesContext.maximumSeverity}" />

	<div id="gradesSavedDiv" class="sak-banner-success" style="display:none">
		<h:outputText value="#{msgs.cdfm_grade_successful}"/>
	</div>

	<div class="sakai-table-toolBar">
		<div class="sakai-table-filterContainer">
			<%@ include file="dfViewSearchBarThread.jsp"%>
			<div class="sakai-table-searchFilter">
				<h:commandLink styleClass="button" value="#{msgs.cdfm_reply_thread}" id="replyThread" 
					rendered="#{ForumTool.selectedTopic.isNewResponseToResponse && ForumTool.selectedThreadHead.msgApproved && !ForumTool.selectedTopic.locked && !ForumTool.selectedForum.locked == 'true'}"
					action="#{ForumTool.processDfMsgReplyThread}" immediate="true"/>
				<h:commandLink styleClass="button markAllAsRead" value=" #{msgs.cdfm_mark_all_as_read}" id="markAllRead" action="#{ForumTool.processActionMarkAllThreadAsRead}"
					rendered="#{ForumTool.selectedTopic.isMarkAsRead && !ForumTool.selectedTopic.topic.autoMarkThreadsRead
								&& (!ForumTool.selectedThreadHead.read || ForumTool.selectedThreadHead.childUnread > 0)}"/>
			</div>
		</div>
		<h:panelGroup layout="block" styleClass="sakai-table-buttonContainer threadOptions" rendered="#{!ForumTool.threadMoved}">
			<div id="messNavHolder"><h:outputLink styleClass="button jumpToNew" value="#messageNewnewMess0"
							  rendered="#{!ForumTool.selectedTopic.topic.autoMarkThreadsRead
								&& (!ForumTool.selectedThreadHead.read || ForumTool.selectedThreadHead.childUnread > 0)}">
					<h:outputText value="#{msgs.cdfm_gotofirstnewtitle}" /></h:outputLink>
			</div>
			<h:outputLink styleClass="button" id="print" value="javascript:printFriendly('#{ForumTool.printFriendlyUrlThread}');">
				<h:outputText value="#{msgs.cdfm_print}" />
			</h:outputLink>
		</h:panelGroup>
	</div>

		<h:outputText value="#{msgs.cdfm_postFirst_warning}" rendered="#{ForumTool.needToPostFirst}" styleClass="sak-banner-info"/>
        <%-- a moved message --%>
        <h:panelGroup rendered="#{ForumTool.threadMoved}" >
          <f:verbatim><span></f:verbatim>
            <h:outputText styleClass="threadMovedMsg" value="<b>#{ForumTool.selectedThreadHead.message.title}</b> " escape="false"/>
            <h:outputText styleClass="threadMovedMsg" value="#{msgs.hasBeen} " />
            <h:commandLink action="#{ForumTool.processActionDisplayTopic}" id="topic_title" styleClass="threadMovedMsg">
              <h:outputText value="#{msgs.moved}" />
                                                    <f:param value="#{ForumTool.selectedThreadHead.message.topic.id}" name="topicId"/>
                                                    <f:param value="#{ForumTool.selectedForum.forum.id}" name="forumId"/>
                                            </h:commandLink>
            <h:outputText styleClass="threadMovedMsg"  value=" #{msgs.anotherTopic}" />
          <f:verbatim></span></f:verbatim>
        </h:panelGroup>

		<%--rjlowe: Expanded View to show the message bodies, but not threaded --%>

		<h:dataTable id="expandedMessages" value="#{ForumTool.selectedThread}" var="message" rendered="#{!ForumTool.threaded}"
			styleClass="table table-hover table-striped table-bordered messagesFlat" columnClasses="bogus">
			<h:column>
				<%@ include file="dfViewThreadBodyInclude.jsp" %>
			</h:column>
		</h:dataTable>

		<%--rjlowe: Expanded View to show the message bodies, threaded --%>
		<mf:hierDataTable id="expandedThreadedMessages" value="#{ForumTool.selectedThread}" var="message" rendered="#{ForumTool.threaded}"
				noarrows="true" styleClass="table table-hover table-striped table-bordered messagesThreaded" border="0" cellpadding="0" cellspacing="0" width="100%" columnClasses="bogus">
			<h:column id="_msg_subject">
				<%@ include file="dfViewThreadBodyInclude.jsp" %>
			</h:column>
		</mf:hierDataTable>

		<%@ include file="/jsp/discussionForum/includes/threadPrevNext.jspf"%>

		<h:inputHidden id="mainOrForumOrTopic" value="dfViewThread" />
		<%--//designNote:  need a message if no messages (as in when there are no unread ones)  --%>

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
	<h:outputText value="#{msgs.cdfm_insufficient_privileges_view_topic}" rendered="#{ForumTool.selectedTopic.topic.draft && ForumTool.selectedTopic.topic.createdBy != ForumTool.userId}" />
</sakai:view>
</f:view>
