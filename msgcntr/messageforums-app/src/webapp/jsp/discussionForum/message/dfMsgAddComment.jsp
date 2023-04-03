<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>
<%@ taglib uri="http://sakaiproject.org/jsf2/sakai" prefix="sakai"%>
<%@ taglib uri="http://sakaiproject.org/jsf/messageforums" prefix="mf"%>
<jsp:useBean id="msgs" class="org.sakaiproject.util.ResourceLoader" scope="session">
	<jsp:setProperty name="msgs" property="baseName" value="org.sakaiproject.api.app.messagecenter.bundle.Messages" />
</jsp:useBean>
<f:view>

<sakai:view title="#{msgs.cdfm_add_comment}">
	<script>includeLatestJQuery("msgcntr");</script>
	<link rel="stylesheet" href="/messageforums-tool/css/msgcntr.css<h:outputText value="#{ForumTool.CDNQuery}" />" type="text/css" />
	<script src="/messageforums-tool/js/sak-10625.js<h:outputText value="#{ForumTool.CDNQuery}" />"></script>
	<script src="/messageforums-tool/js/messages.js<h:outputText value="#{ForumTool.CDNQuery}" />"></script>
	<script src="/messageforums-tool/js/forum.js<h:outputText value="#{ForumTool.CDNQuery}" />"></script>
	<script>
		$(document).ready(function() {
			initExternalWordCount();
		});
	</script>
	<%@ include file="/jsp/discussionForum/includes/rubrics/rubricsJs.jspf" %>
	<h:form id="dfMsgAddComment">

		<h3><h:outputText value="#{msgs.cdfm_add_comment}" /></h3>
		<%@ include file="/jsp/discussionForum/includes/topicHeader/singletonTopicHeaderList.jspf"%>

		<h:messages globalOnly="true" infoClass="sak-banner-success" errorClass="sak-banner-error" rendered="#{! empty facesContext.maximumSeverity}" />

		<t:div><%@ include file="/jsp/discussionForum/includes/singletonMessageList.jspf"%></t:div>

		<div class="instruction">
			<h:outputText value="#{msgs.cdfm_required}"/> <h:outputText value="#{msgs.pvt_star}" styleClass="reqStarInline" />
		</div>

		<div class="longtext">
			<label for="dfMsgAddComment:commentsBox" class="block"><h:outputText value="#{msgs.cdfm_info_required_sign} " styleClass="reqStarInline"/><h:outputText value="#{msgs.cdfm_add_comment_label} " /></label>	
			<h:inputTextarea value="#{ForumTool.moderatorComments}" rows="5" cols="50"  id="commentsBox"/>
		</div>
		<sakai:button_bar> 
			<h:commandButton action="#{ForumTool.processAddCommentToDeniedMsg}" value="#{msgs.cdfm_button_bar_add_comment}" accesskey="s" styleClass="active"/>
			<h:commandButton action="#{ForumTool.processCancelAddComment}" value="#{msgs.cdfm_button_bar_cancel}" accesskey="x"/>
		</sakai:button_bar>

	</h:form>
</sakai:view>
</f:view>
