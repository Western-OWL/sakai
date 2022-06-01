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

	<h:form id="msgForum" styleClass="specialLink">

	<!--jsp/discussionForum/message/dfFlatView.jsp-->
		<script>includeLatestJQuery("msgcntr");</script>
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

                setupMessageNav('messageNew');
                setupMessageNav('messagePending');

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
	    	<div id="dialogDiv" title="Grade Messages" style="display:none">
	    		<iframe id="dialogFrame" name="dialogFrame" width="100%" height="100%" frameborder="0"></iframe>
	    	</div>

		<div class="row">
			<div class="col-md-9 col-xs-12 suppressTopicCrumbLink hideThreadCrumb">
				<%@ include file="/jsp/discussionForum/includes/crumbs/standard.jspf" %>
			</div>
			<div class="pull-right">
				<%@ include file="/jsp/discussionForum/includes/topicPrevNext.jspf" %>
 			</div>
 		</div>
	
		<%--rjlowe: Expanded View to show the message bodies, threaded --%>
	<span class="skip" id="firstNewItemTitleHolder"><h:outputText value="#{msgs.cdfm_gotofirstnewtitle}" /></span>
	<span class="skip" id="firstPendingItemTitleHolder"><h:outputText value="#{msgs.cdfm_gotofirstpendingtitle}" /></span>
	<span class="skip" id="nextPendItemTitleHolder"><h:outputText value="#{msgs.cdfm_gotopendtitle}" /></span>
	<span class="skip" id="lastPendItemTitleHolder"><h:outputText value="#{msgs.cdfm_lastpendtitle}" /></span>
	<span class="skip" id="nextNewItemTitleHolder"><h:outputText value="#{msgs.cdfm_gotonewtitle}" /></span>
	<span class="skip" id="lastNewItemTitleHolder"><h:outputText value="#{msgs.cdfm_lastnewtitle}" /></span>

		<div id="messNavHolder" style="clear:both;">
				<h:commandLink action="#{ForumTool.processActionMarkAllAsRead}" rendered="#{ForumTool.selectedTopic.isMarkAsRead}" styleClass="button"> 
					<h:outputText value=" #{msgs.cdfm_mark_all_as_read}" />
				</h:commandLink>
		</div>

		<%@ include file="/jsp/discussionForum/includes/topicViewActions.jspf" %>

		<h:outputText  value="#{msgs.cdfm_no_messages}" rendered="#{empty ForumTool.messages}"   styleClass="sak-banner-info" style="display:block" />
		<div class="clear">
			<mf:hierDataTable id="expandedThreadedMessages" value="#{ForumTool.messages}" var="message" 
	   	 		noarrows="true" styleClass="table-hover messagesThreaded" cellpadding="0" cellspacing="0" width="100%" columnClasses="bogus">
				<h:column id="_msg_subject">
					<%@ include file="dfViewThreadBodyInclude.jsp" %>
				</h:column>
			</mf:hierDataTable>
		</div>
		
		<f:verbatim><br/><br/></f:verbatim>
		<h:panelGrid columns="1" width="100%" styleClass="navPanel specialLink">
			<%@ include file="/jsp/discussionForum/includes/topicPrevNext.jspf" %>
		</h:panelGrid>
				
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
