<!--jsp/discussionForum/area/dfAreaInclude.jsp-->
<script src="/webcomponents/rubrics/sakai-rubrics-utils.js<h:outputText value="#{ForumTool.CDNQuery}" />"></script>
<script type="module" src="/webcomponents/rubrics/rubric-association-requirements.js<h:outputText value="#{ForumTool.CDNQuery}" />"></script>
<h:panelGroup layout="block" styleClass="noForums" rendered="#{empty ForumTool.forums}">
	<p class="noForumsMessage"><h:outputText value="#{msgs.cdfm_forum_noforums} " /></p>
	<h:commandLink  id="create_forum" styleClass="button" title="#{msgs.cdfm_new_forum}" value="#{msgs.cdfm_forum_inf_no_forum_create}" action="#{ForumTool.processActionNewForum}" rendered="#{ForumTool.newForum}" />
</h:panelGroup>
<h:outputText styleClass="accessUserCheck" style="display:none" rendered="#{ForumTool.newForum}" value="x"/>
<script>
$(document).ready(function() {
	var topicLen = $('.topicTitle').length;
	var forumLen = $('.forumHeader').length;
	var draftForumLen = $('.draftForum').length
	var draftTopicLen = $('.draftTopic').length
	var accessCheck = $('.accessUserCheck').length
	var noForums = $('.noForumsMessage').length

	if (forumLen===1 && draftForumLen ===0 && topicLen===1 && draftTopicLen ===0){
		//probably the default forum adn topic, show an orienting message
		$('.defForums').show();
	}

	// either no topics or all topics are draft - show message in either case
	if((topicLen===0 || draftTopicLen===topicLen) && forumLen !==0){
		if ((topicLen===draftTopicLen) && topicLen!==0){
			$('.noTopicsDraft').show();
		}
		$('.noTopics').show();
		if(topicLen===0){
		$('.noTopicsatAll').show();
		}
	}
	//all forums are draft - show message
	if ((forumLen=== draftForumLen) && forumLen !==0){
		$('.noForumsDraft').show();
		$('.noTopics').hide();
	}
	//no forums because they are all draft or childless- show message to access users
	if (forumLen ===0 && accessCheck === 0 && noForums ===0){
		$('.noForumsAccess').show();
	}
	setupdfAIncMenus();
});
</script>
<h:outputText escape="false" value="<script>$(document).ready(function() {setupLongDesc()});</script>"  rendered="#{!ForumTool.showShortDescription}"/>

<%-- OWL TODO: What does this More line do? --%>
<h:outputText styleClass="showMoreText" style="display:none" value="#{msgs.cdfm_show_more_full_description}" />

<p class="instruction noForumsAccess"  style="display:none;">
	<h:outputText styleClass="instruction"  value="#{msgs.cdfm_forum_inf_no_forum_access}" />
</p>
<f:subview id="maintainMessages" rendered="#{ForumTool.newForum}">
	<p class="instruction defForums highlightPanel"  style="display:none;">
		<h:outputText value="#{msgs.cdfm_forum_inf_init_guide}" escape="false" />
	</p>
	<p class="instruction noTopics  highlightPanel" style="display:none">
		<h:outputText styleClass="highlight" style="font-weight:bold" value="#{msgs.cdfm_forum_inf_note} " />
		<h:outputText escape="false" value="#{msgs.cdfm_forum_inf_no_topics}" styleClass="noTopicsatAll" style="display:none"/>
		<span class="noTopicsDraft" style="display:none"><h:outputText value="#{msgs.cdfm_forum_inf_all_topics_draft}" /></span>
	</p>
	<p class="instruction noForumsDraft  highlightPanel" style="display:none">
		<h:outputText styleClass="highlight" style="font-weight:bold" value="#{msgs.cdfm_forum_inf_note} " />
		<h:outputText escape="false" value="#{msgs.cdfm_forum_inf_no_forums}"/>
		<span class="noForumsDraft" style="display:none"><h:outputText value="#{msgs.cdfm_forum_inf_all_forums_draft}" /></span>
	</p>
</f:subview>

<h:dataTable id="forums" styleClass="forums" value="#{ForumTool.forums}" rendered="#{!empty ForumTool.forums}" role="presentation" var="forum">
<%@ include file="singleForum.jspf"%>
</h:dataTable>
