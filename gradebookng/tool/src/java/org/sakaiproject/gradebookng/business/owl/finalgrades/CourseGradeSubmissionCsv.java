// 2012.07.16, plukasew, New
// Represents a CSV file of course grade submission data
// 2012.09.17, plukasew, Modified
// Removed section eid from file names so they remain < 80 chars (PeopleSoft reasons)
// 2014.06.13, plukasew, Modified
// OWL-1212 add support for CStudies grade submission

package org.sakaiproject.gradebookng.business.owl.finalgrades;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.coursemanagement.api.AcademicSession;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.coursemanagement.api.Section;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeSubmissionGrades;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;

/**
 * Represents a CSV file of course grade submission data
 * @author plukasew
 */
public class CourseGradeSubmissionCsv
{
    private static final Log LOG = LogFactory.getLog(CourseGradeSubmissionCsv.class);
    
    private static final String PREFIX_INITIAL = "\"09\",\"A\",";
    private static final String PREFIX_REVISION = "\"09\",\"C\",";
    private static final String APPROVAL_TYPE_INITIAL = "Initial";
    private static final String APPROVAL_TYPE_REVISION = "Revision";
    
    private static final String SFTP_HOST_SAKAI_PROPERTY = "gradebook.courseGradeSubmission.sftp.host";
    private static final String SFTP_USER_SAKAI_PROPERTY = "gradebook.courseGradeSubmission.sftp.user";
    private static final String SFTP_PASSWORD_SAKAI_PROPERTY = "gradebook.courseGradeSubmission.sftp.password";
	private static final String SFTP_PORT_SAKAI_PROPERTY = "gradebook.courseGradeSubmission.sftp.port";
    
    private List<String> gradeList;
    private String prefix;
    private String filename;
    private String sectionEid;
    private Date approvalDate;
    private String term;
    private String session;
    private String career;
    private String subject;
    private String catalog;
    private String section;
    private boolean submitUserEid; // OWL-1212  --plukasew
    
    public CourseGradeSubmissionCsv()
    {
        gradeList = new ArrayList();
        prefix = PREFIX_INITIAL;
        filename = "";
        sectionEid = "";
        approvalDate = new Date();
        term = "";
        session = "";
        career = "";
        subject = "";
        catalog = "";
        section = "";
        submitUserEid = false;
    }
    
    public void setSectionEid(String value) throws IllegalArgumentException
    {
        sectionEid = value;
        findSectionIdentifiers();
    }
    
    public void setApprovalDate(Date value)
    {
        approvalDate = value;
    }
    
    public boolean addGrades(Set<OwlGradeSubmissionGrades> grades)
    {
        prefix = PREFIX_INITIAL;
        gradeList = new ArrayList();
        for (OwlGradeSubmissionGrades grade : grades)
        {
            String line = prefix + qc(term) + qc(session) + qc(career) + qc(subject) + qc(catalog) + qc(section)
                            + qc(grade.getStudentNumber()) + "\"" + grade.getGrade() +"\"";
            if (submitUserEid)
            {
                line += cq(grade.getStudentEid());  // OWL-1212  --plukasew
            }
            gradeList.add(line);
        }
        
        return !gradeList.isEmpty();
    }
    
    public boolean addRevisedGrades(Set<OwlGradeSubmissionGrades> currentGrades, Set<OwlGradeSubmissionGrades> lastApprovedGrades)
    {
        // OWLTODO: This method could probably be more efficient

        Set<OwlGradeSubmissionGrades> currentCopy = new HashSet<>(currentGrades);
		// OWL-4082 - only compare student number and grade when determining if a grade has changed (allow names to change)
        //currentCopy.removeAll(lastApprovedGrades);
		Map<String, String> lastApprovedMap = lastApprovedGrades.stream().collect(Collectors.toMap(g -> g.getStudentNumber(), g -> g.getGrade()));
		Set<OwlGradeSubmissionGrades> unchangedGrades = currentCopy.stream()
				.filter(g -> lastApprovedMap.containsKey(g.getStudentNumber()) && g.getGrade().equals(lastApprovedMap.get(g.getStudentNumber())))
				.collect(Collectors.toSet());
		currentCopy.removeAll(unchangedGrades);
        
        prefix = PREFIX_REVISION;
        gradeList = new ArrayList();
        for (OwlGradeSubmissionGrades grade : currentCopy)
        {
            String lastGrade = getGradeForStudentByNumber(grade.getStudentNumber(), lastApprovedGrades);
            if ("".equals(lastGrade))
            {
                lastGrade = "NGR";
            }
            
            String line = prefix + qc(term) + qc(session) + qc(career) + qc(subject) + qc(catalog) + qc(section)
                            + qc(grade.getStudentNumber()) + qc(lastGrade) + "\"" + grade.getGrade() +"\"";
            if (submitUserEid)
            {
                line += cq(grade.getStudentEid());  // OWL-1212  --plukasew
            }
            
            gradeList.add(line);
        }
        
        return !gradeList.isEmpty();

    }
   
    private String getGradeForStudentByNumber(String studentNumber, Set<OwlGradeSubmissionGrades> grades)
    {
        String grade = "";
        for (OwlGradeSubmissionGrades g : grades)
        {
            if (g.getStudentNumber().equals(studentNumber))
            {
                grade = g.getGrade();
                break;
            }
        }
        
        return grade;
    }
    
    /**
     * Surrounds with quotes (q) and adds trailing comma (c) to input string
     * @param input original string
     * @return original string surrounded with quotes and followed by a comma
     */
    private String qc(String input)
    {
        return "\"" + input + "\",";
    }
    
    // the reverse of above
    private String cq(String input)
    {
        return ",\"" + input + "\"";
    }
    
    public boolean sftpToRegistrar()
    {
        boolean uploaded = false;
        StringBuilder allGrades = new StringBuilder();
        for (String grade : gradeList)
        {
            allGrades.append(grade);
            allGrades.append("\n");
        }
        
        byte[] csvBytes = allGrades.toString().getBytes();
        
        generateFilename();
		
		try (SshClient sshClient = SshClient.setUpDefaultClient())
		{
			sshClient.start();
			
			// get sftp info from sakai.properties
            String host = ServerConfigurationService.getString(SFTP_HOST_SAKAI_PROPERTY);
			int port = ServerConfigurationService.getInt(SFTP_PORT_SAKAI_PROPERTY, 22);
            String user = ServerConfigurationService.getString(SFTP_USER_SAKAI_PROPERTY);
            String password = ServerConfigurationService.getString(SFTP_PASSWORD_SAKAI_PROPERTY);
			
			ConnectFuture future = sshClient.connect(user, host, port);
			future.await();
			
			try (ClientSession clientSession = future.getSession())
			{
				clientSession.addPasswordIdentity(password);
				clientSession.setServerKeyVerifier(new FingerprintServerKeyVerifier());
				clientSession.auth().verify(30000);  // 30 second timeout
				SftpClientFactory factory = SftpClientFactory.instance();
				SftpClient sftp = factory.createSftpClient(clientSession);
				try (OutputStream os = sftp.write(filename))
				{
					os.write(csvBytes);
					os.flush();
				}
				finally
				{
					sftp.close();
					clientSession.close(false);
				}
			}
			finally
			{
				sshClient.stop();
			}
			
			uploaded = true;
		}
		catch (Exception e)
		{
			LOG.error("OWL: Final Grade Submission: SFTP transfer failed: " + e.getMessage(), e);
		}
                
        return uploaded;
    }
    
    private void generateFilename()
    {
        SimpleDateFormat formatter = new SimpleDateFormat("MM_dd_yyyy-HH_mm_ss");
        String approvalType = APPROVAL_TYPE_INITIAL;
        if (PREFIX_REVISION.equals(prefix))
        {
            approvalType = APPROVAL_TYPE_REVISION;
        }
        filename = subject + "_" + catalog + "_" + section + "_" + term + "_" + session + "_" + career + "-" 
                    + formatter.format(approvalDate) + "-" + approvalType + ".txt";
        filename = filename.replaceAll("/", "-");
        filename = filename.replaceAll("\\\\", "-"); // to replace a single backslash, we must use \\\\
        filename = filename.replaceAll("\\s+", "_");
    }
    
    private void findSectionIdentifiers() throws IllegalArgumentException
    {
        boolean failure;
        CourseManagementService cms = (CourseManagementService) ComponentManager.get(CourseManagementService.class);
        
        if (cms == null || sectionEid == null || sectionEid.trim().isEmpty() || sectionEid.trim().length() < 8)
        {
            failure = true;
        }
        else
        {
            // find section and academic session (term)
            Section cmsSection = cms.getSection(sectionEid);
            AcademicSession cmsSession = cms.getCourseOffering(cmsSection.getCourseOfferingEid()).getAcademicSession();
            String sessionEid = cmsSession.getEid().trim();
            String sectionEID = sectionEid.trim();
            String sectionTitle = cmsSection.getTitle().trim();
            String sectionPrefix = CourseGradeSubmitter.checkForUsernameSubmissionPrefix(sectionEID);
            
            if (sectionPrefix.isEmpty())
            {
                failure = !parseStandardSectionEid(sessionEid, sectionEid, sectionTitle);
            }
            else
            {
                failure = !parseUsernameSubmissionSectionEid(sectionPrefix, sectionEID);
                if (!failure)
                {
                    submitUserEid = true;  // OWL-1212  --plukasew
                }
            }
        }
        
        if (failure)
        {
            throw new IllegalArgumentException("OWL: Final Grade Submission: Unable to find required identifiers for section with eid: " + sectionEid);
        }
    }
    
    private boolean parseStandardSectionEid(String sessionEid, String sectionEid, String sectionTitle)
    {
        try
        {
            // Get the term from the session EID:
            // ex: UWOUGRD1129 -> 1129
            term = StringUtils.right( sessionEid, 4 );

            // Get the career from the session EID:
            // ex: UWOUGRD1129 -> UWOUGRD -> UGRD
            career = StringUtils.right( StringUtils.chomp( sessionEid, term ), 4 ).trim();

            // Get the subject from the section title:
            // ex: GEOGRAPH 2450F 001 FW12 -> GEOGRAPH
            String[] titleParts = sectionTitle.split( "\\s+" );
            subject = titleParts[0].trim();

            // get section number and session code from section eid now that we know the term (ex: 9010505111291001)
            section = StringUtils.right(sectionEid, 3); // ex: 9010505111291001 - > 001
            session = StringUtils.substringAfterLast(StringUtils.chomp(sectionEid, section), term); // ex: 9010505111291001 - > 9010505111291 -> 1

            // get catalog from the section title parts, ex: GEOGRAPH 2450F 001 FW12
            catalog = titleParts[1]; // ex: 2450F

        }
        catch (Exception e)
        {
            LOG.info(e);
            return false;
        }
        
        return true;
    }
    
    // OWL-1212
    private boolean parseUsernameSubmissionSectionEid(String prefix, String sectionEid)
    {
        // username submission format:
        // PREFIX-SUBJECT-CATALOG-SECTION-CAREER-TERM-SESSIONCODE
        // ex: WCCS---PREL-6034-001-CONT-1145-B

        String eid = StringUtils.substringAfter(sectionEid, prefix);
        String[] tokens = eid.split("-");
        
        if (tokens.length < 6)
        {
            return false;
        }
        
        subject = tokens[0];
        catalog = tokens[1];
        section = tokens[2];
        career = tokens[3];
        term = tokens[4];
        session = tokens[5];

        return true;
    }
}
