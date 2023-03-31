    <h:outputText escape="false" value='<div class="topic-picker" id="topic-picker" style="display:none" title="#{msgs.move_thread}">'/>
        <div class="selected-threads-to-move">
            <p>
                <h:outputText value="#{msgs.move_thread_info1} " />
                <span id="sourcetopicid-<h:outputText value="#{ForumTool.selectedTopic.topic.id}" />">
                    <h:outputText styleClass="sourcetitle" escape="true" value=" #{ForumTool.selectedTopic.topic.title} " />
                </span>
                <h:outputText value=" #{msgs.move_thread_info2}" />
            </p>
            <div class="threads-to-move" escape="true"></div>
            <label style="font-weight:normal">
                <input class="checkbox-reminder" id="checkbox-reminder" type="checkbox" name="checkbox-reminder" />
                <h:outputText value="#{msgs.leave_reminder}" />
            </label>
        </div>
        <div class="topic-filter">
            <div class="topic-filter-header">
                <div class="topic-header-h3"><h:outputText value="#{msgs.filter_topics}"  /></div>
            </div>
            <div class="topic-filter-fields">
                <label><h:outputText value="#{msgs.by_name}"  /></label>
                <input class="topic-search-field" type="text" value="" id="searchTopic" /><br />
                <label><h:outputText value="#{msgs.in_forum}"  /></label>
                <select name="forumDropdown" class="forumDropdown">
                    <option value="select-forum" id="select-forum"><h:outputText value="#{msgs.select_forum}"/></option>
                </select>
            </div>
            <div class="sak-banner-warn">
                <h:outputText value="#{msgs.locked_topics_hidden}"  />
            </div>
        </div>
        <div class="topic-source">
            <div class="topic-filter-header">
                <span class="topic-header-h3"><h:outputText value="#{msgs.topics}"  /></span>
                (<h:outputText value="#{msgs.move_showing}"/> 
                <span class="topic-source-counter">0</span> 
                <h:outputText value="#{msgs.move_of}"/>
                <span class="topic-source-total">0</span>
                <h:outputText value="#{msgs.move_topics}"/>)
            </div>
            <div class="topic-source-picker">
                <div id="source-scroller" class="flc-scroller scroller"  style="max-height: 270px;" tabindex="0">
                    <div class="topic-source-scroller-inner">
                        <!-- Individual List -->
                        <div class="topic-source-list"> </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="topic-submit act">
            <h:commandButton type="button" value="#{msgs.move_move}" styleClass="topic-btn-save active"/>
            <h:commandButton type="button" value="#{msgs.move_cancel}" styleClass="topic-btn-cancel" />
        </div>
    </div>
    <div id="data" style="display:none">
        <h:outputText escape="true" value="#{ForumTool.moveThreadJSON}" />
    </div>
