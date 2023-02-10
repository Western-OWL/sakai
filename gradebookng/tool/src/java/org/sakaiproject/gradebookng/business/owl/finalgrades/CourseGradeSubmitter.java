// 2012.06.13, plukasew, New
// Facilitates the submission of course grades to the Office of the Registrar
// 2012.09.21, plukasew, Modified
// Now gracefully degrades when array-based sakai properties are not found
// Added support for identifying "demo" sections by eid prefix. These sections
// will not have their submissions uploaded to the Registrar but will work like normal
// sections in every other way.
// 2012.09.28, plukasew, Modified
// now checks for grades > 100 and disallows grade submission
// 2014.03.11, bjones86, Modified
// if a cstudies roster AND username starts with any prefix defined in sakai.properites list, send username instead of student number
// 2014.06.13, plukasew, Modified
// OWL-1212, building on OWL-1080 work to support CStudies grade submission

package org.sakaiproject.gradebookng.business.owl.finalgrades;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.coursemanagement.api.Membership;
import org.sakaiproject.coursemanagement.api.Section;
import org.sakaiproject.coursemanagement.api.exception.IdNotFoundException;
import org.sakaiproject.service.gradebook.shared.InvalidGradeException;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.MissingStudentNumberException;
import org.sakaiproject.tool.gradebook.GradeMapping;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeSubmission;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeSubmissionGrades;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeApproval;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.emailtemplateservice.api.RenderedTemplate;
import org.sakaiproject.emailtemplateservice.api.EmailTemplateService;
import org.sakaiproject.entitybroker.DeveloperHelperService;
import org.sakaiproject.entitybroker.util.SakaiToolData;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.model.GbCourseGrade;
import org.sakaiproject.gradebookng.business.model.GbGroup;
import org.sakaiproject.gradebookng.business.owl.OwlBusinessService;
import org.sakaiproject.gradebookng.business.owl.OwlGbStopWatch;
import org.sakaiproject.gradebookng.business.owl.OwlGbUser;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.MissingCourseGradeException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.springframework.core.NestedRuntimeException;

/**
 * Facilitates the submission of course grades to the Office of the Registrar
 * @author plukasew
 */
// OWLTODO: this class is getting too large, break it up
public class CourseGradeSubmitter implements Serializable
{
    private static final Log LOG = LogFactory.getLog(CourseGradeSubmitter.class);
    private static final String LOG_PREFIX = "OWL: Final Grade Submission: ";
    
    private static final String REGISTRAR_GRADE_CODES = "gradebook.courseGradeSubmission.registrarGradeCodes";
    private static final List<String> registrarGradeCodes = readListFromProperty(REGISTRAR_GRADE_CODES);
    
    private static final String SAKORA_ROLES_TO_SUBMIT_SAKAI_PROPERTY = "gradebook.courseGradeSubmission.sakoraRolesToSubmit";
    private static final List<String> rolesToSubmit = readListFromProperty(SAKORA_ROLES_TO_SUBMIT_SAKAI_PROPERTY);
    
    private static final String DEMO_SECTION_PREFIXES = "gradebook.courseGradeSubmission.demoSectionPrefixes";
    private static final List<String> demoSectionPrefixList = readListFromProperty(DEMO_SECTION_PREFIXES);
    
    private static final String LOCAL_DEBUG_MODE_ENABLED_SAKAI_PROPERTY = "gradebook.courseGradeSubmission.localDebugMode.enabled";
    private static final boolean localDebugModeEnabled = ServerConfigurationService.getBoolean(LOCAL_DEBUG_MODE_ENABLED_SAKAI_PROPERTY, false);
    
    private static final String SFTP_ENABLED_SAKAI_PROPERTY = "gradebook.courseGradeSubmission.sftp.enable";
    
    private static final String EMAIL_ENABLED_SAKAI_PROPERTY = "gradebook.courseGradeSubmission.email.enable";
    private static final boolean EMAIL_ENABLED = ServerConfigurationService.getBoolean(EMAIL_ENABLED_SAKAI_PROPERTY, false);
    private static final String EMAIL_ERROR_NOTIFICATION_LIST = "gradebook.courseGradeSubmission.email.errorNotificationList";
    private static final List<String> emailErrorNotificationList = readListFromProperty(EMAIL_ERROR_NOTIFICATION_LIST);
    private static final String EMAIL_NO_APPROVERS_NOTIFICATION_LIST = "gradebook.courseGradeSubmission.email.noApproversNotificationList";
    private static final List<String> emailNoApproversNotificationList = readListFromProperty(EMAIL_NO_APPROVERS_NOTIFICATION_LIST);
    
    //private static final String STUDENT_NUMBER_SAKAI_PROPERTY = "gradebook.empNumKey";
    
    private static final String SUPPORT_EMAIL_SAKAI_PROPERTY = "mail.support";
    public static final String SUPPORT_EMAIL = ServerConfigurationService.getString(SUPPORT_EMAIL_SAKAI_PROPERTY, "");
    
    private static final String EMAIL_SFTP_ERROR_TEMPLATE = "gradebook.courseGradeSubmission.sftpError";
    private static final String EMAIL_SFTP_ERROR_KEY_SUBMISSION_ID = "submissionId";
    private static final String EMAIL_SFTP_ERROR_KEY_APPROVER_EID = "approverEid";
    private static final String EMAIL_SUBMISSION_RECEIPT_TEMPLATE = "gradebook.courseGradeSubmission.submissionReceipt";
    private static final String EMAIL_SUBMISSION_RECEIPT_KEY_SECTION_NAME = "sectionName";
    private static final String EMAIL_SUBMISSION_RECEIPT_KEY_SECTION_EID = "sectionEid";
    private static final String EMAIL_SUBMISSION_RECEIPT_KEY_SUBMISSION_TYPE = "submissionType";
    private static final String EMAIL_SUBMISSION_RECEIPT_KEY_APPROVER_LIST = "approverList";
    private static final String EMAIL_SUBMISSION_NOTICE_TEMPLATE = "gradebook.courseGradeSubmission.submissionNotice";
    private static final String EMAIL_SUBMISSION_NOTICE_KEY_SUBMITTER = "submitter";
    private static final String EMAIL_SUBMISSION_NOTICE_KEY_SECTION_NAME = "sectionName";
    private static final String EMAIL_SUBMISSION_NOTICE_KEY_SECTION_EID = "sectionEid";
    private static final String EMAIL_SUBMISSION_NOTICE_KEY_SUBMISSION_TYPE = "submissionType";
    private static final String EMAIL_SUBMISSION_NOTICE_KEY_GRADEBOOK_LINK = "gradebookLink";
    private static final String EMAIL_KEY_SUPPORT_EMAIL = "emailSupport";
    
    //OWL-697 (mweston4) Email instructor(s) when grades are approved for their section
    private static final String EMAIL_APPROVAL_NOTICE_TEMPLATE = "gradebook.courseGradeSubmission.approvalNotice";
    private static final String EMAIL_APPROVAL_NOTICE_NOCHANGES_TEMPLATE = "gradebook.courseGradeSubmission.approvalNotice.noChange";
    private static final String EMAIL_APPROVAL_NOTICE_KEY_APPROVER = "approver";
    private static final String EMAIL_APPROVAL_NOTICE_KEY_SECTION_NAME = "sectionName";
    private static final String EMAIL_APPROVAL_NOTICE_KEY_SECTION_EID = "sectionEid";
    
    private static final String SAKAI_PASS_GRADE_CODE = "P";
    private static final String SAKAI_FAIL_GRADE_CODE = "NP";
    private static final String REGISTRAR_PASS_GRADE_CODE = "PAS";
    private static final String REGISTRAR_FAIL_GRADE_CODE = "FAI";
    
    public static final String GRADEBOOK_TOOL_ID = "sakai.gradebookng";
    
    private static final String EMAIL_SUFFIX = "@uwo.ca";
    
    private static final int REGISTRAR_GRADE_MIN = 0;
    private static final int REGISTRAR_GRADE_MAX = 100;
    
    private static final String NO_GRADES_READY_MSG_KEY = "finalgrades_no_grades_ready";
    private static final String SINGLE_GRADE_READY_MSG_KEY = "finalgrades_single_grade_ready";
    private static final String GRADES_READY_MSG_KEY = "finalgrades_grades_ready";
    private static final String CURRENT_STATUS_MSG_KEY = "finalgrades_current_status";
    private static final String GRADE_HAS_MSG_KEY = "finalgrades_grade_has";
    private static final String GRADES_HAVE_MSG_KEY = "finalgrades_grades_have";
	
	private static final String FINAL_GRADES_URL = "%s/tool/%s/finalgrades";
    
	private final transient GradebookNgBusinessService bus;
	private final transient OwlBusinessService owlbus;
	private final transient CourseGradeSubmissionPresenter presenter;
    private String siteId;
    private String userEid;
    private String userEmail;
    private String userIp;
    private Set<Membership> currentSectionProvidedMembers;  // OWLTODO: split this into a set of submittable members vs non-submittable members to reduce member iterations later
    private int currentSectionSubmittableMemberCount;
    private String gradebookUid;
    private transient CourseGradeStatistics sectionStats;
    
    // bjones86 - grade submission/approval exclude prefixes
    private static final String EXCLUDE_PREFIXES_KEY = "gradebook.courseGradeSubmission.excludePrefixes";
    private static final List<String> excludePrefixes = readListFromProperty( EXCLUDE_PREFIXES_KEY );
    
    // bjones86 - OWL-1080 - submit usernames instead of student numbers section/username prefix map
    private static final String COMMA_DELIMITER = ",";
    public  static final String SAK_PROP_SUBMIT_USERNAME_PREFIX_MAP = "gradebook.courseGradeSubmission.submitUsername.prefixMap";
    private static final Map<String, Set<String>> submitUsernamePrefixMap = initSubmitUsernamePrefixMap( SAK_PROP_SUBMIT_USERNAME_PREFIX_MAP );
	
	private static final String EMPTY_NAME_PLACEHOLDER = "-";
    
    /**
     * Enables submission/approval of course grades
     * @param service
     * @throws IllegalArgumentException
     * @throws IllegalStateException 
     */
    public CourseGradeSubmitter(GradebookNgBusinessService service, CourseGradeSubmissionPresenter presenter) throws IllegalArgumentException, IllegalStateException
    {
		bus = service;
		owlbus = bus.owl();
		this.presenter = presenter;
        siteId = StringUtils.defaultIfBlank(bus.getCurrentSiteId(), "");
        if (siteId.isEmpty())
        {
            throw new IllegalStateException("Cannot determine current site id");
        }
        
        User currentUser = bus.getCurrentUser();
        if (currentUser == null || StringUtils.isBlank(currentUser.getEid()))
        {
            throw new IllegalStateException("Cannot determine current user.");
        }
        userEid = currentUser.getEid().trim();
        if (StringUtils.isBlank(currentUser.getEmail()))
        {
            userEmail = userEid + EMAIL_SUFFIX;
        }
        else
        {
            userEmail = currentUser.getEmail().trim();
        }
        
        userIp = presenter.getUserIp();
        if (userIp.isEmpty())
        {
            throw new IllegalStateException("Cannot determine current user IP address");
        }
        
        currentSectionProvidedMembers = Collections.emptySet();
        currentSectionSubmittableMemberCount = 0;
        
        gradebookUid = bus.getGradebook().getUid();
        
        sectionStats = null; // lazy-loaded upon first request        
    }
    
    public boolean isSectionSelected()
    {
        refreshCurrentProvidedMembers();
        return !currentSectionProvidedMembers.isEmpty();
    }
    
    public CourseGradeStatistics getStatsForSelectedSection()
    {
        if (sectionStats == null)
        {
            sectionStats = new CourseGradeStatistics(getCurrentCourseGrades());
        }
        
        return sectionStats;
    }
	
	public void setStats(CourseGradeStatistics stats)
	{
		sectionStats = stats;
	}
    
    public int getMissingGradeCount()
    {
        return currentSectionSubmittableMemberCount - (getStatsForSelectedSection().getNumericCount() + getStatsForSelectedSection().getNonNumericCount());
    }
    
    public List<SubmissionHistoryRow> getSubmissionHistoryRowsForSelectedSection()
    {
        List<SubmissionHistoryRow> rows = new ArrayList<>();
        for (OwlGradeSubmission s : getSubmissionHistoryForSelectedSection())
        {
            rows.add(new SubmissionHistoryRow(s));
        }
        
        return rows;
    }
    
    public List<SubmissionHistoryRow> getFirstSubmissionHistoryRowForSelectionSection()
    {
        return getSubmissionHistoryRowsForSelectedSection().subList(0, 1);
    }
    
    public List<SubmissionHistoryRow> getAllButFirstRowOfSubmissionHistoryForSelectionSection()
    {
        return getSubmissionHistoryRowsForSelectedSection().subList(1,getSubmissionHistoryRowsForSelectedSection().size());
    }
    
    public List<OwlGradeSubmission> getSubmissionHistoryForSelectedSection()
    {
        return getSubmissionHistoryForSection(getSelectedSectionEid());
    }
    
    public int getSubmissionHistorySizeForSelectedSection()
    {
        return getSubmissionHistoryForSelectedSection().size();
    }
    
    public List<OwlGradeSubmission> getSubmissionHistoryForSection(String sectionEid)
    {
        return owlbus.fg.getAllCourseGradeSubmissionsForSection(sectionEid);
    }
    
    /**
     * Submits initial grades or revisions. Called when the submit/revise button
     * is clicked in the Course Grade Submission section of the Course Grades page.
     */
    public void submit()
    {
        // Check our prerequisities
        /*
        - A section is selected and it has provided (sakora) members
        - There are grades for this section
        - Current user has permission to submit (submit permission & membership in section with non-submittable role)
        */
		
		Set<OwlGradeSubmissionGrades> currentGrades = getCurrentCourseGrades(); // this call refreshes current members as well
        
        // Check that a section is selected and it has provided members
        if (currentSectionProvidedMembers.isEmpty())
        {
            // abort and update UI
            presenter.presentError("Please select a valid Registrar's section before submitting grades.");
            return;
        }
        
        // Can the current user submit?
        if (!isUserAbleToSubmit(userEid, currentSectionProvidedMembers))
        {
            // abort and update UI
            presenter.presentError("You are not permitted to submit grades for the selected section.");
            return;
        }
        
		// Are there submittable grades for this section?
        StringBuilder msg = new StringBuilder();
        if (!hasSubmittableGrades(currentGrades, msg))
        {
            // abort and update UI
            presenter.presentError(msg.toString());
            return;
        }
                 
        // Process submission
        /*
         * 1. get current course grades in Reg format (already done above)
         * 2. check if previous course grades
         * 2a. if previous grades, check for changes
         * 3. store grades, as revision if appropriate
         * 4. find grade admins for this site
         * 5. notify approvers and submitter via email, include direct link to site
         */
             
        /* *********** 1. get current course grades ************** */
        if (!allGradesWithinRange(currentGrades, REGISTRAR_GRADE_MIN, REGISTRAR_GRADE_MAX))
        {
            // update UI and abort
            presenter.presentError("One or more numeric course grades are outside the allowed range of "
                    + REGISTRAR_GRADE_MIN + " to " + REGISTRAR_GRADE_MAX + ". Please revise and submit again.");
            return;
        }
                
        /* **************** 2. check if previous course grades exist ***************** */
        boolean hasPreviousSubmission = false;
        boolean isRevision = false;
        OwlGradeSubmission existingSubmission = owlbus.fg.getMostRecentCourseGradeSubmissionForSection(getSelectedSectionEid());
        
        /* *** 2a. check for changes (UI should help with this but can't trust the client) *** */
        if (existingSubmission != null)
        {
            // we have a previous submission, and possibly a revision
            hasPreviousSubmission = true;
            Set<OwlGradeSubmissionGrades> prevGrades = getPreviousCourseGrades(existingSubmission);
            if (checkForGradeChanges(currentGrades, prevGrades).hasAddsOrRevisions())
            {
                isRevision = true;
            }
        }
        
        /* **************** 3. store grades, as revision if appropriate ************* */
        if (hasPreviousSubmission && !isRevision)
        {
            // no change in grades
            // OWLTODO: Handle this better
            presenter.presentError("No grade changes. You must add or change at least one grade before submitting a revision.");
            return;
        }
        
        OwlGradeSubmission submission = new OwlGradeSubmission();
        submission.setSiteId(siteId);
        submission.setSectionEid(getSelectedSectionEid());
        submission.setSubmissionDate(new Date());
        submission.setUserEid(userEid);
        submission.setUserIp(userIp);         
        submission.setStatusCode(OwlGradeSubmission.PENDING_APPROVAL_STATUS);
        submission.setGradeData(currentGrades);
        if (isRevision)
        {
            submission.setPrevSubmission(existingSubmission);
        }
        
        try
        {
            owlbus.fg.createSubmission(submission);
            if (isRevision)
            {
                existingSubmission.setStatusCode(OwlGradeSubmission.DISCARDED_STATUS);
                owlbus.fg.updateSubmission(existingSubmission);
            }
            
            // 4. find grade admins for this site
			Set<User> approvers = getApproversFromMembership(currentSectionProvidedMembers);
            String gradeAdmins = "";
            String prefix = "";
            for (User approver : approvers)
            {
                gradeAdmins += prefix + approver.getEmail();
                prefix = ", ";
            }
                   
            // 5. notify grade admins and instructor via email, include direct link to site
            if (EMAIL_ENABLED)
            {
                EmailTemplateService emailTemplateService = (EmailTemplateService) ComponentManager.get(EmailTemplateService.class);
                EmailService emailService = (EmailService) ComponentManager.get(EmailService.class);
                //List<String> recipients = new ArrayList<String>();
                Map<String, Object> replacementValues = new HashMap<>();
                
                CourseManagementService courseManagementService = (CourseManagementService) ComponentManager.get(CourseManagementService.class);
                Section section = courseManagementService.getSection(submission.getSectionEid());
                
                DeveloperHelperService dhs = (DeveloperHelperService) ComponentManager.get(DeveloperHelperService.class);
                
                // send receipt to submitter
                replacementValues.put(EMAIL_SUBMISSION_RECEIPT_KEY_SECTION_NAME, section.getTitle());
                replacementValues.put(EMAIL_SUBMISSION_RECEIPT_KEY_SECTION_EID, section.getEid());
                replacementValues.put(EMAIL_SUBMISSION_RECEIPT_KEY_SUBMISSION_TYPE, submission.getSubmissionType());
                replacementValues.put(EMAIL_SUBMISSION_RECEIPT_KEY_APPROVER_LIST, gradeAdmins);
                replacementValues.put( EMAIL_KEY_SUPPORT_EMAIL, SUPPORT_EMAIL );
                //recipients.add( userRef );
                //emailTemplateService.sendRenderedMessages(EMAIL_SUBMISSION_RECEIPT_TEMPLATE, recipients, replacementValues, SUPPORT_EMAIL, SUPPORT_EMAIL);
                RenderedTemplate template = emailTemplateService.getRenderedTemplate(EMAIL_SUBMISSION_RECEIPT_TEMPLATE, Locale.ENGLISH, replacementValues);
                List<InternetAddress> emailRecipients = new ArrayList<>();
                try
                {
                    emailRecipients.add(new InternetAddress(userEmail));
                    emailService.sendMail(new InternetAddress(SUPPORT_EMAIL), emailRecipients.toArray(new InternetAddress[0]), template.getRenderedSubject(),
                            template.getRenderedMessage(), null, null, null);
                    
                    // bjones86 - OWL-966 - log course grade submission
                    String logMsg = String.format( "Email receipt for submission <%s> sent to <%s> for section <%s> in site <%s>", submission.getId(),
                            userEmail, submission.getSectionEid(), submission.getSiteId() );
                    LOG.info( LOG_PREFIX + logMsg );
                }
                catch (AddressException addressException)
                {
                    // now we panic
                    String logMsg = String.format( "Unable to send email receipt to submitter for submission <%s> for section <%s> in site <%s>", submission.getId(),
                            submission.getSectionEid(), submission.getSiteId() );
                    LOG.error( LOG_PREFIX + logMsg, addressException );
                }
                
                // send notice to approvers
                //String linkToGradebook = dhs.getToolViewURL(GRADEBOOK_TOOL_ID, null, null, null);
				SakaiToolData td = dhs.getToolData(GRADEBOOK_TOOL_ID, null);
				String toolId = td.getPlacementId();
				String linkToGradebook = String.format(FINAL_GRADES_URL, dhs.getLocationReferenceURL(td.getLocationReference()), toolId);
                replacementValues.clear();
                replacementValues.put(EMAIL_SUBMISSION_NOTICE_KEY_SUBMITTER, userEid);
                replacementValues.put(EMAIL_SUBMISSION_NOTICE_KEY_SECTION_NAME, section.getTitle());
                replacementValues.put(EMAIL_SUBMISSION_NOTICE_KEY_SECTION_EID, section.getEid());
                replacementValues.put(EMAIL_SUBMISSION_NOTICE_KEY_SUBMISSION_TYPE, submission.getSubmissionType());
                replacementValues.put(EMAIL_SUBMISSION_NOTICE_KEY_GRADEBOOK_LINK, linkToGradebook);
                replacementValues.put( EMAIL_KEY_SUPPORT_EMAIL, SUPPORT_EMAIL );
                
                emailRecipients.clear();
                for (User approver : approvers)
                {
                    try
                    {
                        emailRecipients.add(new InternetAddress(approver.getEmail()));
                    }
                    catch (AddressException ex)
                    {
                        // log and move on to next user
                        LOG.info(LOG_PREFIX + "Bad email address: " + approver.getEmail() + " for approver:" + approver.getEid());
                    }
                }
                
                try
                {
                    if (emailRecipients.isEmpty() && !isDemoSection(section.getEid()))
                    {
                        // No approvers and not demo section, notify Registrar
                        for (String email : emailNoApproversNotificationList)
                        {
                            emailRecipients.add(new InternetAddress(email));
                        }
                    }
                    
                    if (!emailRecipients.isEmpty()) // don't send email if still no recipients at this point
                    {
                        template = emailTemplateService.getRenderedTemplate(EMAIL_SUBMISSION_NOTICE_TEMPLATE, Locale.ENGLISH, replacementValues);
                        emailService.sendMail(new InternetAddress(SUPPORT_EMAIL), emailRecipients.toArray(new InternetAddress[0]), template.getRenderedSubject(),
                            template.getRenderedMessage(), null, null, null);
                        
                        // bjones86 - OWL-966 - log course grade submission
                        String recipients = "";
                        prefix = "";
                        for( InternetAddress email : emailRecipients )
                        {
                            recipients += prefix + email.getAddress();
                            prefix = ", ";
                        }
                        String logMsg = String.format( "Email notification to approvers for submission <%s> sent to <%s> for section <%s> in site <%s>", 
                                submission.getId(), recipients, submission.getSectionEid(), submission.getSiteId() );
                        LOG.info( LOG_PREFIX + logMsg );
                    }
                }
                catch (AddressException addressException)
                {
                    // now we panic
                    String logMsg = String.format( "Unable to send email notification to approvers for submission <> for section <> in site <>", submission.getId(),
                            submission.getSectionEid(), submission.getSiteId() );
                    LOG.error( LOG_PREFIX + logMsg, addressException );
                }
                
            }
            
            // bjones86 - OWL-1003 - if no grade admins for the section, present differnet UI message to user indicating so
            String message = "Grades submitted.";
            if( approvers.isEmpty() )
                message += " These grades will be reviewed for approval when a grade admin has been assigned to this section by your department.";
            else
                message += " Grade admins (" + gradeAdmins + ") have been notified via email.";
            presenter.presentMsg(message);
        }
        catch (NestedRuntimeException he)
        {
            // submission can't be created, so we'll notify the user and return?
            LOG.error(LOG_PREFIX + "Hibernate error during submission by " + userEid + ": " + he.getMessage(), he);
            presenter.presentError("There was an error saving data to the database. Please try again later. If the problems persists, please contact " + SUPPORT_EMAIL);
        }
		
    } // end submit
    
    
    /**
     * Approves grades and sends them to the Registrar. Called when the approve
     * button is clicked in the Course Grade Submission section of the Course Grades page.
     * @param event the event object
     */
    public void approve()
    {
        // check prerequisites
        OwlGradeSubmission existingSubmission = owlbus.fg.getMostRecentCourseGradeSubmissionForSection(getSelectedSectionEid());
        StringBuilder message = new StringBuilder();
        if (!isSectionReadyForApprovalByCurrentUser(message))
        {
            // abort and update UI
            presenter.presentError(message.toString());
            return;
        }
        
        //OWL-697 (mweston4) Email instructor(s) when grades are approved for their section
        Section section = owlbus.fg.getSectionByEid(existingSubmission.getSectionEid());
        
        /*
         * 1. generate csv of grades (only those that have changed)
         * 2. sftp to registrar
         * 3. set status of latest submission to "approved"
         */
        
        /* 1. generate csv of grades (only those that have changed) */
        CourseGradeSubmissionCsv csv = new CourseGradeSubmissionCsv();
        Date approvalDate = new Date();
        csv.setApprovalDate(approvalDate);
        csv.setSectionEid(existingSubmission.getSectionEid());
        
        // is this a revised approval?
        Optional<OwlGradeSubmission> previousApprovedSubmission = findPreviousApproval(existingSubmission.getPrevSubmission());
        boolean gradesAdded;
        boolean uploaded = false;
        boolean skipUpload = false;
        boolean sftpEnabled = ServerConfigurationService.getBoolean(SFTP_ENABLED_SAKAI_PROPERTY, false);
        if (!previousApprovedSubmission.isPresent())
        {
            gradesAdded = csv.addGrades(existingSubmission.getGradeData());
        }
        else
        {
            gradesAdded = csv.addRevisedGrades(existingSubmission.getGradeData(), previousApprovedSubmission.get().getGradeData());
        }
                
        /* 2. sftp csv to registrar */
        if (gradesAdded)
        {
            if (sftpEnabled)
            {
                // check to see if we are dealing with a "demo" section
                if (!isDemoSection(existingSubmission.getSectionEid()))
                {
                    uploaded = csv.sftpToRegistrar();
                }
                else
                {
                    // don't transfer to registrar, just mark as uploaded and continue
                    uploaded = true;
                }
            }
        }
        else
        {
            skipUpload = true;
        }
        
        //OWL-697 (mweston4) Email instructor(s) when grades are approved for their section
        EmailTemplateService emailTemplateService = null;        
        RenderedTemplate template = null;
		EmailService emailService = null;
        Set<InternetAddress> recipients = null;
        
        if (EMAIL_ENABLED)
		{
			emailTemplateService = (EmailTemplateService) ComponentManager.get(EmailTemplateService.class);
            emailService = (EmailService) ComponentManager.get(EmailService.class);
            recipients = new HashSet<>();            
        }        
        
        /* 3. set status of latest submission to approved if uploaded */
 
        if (uploaded || !sftpEnabled || skipUpload)
        {   
            boolean upload;
            if (skipUpload)
            {
                upload = Boolean.FALSE;
            }
            else
            {
                upload = Boolean.TRUE;
            }
            OwlGradeApproval approval = new OwlGradeApproval();
            approval.setApprovalDate(approvalDate);
            approval.setUploadedToRegistrar(upload);
            approval.setUserEid(userEid);
            approval.setUserIp(userIp);
        
            try
            {
                owlbus.fg.createApproval(approval);
                existingSubmission.setApproval(approval);
                existingSubmission.setStatusCode(OwlGradeSubmission.APPROVED_STATUS);
                owlbus.fg.updateSubmission(existingSubmission);
            }
            catch (NestedRuntimeException he)
            {
                // approval can't be created, or submission can't be updated, so we'll notify the user and return?
                LOG.error(LOG_PREFIX + "Error saving approval to database: " + he.getMessage(), he);
                presenter.presentError("An error occurred saving data to the database.");
            }
            
            //OWL-697 (mweston4) Email instructor(s) when grades are approved for their section
            StringBuilder msg = new StringBuilder();
            Map<String, Object> placeholderValues = new HashMap<>();
            if (EMAIL_ENABLED){
                    
                    //Populate email template placeholders
                    placeholderValues.put(EMAIL_APPROVAL_NOTICE_KEY_APPROVER, userEid);
                    placeholderValues.put(EMAIL_APPROVAL_NOTICE_KEY_SECTION_NAME, section.getTitle());
                    placeholderValues.put(EMAIL_APPROVAL_NOTICE_KEY_SECTION_EID, section.getEid());
                    placeholderValues.put(EMAIL_KEY_SUPPORT_EMAIL, SUPPORT_EMAIL);        
            }
            
            if (skipUpload)
            {
                //OWL-697 (mweston4) Email instructor(s) when grades are approved for their section
                if (EMAIL_ENABLED && emailTemplateService != null)
                {
                    template = emailTemplateService.getRenderedTemplate(EMAIL_APPROVAL_NOTICE_NOCHANGES_TEMPLATE, new Locale( "en" ), placeholderValues);
                }
                
                msg.append("Grades approved, but were identical to the previously approved grades and therefore not transferred to the Office of the Registar. No additional action is required.");
            }
            else
            {
                //OWL-697 (mweston4) Email instructor(s) when grades are approved for their section
                if (EMAIL_ENABLED && emailTemplateService != null) {
                    template = emailTemplateService.getRenderedTemplate(EMAIL_APPROVAL_NOTICE_TEMPLATE, new Locale( "en" ), placeholderValues);
                }
                
                msg.append("Grades approved and securely transferred to the Office of the Registrar.");
            }
            
            //OWL-697 (mweston4) Email instructor(s) when grades are approved for their section
            if (EMAIL_ENABLED) {
                try {
                    //Get email addresses of grade admins and instructors for the current section
                    refreshCurrentProvidedMembers();
                    if (!currentSectionProvidedMembers.isEmpty()) {
                        StringBuilder notificationList = new StringBuilder();
                        
                        //Get submitters (i.e. instructors)
                        Set<User> usersToEmail = getSubmittersFromMembership(currentSectionProvidedMembers);
                        
                        //Get the grade admin who approved these grades
                        UserDirectoryService userDirectoryService = (UserDirectoryService) ComponentManager.get(UserDirectoryService.class);
                        try {
                            User approver = userDirectoryService.getUserByEid(userEid);
                            usersToEmail.add(approver);
                        } catch (UserNotDefinedException e) {
                            //ignore 
                        }
                        
                        // bjones86 - OWL-996
                        String recipientAddresses = "";
                        String prefix = "";
                        
                        int countUsersToEmail = 0;
                        for (User u : usersToEmail) {
                            if (u.getEmail() == null || u.getEmail().isEmpty()) {
                                LOG.warn(LOG_PREFIX + "Unable to send Grade Approval email notification to " + u.getEid() + ". Reason: No email addresss");
                            } else {
                                recipients.add(new InternetAddress(u.getEmail()));
                                
                                // bjones86 - OWL-996
                                recipientAddresses += prefix + u.getEmail();
                                prefix = ", ";
                                
                                //Generate a list of user ids who we are notifying by email
                                notificationList.append(u.getEid());
                                ++countUsersToEmail;
                                if (countUsersToEmail < usersToEmail.size()) {
                                    notificationList.append(", ");
                                }
                            }                           
                        }                                                
                          
                        //Send out the email if we found anyone to email
                        if (!recipients.isEmpty()) {
                            emailService.sendMail(new InternetAddress(SUPPORT_EMAIL), recipients.toArray(new InternetAddress[0]), template.getRenderedSubject(), template.getRenderedMessage(), null, null, null);
                            if (notificationList.length() > 0) {
                                msg.append(" The following users have been notified by email (").append(notificationList).append(").");
                            }
                            
                            // bjones86 - OWL-966 - log course grade approval
                            String logMsg = String.format( "Email notification for approval <%s> by <%s> sent to <%s> for section <%s> in site <%s>", 
                                    approval.getId(), approval.getUserEid(), recipientAddresses, existingSubmission.getSectionEid(), existingSubmission.getSiteId() );
                            LOG.info( LOG_PREFIX + logMsg );
                            
                        } else {
                            LOG.error( LOG_PREFIX + "We don't have any email addresses to notify this grade approval"); 
                        }
   
                    } else {
                        LOG.error(LOG_PREFIX + "We don't have any email addresses to notify this grade approval");
                    }
                } catch (AddressException e) {
                   LOG.error( LOG_PREFIX + "Unable to send Grade Approval email notification", e); 
                }
            }
            
            presenter.presentMsg(msg.toString());
        }
        else
        {
            // Notifiy the instructional team and registrar about sftp failure (if email is enabled)
            if (EMAIL_ENABLED && emailTemplateService != null && emailService != null)
            {
            	try
            	{
	                // Load up the replacement values
	                Map<String, Object> replacementValues = new HashMap<>();
	                replacementValues.put( EMAIL_SFTP_ERROR_KEY_SUBMISSION_ID, existingSubmission.getId().toString() );
	                replacementValues.put( EMAIL_SFTP_ERROR_KEY_APPROVER_EID, userEid );
	                replacementValues.put( EMAIL_KEY_SUPPORT_EMAIL, SUPPORT_EMAIL );
	                InternetAddress supportEmail = new InternetAddress( SUPPORT_EMAIL );
	                
	                // Get the rendered template
	                RenderedTemplate sftpTemplate = emailTemplateService.getRenderedTemplate( EMAIL_SFTP_ERROR_TEMPLATE, new Locale( "en" ), replacementValues );
	                
	                // Load up the recipients list
	                recipients = new HashSet<>();
                        String recipientsString = "";
                        String prefix = "";
	                for( String email : emailErrorNotificationList )
                        {
	                	recipients.add( new InternetAddress( email ) );
                                recipientsString += email + prefix;
                                prefix = ", ";
                        }
	                
	                // Use the generic EmailService to send the email
	                emailService.sendMail( supportEmail, recipients.toArray( new InternetAddress[0] ), sftpTemplate.getRenderedSubject(), 
	                		sftpTemplate.getRenderedMessage(), null, null, null );
                        
                        // bjones86 - OWL-966 - log course grade sftp failure email
                        String logMsg = String.format( "Email notification of SFTP failure for approval <%s> by <%s> sent to <%s>", 
                                existingSubmission.getId(), userEid, recipientsString );
                        LOG.info( LOG_PREFIX + logMsg );
            	}
            	catch( AddressException e )
            	{
            		LOG.error( LOG_PREFIX + "Unable to send email notification of SFTP failure.", e );
            	}
            }
            
            LOG.error(LOG_PREFIX + "SFTP transfer to Registrar by " + userEid + " failed for submission: " + existingSubmission.getId());
            presenter.presentError("An error occurred transfering grades to the Registrar. Please try again later. If the problem persists, please contact " + SUPPORT_EMAIL);
        }
        
    }
    
    public boolean hasSubmittableGrades(Set<OwlGradeSubmissionGrades> currentGrades, StringBuilder messageRef)
    {	
        boolean submittable = true;
        if (currentGrades.isEmpty())
        {
            messageRef.append("There are no valid grades to submit for the selected section.");
            submittable = false;
        }
        else if (getGradeChangeReport(currentGrades).hasAddsOrRevisions() == false)
        {
            messageRef.append("No grades have been added or modified since last submission.");
            submittable = false;
        }
        
        return submittable;
    }
    
    public GradeChangeReport getGradeChangeReport(Set<OwlGradeSubmissionGrades> currentGrades)
    {
        if (isSectionSelected())
        {
            OwlGradeSubmission existingSubmission = owlbus.fg.getMostRecentCourseGradeSubmissionForSection(getSelectedSectionEid());
            Set<OwlGradeSubmissionGrades> prevGrades = new HashSet<>();
            if (existingSubmission != null)
            {
                prevGrades = getPreviousCourseGrades(existingSubmission);
            }
            return checkForGradeChanges(currentGrades, prevGrades);
        }
        
        return new GradeChangeReport();
    }
    
    private GradeChangeReport checkForGradeChanges(Set<OwlGradeSubmissionGrades> currentGrades, Set<OwlGradeSubmissionGrades> previousGrades)
    {
        GradeChangeReport changes = new GradeChangeReport();
        
		// OWLTODO: keying on student number here fails to detect changes if student has no student number
		// is having no student number valid (CStudies?)? switch to eid instead?
        Map<String, OwlGradeSubmissionGrades> currentGradeMap = new HashMap<>();
        for (OwlGradeSubmissionGrades grade : currentGrades)
        {
            currentGradeMap.put(grade.getStudentNumber(), grade);
        }
        
        Map<String, OwlGradeSubmissionGrades> prevGradeMap = new HashMap<>();
        for (OwlGradeSubmissionGrades grade : previousGrades)
        {
            prevGradeMap.put(grade.getStudentNumber(), grade);
        }
        
        Set<String> newStudents = new HashSet<>();
        Set<String> sameStudents = new HashSet<>();
        Set<String> changedStudents = new HashSet<>();
        Set<String> missingStudents = new HashSet<>();
        
        sameStudents.addAll(currentGradeMap.keySet());
        sameStudents.retainAll(prevGradeMap.keySet());
        
        newStudents.addAll(currentGradeMap.keySet());
        newStudents.removeAll(sameStudents);
                
        missingStudents.addAll(prevGradeMap.keySet());
        missingStudents.removeAll(sameStudents);
        
        for (String stu : sameStudents)
        {
            if (!currentGradeMap.get(stu).getGrade().equals(prevGradeMap.get(stu).getGrade()))
            {
                changedStudents.add(stu);
            }
        }
        
        changes.setNewStudentCount(newStudents.size());
        changes.setMissingStudentCount(missingStudents.size());
        changes.setRevisedStudentCount(changedStudents.size());
        
        return changes;
    }
    
    private boolean allGradesWithinRange(Set<OwlGradeSubmissionGrades> grades, int min, int max)
    {
        boolean withinRange = true;
        for (OwlGradeSubmissionGrades grade : grades)
        {
            try
            {
                int g = Integer.parseInt(grade.getGrade());
                if (g < min || g > max)
                {
                    withinRange = false;
                    break;
                }
            }
            catch (NumberFormatException nfe)
            {
                // non-numeric value, just skip it
            }
        }
        
        return withinRange;
    }
    
    public boolean isSectionReadyForApprovalByCurrentUser()
    {
        return isSectionReadyForApprovalByCurrentUser(new StringBuilder());
    }
    
    private boolean isSectionReadyForApprovalByCurrentUser(StringBuilder messageRef)
    {          
        boolean ready = true;
        
        // Check prerequisites
        /*
        - there is a section selected that has provided members
        - there is a submission pending approval
        - the current user can approve grades
        - the new grades are not exactly the same as the old grades
        */
        
        //refreshCurrentProvidedMembers();
        OwlGradeSubmission sub = owlbus.fg.getMostRecentCourseGradeSubmissionForSection(getSelectedSectionEid());
        
        if (currentSectionProvidedMembers.isEmpty())
        {
            messageRef.append("Please select a valid Registrar's section before approving grades.");
            ready = false;
        }
        else if (!isUserAbleToApprove(userEid, currentSectionProvidedMembers))
        {
            messageRef.append("You are not permitted to approve grades for the selected section.");
            ready = false;
        }
        else if (sub == null || OwlGradeSubmission.PENDING_APPROVAL_STATUS != sub.getStatusCode())
        {
            messageRef.append("There are no submissions pending approval for the selected section.");
            ready = false;
        }
        
        return ready;
    }
             
    public Set<OwlGradeSubmissionGrades> getCurrentCourseGrades()
    {
		// OWLTODO: this method is quite slow....
		// split into getCurrent... and refreshCurrent... ?
		
		// OWL NOTE: refactored this method heavily for GBNG
		
		OwlGbStopWatch stopwatch = new OwlGbStopWatch("getCurrentCourseGradesTimer");
		
        Set<OwlGradeSubmissionGrades> finalGrades = new HashSet();
          
        // get all course grades from gradebook for the selected section
        GbGroup section = presenter.getSelectedSection();
        if (section == null)
        {
            // no selected section, just return early
            return finalGrades;
        }
		String selectedSectionEid = owlbus.findSectionEid(section).orElse("");

		refreshCurrentProvidedMembers();
		stopwatch.time("refreshCurrentProvidedMembers");
        List<OwlGbStudentCourseGradeInfo> courseGrades = owlbus.fg.getSectionCourseGrades(section);
		stopwatch.time("bus.getSectionCourseGrades");
		
        // convert valid course grade records to Registrar format
		Gradebook gb = bus.getGradebook(); // pay this cost up front, instead of once for each grade override
        GradeMapping gradeMapping = gb.getSelectedGradeMapping();
        int count = 0;
        for (OwlGbStudentCourseGradeInfo record : courseGrades)
        {   
			OwlGbUser student = record.student;
            ++count;
            try
            {
                OwlGradeSubmissionGrades grade = new OwlGradeSubmissionGrades();
				// In OWL we do not have a ContextualUserDisplayService or a DisplayAdvisor, so displayId = eid
				String studentEid = student.gbUser.getDisplayId();
                if (!isOfficialStudent(studentEid, currentSectionProvidedMembers))
                {
                    continue; // skip unofficial students
                }
                grade.setStudentEid(studentEid);
				
				// OWL-1187  --plukasew
				// check for null or empty names, log and replace with placeholder
				String firstName = student.gbUser.getFirstName();
				if (StringUtils.isBlank(firstName))
				{
					firstName = EMPTY_NAME_PLACEHOLDER;
					LOG.error(LOG_PREFIX + "No first name found for user " + studentEid +
                            " in section " + selectedSectionEid + " of site " + siteId);
				}
				String lastName = student.gbUser.getLastName();
				if (StringUtils.isBlank(lastName))
				{
					lastName = EMPTY_NAME_PLACEHOLDER;
					LOG.error(LOG_PREFIX + "No last name found for user " + studentEid +
                            " in section " + selectedSectionEid + " of site " + siteId);
				}
                grade.setStudentFirstName(firstName);
                grade.setStudentLastName(lastName);
                if (localDebugModeEnabled)
                {
                    // when running locally we don't have student numbers from ldap, so
                    // we use this reasonable facsimile
                    grade.setStudentNumber("12345" + count);
                }
                else if (sectionAndUsernameMatchesPrefixList(student, selectedSectionEid)) // OWL-1212 substitute username for student number in the database  --plukasew
                {
                    grade.setStudentNumber(studentEid);
                }
                else
                {
                    grade.setStudentNumber(getStudentNumber(student));
                }
				
                grade.setGrade(courseGradeToRegistrarGrade(record, gradeMapping));
                finalGrades.add(grade);
            }
            catch (MissingStudentNumberException msne)
            {
                // skip students with no student number
                LOG.error(LOG_PREFIX + "No student number found for user " + student.gbUser.getUserUuid() +
                            " in section " + selectedSectionEid + " of site " + siteId);
            }
            catch (MissingCourseGradeException mcge)
            {
                // skip students with no grade entered (see OWL-245) 
                LOG.info(LOG_PREFIX + "No course grade found for user " + student.gbUser.getUserUuid() + " in section "
                            + selectedSectionEid + " of site " + siteId);
            }
            catch (InvalidGradeException ige)
            {
                // skip students with invalid grades (similar to above)
                LOG.error(LOG_PREFIX + "Invalid grade found for user " + student.gbUser.getUserUuid() +
                            " in section " + selectedSectionEid + " of site " + siteId, ige);
            }
        }
        stopwatch.time("convert course grades to Final Grades - method complete");
		
        return finalGrades;
    }
    
    private Set<OwlGradeSubmissionGrades> getPreviousCourseGrades(OwlGradeSubmission submission)
    {
        Set<OwlGradeSubmissionGrades> prevCourseGrades = new HashSet<>();
        int submissionStatus = submission.getStatusCode();
        if (submissionStatus == OwlGradeSubmission.PENDING_APPROVAL_STATUS
                || submissionStatus == OwlGradeSubmission.APPROVED_STATUS)
        {
            prevCourseGrades.addAll(submission.getGradeData());
        }
        
        return prevCourseGrades;
    }
    
    private boolean isOfficialStudent(String eid, Set<Membership> members)
    {
        boolean official = false;
        if (eid != null && !eid.isEmpty())
        {
            for (Membership m : members)
            {
                if (eid.equals(m.getUserId()))
                {
                    official = rolesToSubmit.contains(m.getRole());
                    break;
                }
            }
        }
        
        return official;
    }
    
    private String getStudentNumber(OwlGbUser student) throws MissingStudentNumberException
    {
		String number = student.gbUser.getStudentNumber();
		
		if (number.isEmpty())
		{
			throw new MissingStudentNumberException("Couldn't find student number for user: " + student.gbUser.getDisplayId());
		}
		
        return number;
    }
    
    /**
     * Check if the given section identifier starts with any of the section prefixes defined in sakai.properties;
     * if true, check if username starts with any of the section prefix specific username prefixes defined in sakai.properties.
     * 
     * @author bjones86
     * @param user the user object to check username
     * @return true/false if section identifier starts with one of the prefixes AND username starts with one of the section prefix specific username prefix list
     */
    private boolean sectionAndUsernameMatchesPrefixList( OwlGbUser user, String sectionEID )
    {
        // Short circuit
        //String sectionEID = getSelectedSectionEid();
        if( sectionEID == null || sectionEID.isEmpty() || user == null )
            return false;
        
        // Find the matching section prefix
        String sectionPrefix = checkForUsernameSubmissionPrefix(sectionEID);
        
        // If a section prefix match was found, return true/false if username starts with any prefix from the section prefix specific username prefix list
        if( !sectionPrefix.isEmpty() )
            return StringUtils.startsWithAny( user.gbUser.getDisplayId(),
                submitUsernamePrefixMap.get( sectionPrefix ).toArray( new String[submitUsernamePrefixMap.get( sectionPrefix ).size()] ) );
        
        // No section prefix match, return false
        return false;
    }
    
    public static String checkForUsernameSubmissionPrefix(String sectionEid)
    {
        for (String prefix : submitUsernamePrefixMap.keySet())
        {
            if (sectionEid.startsWith(prefix))
            {
                return prefix;
            }
        }
        
        return "";
    }
    
    /**
     * Initialize the section/username prefix map from sakai.properties
     * 
     * @author bjones86
     * @param propName the sakai.property key containing the section->username prefix lists
     * @return initialized map, where each section prefix (key) has a list of acceptable username prefixes
     */
    public static Map<String, Set<String>> initSubmitUsernamePrefixMap( String propName )
    {
        List<String> prefixList = readListFromProperty( propName );
        Map<String, Set<String>> prefixMap = new HashMap<>();
        for( String prefixEntry : prefixList )
        {
            String[] entry = prefixEntry.split( COMMA_DELIMITER );
            String sectionPrefix = entry[0];
            String usernamePrefix = entry[1];
            
            if( prefixMap.keySet().contains( sectionPrefix ) )
                prefixMap.get( sectionPrefix ).add( usernamePrefix );
            else
                prefixMap.put( sectionPrefix, new HashSet<>( Arrays.asList( usernamePrefix ) ) );
        }
        
        return prefixMap;
    }
    
    private String courseGradeToRegistrarGrade(OwlGbStudentCourseGradeInfo record, GradeMapping mapping) throws InvalidGradeException, MissingCourseGradeException
    {
        String finalGrade;
		GbCourseGrade gbcg = record.getCourseGrade();
		Optional<String> override = OwlGbCourseGrade.getOverride(gbcg);
        if (override.isPresent()) // we have an overridden grade, as a string
        {
            finalGrade = overriddenGradeAsGradeString(override.get(), mapping);
        }
        else // we have a numeric grade, or no grade was recorded
        {
			// getCalculatedGrade was originally a non-null toString'd Double, so this should be safe
            if (!OwlGbCourseGrade.getCalculatedGrade(gbcg).isPresent())
			{
				throw new MissingCourseGradeException("No course grade entered for student: " + record.student.gbUser.getUserUuid());
			}
            finalGrade = FinalGradeFormatter.formatForRegistrar(gbcg);
        } 
        
        if (finalGrade.isEmpty() || finalGrade.length() != 3)
        {
            throw new InvalidGradeException("Could not find valid course grade for student: " + record.student.gbUser.getUserUuid());
        }
        
        return finalGrade;
    }
    
    private String overriddenGradeAsGradeString(String grade, GradeMapping gradeMapping) throws IllegalArgumentException, InvalidGradeException
    {	
        if (grade == null)
        {
            throw new IllegalArgumentException("Grade cannot be null");
        }
        
		// OWLTODO: revise all this for GBNG
		// we're validating on the wicket side, we should use the same process here?
		
        // get the current grade mapping
        // OWLTODO: Figure out what to do if the grade mapping is not the standard one (ie. missing NGR, etc.)
        //Gradebook gb = bean.getGradebookManager().getGradebook(bean.getGradebookBean().getGradebookId());

        if (gradeMapping == null || gradeMapping.getGradeMap().isEmpty())
        {
            throw new IllegalStateException("No letter grade mapping defined for this gradebook");
        }
        
        String finalGrade;
        String g = grade.trim();
		if (isNumber(g) || registrarGradeCodes.contains(g)) // we have a valid numeric grade
        {
            finalGrade = FinalGradeFormatter.overrideToRegistrarFinal(g);
        }
        else if (SAKAI_PASS_GRADE_CODE.equals(g))
        {
            finalGrade = REGISTRAR_PASS_GRADE_CODE;
        }
        else if (SAKAI_FAIL_GRADE_CODE.equals(g))
        {
            finalGrade = REGISTRAR_FAIL_GRADE_CODE;
        }
        else if (gradeMapping.getGradeMap().containsKey(g)) // have a non-Registrar, non-numeric letter grade
        {
            Double doubleGrade = gradeMapping.getValue(g);
            if (doubleGrade == null)
            {
                throw new InvalidGradeException("Could not convert trimmed letter grade to percentage. Grade: " + g);
            }
            finalGrade = FinalGradeFormatter.padNumeric(FinalGradeFormatter.round(doubleGrade));
        }
        else
        {
            throw new InvalidGradeException("Could not parse valid grade from trimmed argument: " + g);
        }
        
        return finalGrade;
    }
    
    private boolean isNumber(String value)
    {
        boolean result = true;
        try
        {
            Double.parseDouble(value);
        }
        catch (NumberFormatException nfe)
        {
            result = false;
        }
        
        return result;
    }
	
	public GbGroup getSelectedSection()
	{
		return presenter.getSelectedSection();
	}
    
    private String getSelectedSectionEid()
    {
		return owlbus.findSectionEid(presenter.getSelectedSection()).orElse("");
	}
    
    private void refreshCurrentProvidedMembers()
    {
        // OWLTODO: Provided members aren't going to change very often, so we should
        // add a check here to see if the selected section was the same as the last
        // time we checked this and just return the current membership
		// OR investigate caching CMS
        try
        {
            currentSectionProvidedMembers = owlbus.fg.getSectionMemberships(getSelectedSectionEid());
            currentSectionSubmittableMemberCount = 0;
            for (Membership m : currentSectionProvidedMembers)
            {
                if (rolesToSubmit.contains(m.getRole()))
                {
                    ++currentSectionSubmittableMemberCount;
                }
            }
        }
        catch (IdNotFoundException idNotFoundException)
        {
            currentSectionProvidedMembers = Collections.emptySet();
        }
    }
    
    private Set<User> getApproversFromMembership(Set<Membership> members)
    {
        Set<User> approvers = new HashSet<>();
        UserDirectoryService userDirectoryService = (UserDirectoryService) ComponentManager.get(UserDirectoryService.class);
        SiteService siteService = (SiteService) ComponentManager.get( SiteService.class );
        Map<String, String> userRoleMap = bus.owl().owlAuthz().getSectionUserRoleMap(getSelectedSectionEid());
        Set<org.sakaiproject.authz.api.Role> siteRoles = bus.owl().owlAuthz().getSiteRolesForGradebook(gradebookUid);
        Site site = null;
        try
        {
            site = siteService.getSite( siteId );
        }
        catch( IdUnusedException ex )
        {
            // Don't really care about this, but it would be a strange occurance
            LOG.warn( "Can't find site with id = " + siteId, ex );
        }

        for (Membership m : members)
        {
            if (!rolesToSubmit.contains(m.getRole()))
            {
                try
                {
                    User u = userDirectoryService.getUserByEid(m.getUserId());
                    if (bus.owl().owlAuthz().isUserAbleToApproveCourseGrades(u.getEid(), siteRoles, userRoleMap))
                    {
                        // OWL-2059 - only add user to list of approvers if they're 'active' in the site
                        boolean addUserToApproversList = true;
                        if( site != null )
                        {
                            if( !site.getMember( u.getId() ).isActive() )
                            {
                                addUserToApproversList = false;
                            }
                        }

                        if( addUserToApproversList )
                        {
                            approvers.add( u );
                        }
                    }
                }
                catch (UserNotDefinedException userNotDefinedException)
                {
                    // don't care, just skip user
                }
            }
        }

        return approvers;
    }
    
    /*
     * getSubmittersFromMembership
     * OWL-697 (mweston4) Email instructor(s) when grades are approved for their section
     * @author mweston4
     * @param members set of enrolled members
     * @return A set of unique users who are granted the permissions necessary to submit course grades
     */
    public Set<User> getSubmittersFromMembership(Set<Membership> members) {
        Set<User> submitters = new HashSet<>();
        
        // bjones86 - grade submission/approval exclude prefixes
        if(!isSectionInExcludePrefixList()) {
                 
            UserDirectoryService userDirectoryService = (UserDirectoryService) ComponentManager.get(UserDirectoryService.class);       
            Map<String, String> userRoleMap = bus.owl().owlAuthz().getSectionUserRoleMap(getSelectedSectionEid());
			Set<org.sakaiproject.authz.api.Role> siteRoles = bus.owl().owlAuthz().getSiteRolesForGradebook(gradebookUid);
			
            String mUserEid, mRole;
            for (Membership m : members) {
                try {                      
                    mUserEid = m.getUserId();
                    mRole = m.getRole();
                    if ( mUserEid != null && !mUserEid.isEmpty() && members != null && !members.isEmpty()) {
            
                        if (bus.owl().owlAuthz().isUserAbleToSubmitCourseGrades(mUserEid, siteRoles, userRoleMap) && !rolesToSubmit.contains(mRole)) {
                            User u = userDirectoryService.getUserByEid(mUserEid);
                            submitters.add(u);                              
                        }
                    }
                } catch (UserNotDefinedException userNotDefinedException){
                    // don't care, just skip user
                }
            }                                               
        }
        
        return submitters;
    }
    
    public boolean isCurrentUserAbleToSubmit()
    {
        //refreshCurrentProvidedMembers();
        return isUserAbleToSubmit(userEid, currentSectionProvidedMembers);
    }
    
    public boolean isCurrentUserAbleToApprove()
    {
        //refreshCurrentProvidedMembers();
        return isUserAbleToApprove(userEid, currentSectionProvidedMembers);
    }
    
    private boolean isUserAbleToSubmit(String userEid, Set<Membership> members)
    {
        //submit permission & membership in section with non-submittable role
        boolean submit = false;
        
        // bjones86 - grade submission/approval exclude prefixes
        if( isSectionInExcludePrefixList() )
		{
        	return submit;
		}
        
        if (userEid != null && !userEid.isEmpty() && members != null && !members.isEmpty())
        {
            if (bus.owl().owlAuthz().isUserAbleToSubmitCourseGrades(userEid, getSelectedSectionEid(), gradebookUid))
            {
                for (Membership m : members)
                {
                    if (userEid.equals(m.getUserId()))
                    {
                        submit = !rolesToSubmit.contains(m.getRole());
                        break;
                    }
                }
            }
        }
        
        return submit;
    }
    
    private boolean isUserAbleToApprove(String userEid, Set<Membership> members)
    {
        // approve permission & membership in section with non-submittable role
        boolean approve = false;
        
        // bjones86 - grade submission/approval exclude prefixes
        if( isSectionInExcludePrefixList() )
		{
        	return approve;
		}
        
        if (userEid != null && !userEid.isEmpty() && members != null && !members.isEmpty())
        {
            if (bus.owl().owlAuthz().isUserAbleToApproveCourseGrades(userEid, getSelectedSectionEid(), gradebookUid))
            {
                for (Membership m : members)
                {
                    if (userEid.equals(m.getUserId()))
                    {
                        if (!rolesToSubmit.contains(m.getRole()))
                        {
                            approve = true;
                        }
                        break;
                    }
                }
            }
        }
        
        return approve;
    }
    
    /**
     * Determines if the currently selected section starts with any of the prefixs 
     * from sakai.properties that should be excluded from grade submission/approval.
     * 
     * @author bjones86
     */
    public boolean isSectionInExcludePrefixList()
    {
    	String selectedSectionEid = getSelectedSectionEid();
    	if( selectedSectionEid != null && "".compareToIgnoreCase( selectedSectionEid ) != 0 )
    		for( String prefix : excludePrefixes )
    			if( selectedSectionEid.startsWith( prefix ) )
    				return true;
    						
    	return false;
    }
    
    /**
     * 
     * @param previousSub the current submission's previous submission. Can be null.
     * @return the most recent approved previous submission, or empty if there was no previous approval
     */
	public static Optional<OwlGradeSubmission> findPreviousApproval(OwlGradeSubmission previousSub)
	{
		if (previousSub == null)
        {
            return Optional.empty();
        }
        else if (previousSub.hasApproval())
        {
            return Optional.of(previousSub);
        }
        else if (previousSub.hasPrevSubmission() == false)
        {
            return Optional.empty();
        }

        return findPreviousApproval(previousSub.getPrevSubmission());
	}
    
    private static List<String> readListFromProperty(String propName)
    {
        String[] propArray = ServerConfigurationService.getStrings(propName);
        List<String> propList;
        if (propArray != null)
        {
            propList = Arrays.asList(propArray);
        }
        else
        {
            LOG.warn(LOG_PREFIX + "Sakai property " + propName + " has not been set.");
            propList = new ArrayList<String>();
        }
        
        return propList;
    }
    
    private boolean isDemoSection(String sectionEid)
    {
        boolean demoSection = false;
        for (String prefix : demoSectionPrefixList)
        {
            if (sectionEid.startsWith(prefix))
            {
                demoSection = true;
                break;
            }
        }
        
        return demoSection;
    }
    
    /****************** Begin Course Grade Submission Nested Classes *********************/
    
    public class SubmissionHistoryRow implements Serializable
    {
		public static final String APPROVED_STATUS = "Approved";
		public static final String PENDING_STATUS = "Pending Approval";
		
		@Getter
        private final OwlGradeSubmission sub;
		@Getter
		private final String subDate, subByName, subByEid, status, appDate, appByName, appByEid;
		
		SimpleDateFormat formatter = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");
        
        public SubmissionHistoryRow(OwlGradeSubmission submission)
        {
            sub = submission;
			subDate = formatter.format(sub.getSubmissionDate());
			subByEid = sub.getUserEid();
			subByName = owlbus.getNonStudentUserByEid(subByEid).map(u -> u.gbUser.getDisplayName()).orElse(subByEid);

			if (sub.hasApproval())
			{
				status = APPROVED_STATUS;
				OwlGradeApproval app = sub.getApproval();
				appDate = formatter.format(app.getApprovalDate());
				appByEid = app.getUserEid();
				appByName = owlbus.getNonStudentUserByEid(appByEid).map(u -> u.gbUser.getDisplayName()).orElse(appByEid);
			}
			else
			{
				status = PENDING_STATUS;
				appDate = appByName = appByEid = "-";
			}
			
        }
        
        public boolean isApproved()
        {
            return sub.hasApproval();
        }
        
    } // end class SubmissionHistoryRow
    
    public class GradeChangeReport
    {
        private int newStudentCount;
        private int missingStudentCount;
        private int revisedStudentCount;
        
        public GradeChangeReport()
        {
            newStudentCount = 0;
            missingStudentCount = 0;
            revisedStudentCount = 0;
        }

        public int getMissingStudentCount()
        {
            return missingStudentCount;
        }

        public void setMissingStudentCount(int missingStudentCount)
        {
            this.missingStudentCount = missingStudentCount;
        }

        public int getNewStudentCount()
        {
            return newStudentCount;
        }

        public void setNewStudentCount(int newStudentCount)
        {
            this.newStudentCount = newStudentCount;
        }

        public int getRevisedStudentCount()
        {
            return revisedStudentCount;
        }

        public void setRevisedStudentCount(int revisedStudentCount)
        {
            this.revisedStudentCount = revisedStudentCount;
        }
        
        /**
         * Returns total number of changes (added, removed, or revised)
         * @return 
         */
        public int getTotalChanges()
        {
            return newStudentCount + missingStudentCount + revisedStudentCount;
        }
        
        /**
         * Returns total number of added students and revised students
         * @return 
         */
        public int getTotalAddsAndRevisions()
        {
            return newStudentCount + revisedStudentCount;
        }
        
        public boolean hasChanges()
        {
            return getTotalChanges() > 0;
        }
        
        public boolean hasAddsOrRevisions()
        {
            return getTotalAddsAndRevisions() > 0;
        }
        
        /**
         * Returns message that should be displayed if grades are ready for first submission
         * @return message that should be displayed if grades are ready for first submission
         */
        public String getFirstSubmissionMessage()
        {
            if (newStudentCount < 1)
            {
                return presenter.getLocalizedString(CourseGradeSubmitter.NO_GRADES_READY_MSG_KEY);
            }
            else if (newStudentCount == 1)
            {
                return presenter.getLocalizedString(CourseGradeSubmitter.SINGLE_GRADE_READY_MSG_KEY);
            }
            else
            {
                return presenter.getLocalizedString(CourseGradeSubmitter.GRADES_READY_MSG_KEY, new String[] {String.valueOf(newStudentCount)});
            }
        }
        
        /**
         * Returns message that should be displayed if grades have been submitted before
         * @return message that should be displayed if grades have been submitted before
         */
        public String getCurrentStatusMessage()
        {
            String counts[] = {gradeCountMessage(revisedStudentCount), gradeCountMessage(newStudentCount), gradeCountMessage(missingStudentCount)};
       
            return presenter.getLocalizedString(CourseGradeSubmitter.CURRENT_STATUS_MSG_KEY, counts);
        }
        
        /**
         * Creates an appropriately pluralized grade count message 
         * @param count the number of grades
         * @return a grade count message that is singular iff count == 1
         */
        private String gradeCountMessage(int count)
        {
            String prefix = count + " ";
            if (count == 1)
            {
                return prefix + presenter.getLocalizedString(CourseGradeSubmitter.GRADE_HAS_MSG_KEY);
            }
            
            return prefix + presenter.getLocalizedString(CourseGradeSubmitter.GRADES_HAVE_MSG_KEY);   
        }
        
    } // end class GradeChangeReport
    
    /****************** End Course Grade Submission Nested Classes *********************/
        
} // end class
