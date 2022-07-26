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
<f:view>
	<sakai:view toolCssHref="/messageforums-tool/css/msgcntr.css">
      <h:form id="revise">
        <script>includeLatestJQuery("msgcntr");</script>
        <script src="/messageforums-tool/js/messages.js"></script>
		<script>
			$(document).ready(function(){
				//fade permission block and then disable all the inputs/selects in the permission include so as not to confuse people
				$('#permissionReadOnly').fadeTo("fast", 0.50);
				// cannot seem to disable these controls and still submit
				// $('#permissionReadOnly input, #permissionReadOnly select').attr('disabled', 'disabled');
				var menuLink = $('#forumsMainMenuLink');
				var menuLinkSpan = menuLink.closest('span');
				menuLinkSpan.addClass('current');
				menuLinkSpan.html(menuLink.text());
			});
		</script>
		<%@ include file="/jsp/discussionForum/menu/forumsMenu.jsp" %>
		<%
	  	String thisId = request.getParameter("panel");
  		if (thisId == null) 
  		{
    		thisId = "Main" + org.sakaiproject.tool.cover.ToolManager.getCurrentPlacement().getId();
  		}
		%>
       		<script src="/messageforums-tool/js/sak-10625.js"></script>
       		<script src="/messageforums-tool/js/forum.js"></script>			
		<%--//designNote: this just feels weird - presenting somehting that sort of looks like the form used to create the topic (with an editable permissions block!) to comfirm deletion --%>
<!--jsp/discussionForum/topic/dfTopicSettings.jsp-->
		<%--<sakai:tool_bar_message value="#{msgs.cdfm_delete_topic_title}"/>--%>

		<t:div id="alert-delete" styleClass="sak-banner-warn" rendered="#{ForumTool.selectedTopic.markForDeletion}">
			<h:outputText value="#{ForumTool.confirmDeleteSelectedTopicWarning}"/>
		</t:div>
        <h:outputText styleClass="sak-banner-warn" value="#{msgs.cdfm_duplicate_topic_confirm}" rendered="#{ForumTool.selectedTopic.markForDuplication}" style="display:block" />
		<h:messages styleClass="sak-banner-warn" id="errorMessages" rendered="#{! empty facesContext.maximumSeverity}" />
		<%@ include file="/jsp/discussionForum/includes/topicHeader/singletonTopicHeaderList.jspf"%>
    
       <div class="act">
          <h:commandButton action="#{ForumTool.processActionReviseTopicSettings}" id="revise"  
                           value="#{msgs.cdfm_button_bar_revise}" rendered="#{!ForumTool.selectedTopic.markForDeletion && !ForumTool.selectedTopic.markForDuplication}"
                           accesskey="r" styleClass="active"> 
    	 	  	<f:param value="#{ForumTool.selectedTopic.topic.id}" name="topicId"/> 
    	 	  	<f:param value="#{ForumTool.selectedForum.forum.id}" name="forumId"/>        
          </h:commandButton>
          <h:commandButton action="#{ForumTool.processActionDeleteTopicConfirm}" id="delete_confirm" 
                           value="#{msgs.cdfm_button_bar_delete_topic}" rendered="#{!ForumTool.selectedTopic.markForDeletion && !ForumTool.selectedTopic.markForDuplication}"
                           styleClass="active blockMeOnClick">
	        	<f:param value="#{ForumTool.selectedTopic.topic.id}" name="topicId"/>
          </h:commandButton>
          <h:commandButton action="#{ForumTool.processActionDeleteTopic}" id="delete" 
                           value="#{msgs.cdfm_button_bar_delete_topic}" rendered="#{ForumTool.selectedTopic.markForDeletion}"
                           styleClass="active blockMeOnClick">
	        	<f:param value="#{ForumTool.selectedTopic.topic.id}" name="topicId"/>
          </h:commandButton>
          
          <h:commandButton id="duplicate" action="#{ForumTool.processActionDuplicateTopic}" 
                           value="#{msgs.cdfm_duplicate_topic}" rendered="#{ForumTool.selectedTopic.markForDuplication}"
                           accesskey="s" styleClass="active blockMeOnClick">
	        	<f:param value="#{ForumTool.selectedTopic.topic.id}" name="topicId"/>
          </h:commandButton>
          
          <h:commandButton immediate="true" action="#{ForumTool.processReturnToOriginatingPage}" id="cancel" 
                           value="#{msgs.cdfm_button_bar_cancel}" accesskey="x" />
         <h:outputText styleClass="sak-banner-info" style="display:none" value="#{msgs.cdfm_processing_submit_message}" />
       </div>
	 </h:form>
    </sakai:view>
</f:view>
