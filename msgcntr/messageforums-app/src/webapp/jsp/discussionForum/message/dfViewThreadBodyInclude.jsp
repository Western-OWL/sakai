<%-- Display single message in threaded view. (included for each message). --%>
<%-- designNote: what does read/unread mean in this context since I am seeing the whole message?--%>

<%
  	String thisId = request.getParameter("panel");
  	if (thisId == null) 
  	{
    	thisId = "Main" + org.sakaiproject.tool.cover.ToolManager.getCurrentPlacement().getId();
	}
%>
<script>
	var iframeId = '<%= org.sakaiproject.util.Web.escapeJavascript(thisId)%>';
	
	function resize(){
		mySetMainFrameHeight('<%= org.sakaiproject.util.Web.escapeJavascript(thisId)%>');
	}
	
	function dialogLinkClick(link){
		var position =  $(link).position();
		dialogutil.openDialog('dialogDiv', 'dialogFrame', position.top);
	}
</script>

<h:outputText escape="false" value="<a id=\"#{message.message.id}\" name=\"#{message.message.id}\"></a>" />
<div class="hierItemBlock" >
	<%@ include file="/jsp/discussionForum/includes/singleMessage.jspf"%>
	<%-- close the div with class of hierItemBlock --%>
<h:outputText escape="false" value="</div>"  rendered="#{!message.deleted}"/>
