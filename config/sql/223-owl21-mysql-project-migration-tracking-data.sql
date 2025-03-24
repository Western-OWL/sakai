# If any of these values are updated later, we may need to run this script again. Due to referential integrity, all the records must be deleted before they can be re-inserted.
# Uncomment and run the following statement to delete all site properties belonging to the !admin worksite
# DELETE FROM sakai_site_property WHERE site_id = '!admin' AND name LIKE 'OWL_MIG%';

# Global on/off switch (boolean); determines if the tab in Membership is visible for appropriate users
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_ENABLED', 'false');

# Migration type option (keys) that are "active" (available for selection in the UI), (pipe delimited)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_ACTIVE_TYPE_KEYS', 'undecided|research|hrTrain|stuTrain|empTrain|acad|extTrain|extOther|nonInstruct|other');

# Migration type options map; key:displayValue (pipe delimited)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_TYPE_MAP', 'undecided:Undecided|research:Research|hrTrain:Central HR Training|stuTrain:Student Training|empTrain:Departmental Employee Training|acad:Supplementary Academic Materials|extTrain:External Training|extOther:External Other|nonInstruct:Non-Instructional, Other|other:Other');

# Migration admin-only type options map; key:displayValue (pipe delimited)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_ADMIN_TYPE_MAP', 'ofr:OFR Promotion and Tenure|committee:Board/Selection Committees');

# Migration type default selection value to display if the OWL_MIG_ACTIVE_TYPE_KEYS list is empty
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_TYPE_DEFAULT', 'Undecided');

# Migration type->action map, format: typeKey1:actionKey1;actionKey2|typeKey2:actionKey1;actionKey3|typeKey3:actionKey2;actionKey4
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_TYPES_TO_ACTIONS_MAP', 'acad:undecided;mig;alt;ret;selfDel;del|research:undecided;alt;ret;selfDel;del|hrTrain:undecided;mig;alt;ret;selfDel;del|stuTrain:undecided;mig;alt;ret;selfDel;del|empTrain:undecided;mig;alt;ret;selfDel;del|extTrain:undecided;alt;ret;selfDel;del|extOther:undecided;alt;ret;selfDel;del|nonInstruct:undecided;alt;ret;selfDel;del|other:undecided;alt;ret;selfDel;del');

# Migration action option (keys) that are "active" (available for selection in the UI), (pipe delimited)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_ACTIVE_ACTION_KEYS', 'undecided|mig|alt|ret|selfDel|del');

# Migration action options map; key:displayValue (pipe delimited)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_ACTIONS_MAP', 'undecided:Undecided|mig:Request Migration to OWL Brightspace|alt:Transition to Alternate Solution|ret:Retain in Sakai until April 30, 2026|selfDel:I will Delete|del:Delete Anytime');

# Migration statuses; key:displayValue (pipe delimited)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_STATUS_MAP', 'migDone:Migrated|pendingMig:Migration Pending|transDone:Transitioned to Alternate Solution|transPending:Transition to Alternate Solution Pending|ret:Site Retained until April 30, 2026|selfDel:User will Delete|del:Will be Deleted after April 30, 2026');

# Migration action->status map, so an initial action always has an initial status; actionKey:statusKey (pipe delimited)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_ACTION_INITIAL_STATUS_MAP', 'mig:pendingMig|alt:transPending|ret:ret|selfDel:selfDel|del:del');

# Actions on which statuses can be displayed (action keys, pipe delimited)
# NOTE: if the action key is not in this list, the status will not be displayed even if it is contained in OWL_MIG_VISIBLE_STATUSES (below)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_ACTIONS_WITH_VISIBLE_STATUSES', 'mig|alt|ret|del');

# Statuses that are allowed to be displayed in the UI (status keys, pipe delimited)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_VISIBLE_STATUSES', 'migDone|pendingMig|transDone|transPending|ret|del');

# Types that can be changed in the UI by users after saving the type (type keys, pipe delimited)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_CHANGEABLE_TYPES', 'undecided');

# Actions that can be changed in the UI by users after saving the action (action keys, pipe delimited)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_CHANGEABLE_ACTIONS', 'undecided');

# Site size warning threshold; interpretted as gigabytes, max 1 decimal place
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_SITE_SIZE_WARN_THRESHOLD', '1.5');

# Site size error threshold; interpretted as gigabytes, max 1 decimal place
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_SITE_SIZE_ERROR_THRESHOLD', '2.0');

# Actions that will trigger a site resources size check in the UI, format: <actionKey>|<actionKey|<actionKey>
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_ACTIONS_WITH_SIZE_CHECKS', 'mig');

# Admin display name (what will be shown in the UI for admin EIDs, instead of their actual EIDs/display names)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_ADMIN_DISPLAY_NAME', 'Admin');

# Support email address
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_SUPPORT_EMAIL', 'owlmigrationquestions@uwo.ca');

# UI messages/banners that are too difficult to maintain and update through MBM
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_MSG_BANNER_TOP_1', 'Your OWL project sites are listed below. Please make selections for each. Once a choice is made, it cannot be changed. You do not need to make a decision for all listed sites at the same time. Any sites left as just "Undecided", or "Other" can be changed later.');
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_MSG_BANNER_TOP_2', 'More information about specific migration options or how to request a change if you made a mistake can be found at <a target="_blank" href="https://owlmigration.uwo.ca">OWL Migration Help</a>');
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_MSG_SAVE_CONFIRM_1', 'Please be aware that it is not possible to change a selection once it has been made.');
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_MSG_SAVE_CONFIRM_2', 'You have selected one or more sites for migration that are estimated to be close to or above 2 GB in size. Sites this large may be difficult to migrate on your own due to size limitations in Brightspace. Please review your migration selections and consider choosing a different option. For more information on the impact of site size, see <a target="_blank" href="https://owlmigration.uwo.ca">OWL Migration Help</a>');

# Extra blank slots for more messages/banners if need be
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_MSG_BANNER_TOP_3', '');
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_MSG_SAVE_CONFIRM_3', '');
