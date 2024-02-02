# If any of these values are updated later, we may need to run this script again. Due to referential integrity, all the records must be deleted before they can be re-inserted.
# Uncomment and run the following statement to delete all site properties belonging to the !admin worksite
# DELETE FROM sakai_site_property WHERE site_id = '!admin';

# Global on/off switch (boolean); determines if the tab in Membership is visible for appropriate users
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_ENABLED', 'true');

# Migration selection options available in the UI; key:displayValue (pipe delimited)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_SELECTION_OPTIONS_MAP', 'undecided:Undecided|doNotMig:Do Not Migrate|selfMig:Self-Migration|assistedMig:Assisted Migration');

# Migration statuses; key:displayValue (pipe delimited)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_STATUS_DISPLAY_MAP', 'migDone:Migrated|doNotMig:Do Not Migrate|manualMig:Manual Migration|pendingMig:Migration Pending|toBeDeleted:To Be Deleted|projPendingMig:Move Pending');

# Migration selection->status map, so an initial selection always has an initial status; selectionKey:statusKey (pipe delimited)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_SELECTION_INITIAL_STATUS_MAP', 'doNotMig:doNotMig|selfMig:manualMig|assistedMig:pendingMig');

# Selections on which statuses can be displayed (selection keys, pipe delimited)
# NOTE: if the selection key is not in this list, the status will not be displayed even if it is contained in OWL_MIG_VISIBLE_STATUSES (below)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_SELECTIONS_WITH_VISIBLE_STATUSES', 'assistedMig');

# Statuses that are allowed to be displayed in the UI (status keys, pipe delimited)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_VISIBLE_STATUSES', 'migDone|pendingMig');

# Selections that can be changed in the UI by users after saving the selection (selection keys, pipe delimited)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_CHANGEABLE_SELECTIONS', 'undecided');

# UI messages/banners that are too difficult to maintain and update through MBM
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_MSG_BANNER_TOP_1', 'Your OWL courses eligible for migration are listed below. Please make a choice for each. Once a choice is made, it cannot be changed. You do not need to make a decision for all listed courses at the same time. Any courses left as just "Undecided" can be changed later.');
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_MSG_BANNER_TOP_2', 'More information about specific migration options or how to request a change if you made a mistake can be found at <a href="https://owlmigration.uwo.ca">OWL Migration Help</a>');
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_MSG_SAVE_CONFIRM_1', 'Please be aware that it is not possible to change a selection once it has been made.');
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_MSG_SAVE_CONFIRM_2', 'You have selected one or more courses for self-migration that are estimated to be close to or above 2 GB in size. Courses this large may be difficult to migrate on your own due to size limitations in Brightspace. Please review your self-migration selections and consider choosing the assisted migraiton option. For more information on the impact of course size, see <a href="https://owlmigration.uwo.ca">OWL Migration Help</a>');

# Extra blank slots for more messages/banners if need be
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_MSG_BANNER_TOP_3', '');
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_MSG_SAVE_CONFIRM_3', '');

# Eligible terms for UI selections; pipe delimited term codes from CM data
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_ELIGIBLE_TERMS', 'UWOCONT1245|UWOGRAD1241|UWOUGRD1239|UWOPREL1239|UWOCONT1239|UWOEDUC1239|UWOPROF1239|UWOGRAD1238|UWOUGRD1235|UWOPROF1235|UWOPREL1235|UWOEDUC1235|UWOGRAD1236|UWOCONT1235|UWOGRAD1231|UWOPREL1229|UWOUGRD1229|UWOCONT1229|UWOPROF1229|UWOEDUC1229|UWOGRAD1228|UWOPREL1225|UWOGRAD1226|UWOEDUC1225|UWOUGRD1225|UWOPROF1225|UWOCONT1225|project');

# Term mappings for the UI, format: <year>:<termCode#>;<termCode#>|<year>:<termCode#>;<termCode#>
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_TERM_GROUPINGS', 'Summer 2022:1225;1226|Fall/Winter 2022:1228;1229;1231|Summer 2023:1235;1236|Fall/Winter 2023:1238;1239;1241|Summer 2024:1245;1246|Project:All');

# Cut off date for project sites
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_PROJECT_SITE_CUTOFF_DATE', '2022-02-28');

# Cut off date for course sites
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_COURSE_SITE_CUTOFF_DATE', '2022-02-28');

# Site size warning threshold; interpretted as gigabytes, max 1 decimal place
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_SITE_SIZE_WARN_THRESHOLD', '1.5');

# Site size error threshold; interpretted as gigabytes, max 1 decimal place
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_SITE_SIZE_ERROR_THRESHOLD', '2.0');

# Admin display name (what will be shown in the UI for admin EIDs, instead of their actual EIDs)
INSERT INTO sakai_site_property
VALUES ('!admin', 'OWL_MIG_ADMIN_DISPLAY_NAME', 'Admin');
