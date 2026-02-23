package org.sakaiproject.component.gradebook.owl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.component.gradebook.GradebookServiceHibernateImpl;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeSubmission;
import org.sakaiproject.component.gradebook.owl.report.FGInfo;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.coursemanagement.api.Membership;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.service.gradebook.shared.CourseGrade;
import org.sakaiproject.service.gradebook.shared.InvalidGradeException;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.MissingCourseGradeException;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.MissingStudentNumberException;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeSubmissionGrades;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.report.FGChanges;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.CandidateDetailProvider;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

/**
 * Delegate class to handle retrieving final grade changes (revised/added/removed counts) for the unsubmitted
 * final grades report job.
 *
 * NOTE: this class heavily copies from code in GradebookNG in order to make that logic accessible through
 * GradebookService, introducing code duplication. See OWL-5728 for the rationale behind this decision.
 *
 * @author plukasew
 */
@Slf4j
class FinalGradeChangesReporter
{
	// statics from CourseGradeSubmitter
	private static final String SAKORA_ROLES_TO_SUBMIT_SAKAI_PROPERTY = "gradebook.courseGradeSubmission.sakoraRolesToSubmit";
    private static final List<String> rolesToSubmit = readListFromProperty(SAKORA_ROLES_TO_SUBMIT_SAKAI_PROPERTY);
	private static final String COMMA_DELIMITER = ",";
    public  static final String SAK_PROP_SUBMIT_USERNAME_PREFIX_MAP = "gradebook.courseGradeSubmission.submitUsername.prefixMap";
    private static final Map<String, Set<String>> submitUsernamePrefixMap = initSubmitUsernamePrefixMap( SAK_PROP_SUBMIT_USERNAME_PREFIX_MAP );

	private final GradebookServiceHibernateImpl gbServ;
	private final OwlGradebookServiceImpl owlgbServ;
	private final SiteService siteServ;
	private final UserDirectoryService userDirServ;
	private final CandidateDetailProvider cdp;
	private final CourseManagementService cms;

	FinalGradeChangesReporter(GradebookServiceHibernateImpl gbService, OwlGradebookServiceImpl owlgbService, SiteService siteService, UserDirectoryService userDirService)
	{
		gbServ = gbService;
		owlgbServ = owlgbService;
		siteServ = siteService;
		userDirServ = userDirService;
		cdp = (CandidateDetailProvider) ComponentManager.get("org.sakaiproject.user.api.CandidateDetailProvider");
		cms = (CourseManagementService) ComponentManager.get(CourseManagementService.class);
	}

	FGChanges getChanges(String siteId, String sectionEid)
	{
		// overall impl based on CourseGradeSubmitter.getGradeChangeReport()
		// 1. get the current grades
		// 2. get the existing final grade submission
		// 3. get the submitted grades from the existing submission
		// 4. compare the current and previous grades and return result

		return checkForChanges(getCurrentGrades(siteId, sectionEid), getPreviousGrades(siteId, sectionEid));
	}

	private Set<OwlGradeSubmissionGrades> getCurrentGrades(String siteId, String sectionEid)
	{
		// OWLTODO: impl...see CourseGradeSubmitter.getCurrentCourseGrades()
		// step 1

		Set<OwlGradeSubmissionGrades> finalGrades = new HashSet<>();

		// CourseGradeSubmitter.refreshCurrentProvidedMembers() is called and this point in the original code...
		// OWLTODO: while we don't need to "refresh", we do need to actually get the members so we essentially
		// need to still do this
		Set<Membership> providedMembers = cms.getSectionMemberships(sectionEid);

		// Next is: List<OwlGbStudentCourseGradeInfo> courseGrades = owlbus.fg.getSectionCourseGrades(section);
		// OWLTODO: this info class is part of tool and we likely don't need most of it, so we can probably
		// just create a stripped down replacement class to perform the same role here
		// NOTE: we are actually getting the student numbers in this step, just like GBNG does
		var courseGrades = getSectionCourseGrades(siteId, providedMembers, sectionEid);

		// Next is:
		// convert valid course grade records to Registrar format
		//Gradebook gb = bus.getGradebook(); // pay this cost up front, instead of once for each grade override
        //GradeMapping gradeMapping = gb.getSelectedGradeMapping();
		// OWLTODO: the above are GBNG classes...we need to find out what they are actually used for
		// gb is only used to get the gradeMapping
		// mapping is only used to convert course grade to registrar grade, which will be a step later on
		// for now we will skip this and move on to the main loop

		// Next is: main loop
		for (FGInfo record : courseGrades)
		{
			try
			{
				String studentEid = record.userEid;
				if (!isOfficialStudent(studentEid, providedMembers))
                {
                    continue; // skip unofficial students
                }

				OwlGradeSubmissionGrades grade = new OwlGradeSubmissionGrades();
				grade.setStudentEid(studentEid);

				if (sectionAndUsernameMatchesPrefixList(studentEid, sectionEid)) // OWL-1212 substitute username for student number in the database  --plukasew
                {
                    grade.setStudentNumber(studentEid);
                }
                else
                {
                    grade.setStudentNumber(getStudentNumber(studentEid, record.studentNumber));
                }
				// OWLTODO: figure out student numbers...

				//grade.setGrade(courseGradeToRegistrarGrade(record, gradeMapping));
				// OWLTODO: figure out mapping...

                finalGrades.add(grade);
			}
			catch (MissingStudentNumberException | MissingCourseGradeException | InvalidGradeException e)
			{
				// just skip this student regardless of reason
				// GBNG would log but we don't need to do that for this report
			}
		}

		return finalGrades;
	}

	// Compare with OwlFinalGradesService.getSectionCourseGrades()
	private List<FGInfo> getSectionCourseGrades(String siteId, Set<Membership> sectionMembers, String sectionEid)
	{
		try
		{
			// Get the list of gradeable users for the section (compare with GradebookNgBusinessService.getGradeableUsers())
			Site site = siteServ.getSite(siteId);
			List<String> userUuids = new ArrayList<>(site.getUsersIsAllowed("section.role.student"));

			// Retain only those users belonging to the section in question
			Map<String, String> uuidToEIDMap = new HashMap<>(sectionMembers.size());
			ListIterator<String> itr = userUuids.listIterator();
			while (itr.hasNext())
			{
				String uuid = itr.next();
				try
				{
					User user = userDirServ.getUser(uuid);
					Optional<Membership> opt = sectionMembers.stream().filter(m -> m.getUserId().equals(user.getEid())).findFirst();
					if (opt.isEmpty())
					{
						itr.remove();
					}
					else
					{
						uuidToEIDMap.put(uuid, opt.get().getUserId());
					}
				}
				catch (UserNotDefinedException ex)
				{
					log.error("Unable to get user by UUID: {}", uuid, ex);
				}
			}

			// Get the course grades for the gradeable users
			Map<String, CourseGrade> grades = gbServ.getCourseGradeForStudents(siteId, userUuids);
			List<FGInfo> gradeList = new ArrayList<>(grades.size());
			for (Entry<String, CourseGrade> entry : grades.entrySet())
			{
				//Optional<OwlGbUser> student = bus.owl().getUserRevealingNumber(entry.getKey(), site);
				// OWLTODO: instead of the above we just need eid and student number
				String eid = uuidToEIDMap.get(entry.getKey());
				String number = getRevealedStudentNumber(eid, siteId, sectionEid);
				FGInfo fg = new FGInfo(eid, number, entry.getValue());

				gradeList.add(fg);
			}

			return gradeList;
		}
		catch (IdUnusedException ex)
		{
			log.error("Unable to get site by id: {}", siteId, ex);
			return Collections.emptyList();
		}
	}

	// from CourseGradeSubmitter.isOfficialStudent()
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

	// adapted from CourseGradeSubmitter
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
            throw new RuntimeException("Required property " + propName + " has not been set.");
        }

        return propList;
    }

	// adapted from CourseGradeSubmitter
	private boolean sectionAndUsernameMatchesPrefixList( String userEID, String sectionEID )
    {
        // Short circuit
        if( sectionEID == null || sectionEID.isEmpty() || StringUtils.isBlank(userEID) )
            return false;

        // Find the matching section prefix
        String sectionPrefix = checkForUsernameSubmissionPrefix(sectionEID);

        // If a section prefix match was found, return true/false if username starts with any prefix from the section prefix specific username prefix list
        if( !sectionPrefix.isEmpty() )
            return StringUtils.startsWithAny( userEID,
                submitUsernamePrefixMap.get( sectionPrefix ).toArray( new String[submitUsernamePrefixMap.get( sectionPrefix ).size()] ) );

        // No section prefix match, return false
        return false;
    }

	// from CourseGradeSubmitter
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

	// from CourseGradeSubmitter
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

	// adapted from CourseGradeSubmitter and OwlFinalGradesService.getSectionCourseGrades()/OwlBusinessService.getRevealedStudentNumber()
	private String getStudentNumber(String studentEid, String number) throws MissingStudentNumberException
    {
		//String number = student.gbUser.getStudentNumber();
		// OWLTODO: we can't use the above so have to recreate student number acquisition...
		// CourseGradeSubmitter ultimately ends up with a call to owlbus.getRevealedStudentNumber(), so we use that logic here...
		//String number = getRevealedStudentNumber(studentEid, siteId);
		// OWLTODO: we are getting student number passed in now, like CourseGradeSubmitter does...clean up these comments if that works out

		if (number.isEmpty())
		{
			throw new MissingStudentNumberException("Couldn't find student number for user: " + studentEid);
		}

        return number;
    }

	// adapted from OwlBusinessService
	private String getRevealedStudentNumber(String userEid, String siteId, String sectionEid)
	{
		try
		{
			var user = userDirServ.getUserByEid(userEid);
			var site = siteServ.getSite(siteId);
			return cdp.getInstitutionalNumericId(user, site)
					.orElseGet(() ->
					{
						String num = cdp.getInstitutionalNumericIdIgnoringCandidatePermissions(user, site).orElse("");
						if (!num.isEmpty() && isStudentInRoster(user, sectionEid)) // check for presence of number before hitting CM tables
						{
							return num; // always reveal student number if they are in the roster as a student
						}

						return "";
					});
		}
		catch (UserNotDefinedException e)
		{
			// OwlBusinessService.getUserRevealingNumber() handles this by returning empty optional, so we will return empty string here
			return "";
		}
		catch (IdUnusedException iue)
		{
			// GradebookNgBusinessService returns null in this case and OwlFinalGradesService just returns empty list if that were to happen
			// however, in both cases the siteId is coming from Sakai itself and so this will not happen in practce. We can return empty string here.
			return "";
		}
	}

	// adapted from OwlBusinessService.isStudentInARoster(user, sections)
	private boolean isStudentInRoster(User user, String sectionEid)
	{
		// OWLTODO: the original code would return true if the student was in any roster in the site...
		// However, since we will only be dealing with students that have already been show to be members
		// of the specific roster we are concerned with, it should be safe to only check that roster...
		// in fact, if performance is a concern we can probably skip this check entirely...
		// although, there is a role checking component of this code that we may have to account for elsewhere
		// if we totally skip the check
		Set<Membership> members = cms.getSectionMemberships(sectionEid);
		return members.stream().anyMatch(m -> m.getUserId().equals(user.getEid()) && "S".equals(m.getRole()));
	}

	// Compare with CourseGradeSubmitter.getGradeChangeReport()
	private Set<OwlGradeSubmissionGrades> getPreviousGrades(String siteId, String sectionEid)
	{
		// step 2 - get the last submission
		OwlGradeSubmission lastSub = owlgbServ.getMostRecentCourseGradeSubmissionForSectionInSite(sectionEid, siteId);

		// step 3 - get the grades for the last submission
		Set<OwlGradeSubmissionGrades> prevGrades = new HashSet<>();
		if (lastSub != null)
		{
			int submissionStatus = lastSub.getStatusCode();
			if (submissionStatus == OwlGradeSubmission.PENDING_APPROVAL_STATUS || submissionStatus == OwlGradeSubmission.APPROVED_STATUS)
			{
				prevGrades.addAll(lastSub.getGradeData());
			}
		}

		return prevGrades;
	}

	// Compare with CourseGradeSubmitter.checkForGradeChanges()
	private FGChanges checkForChanges(Set<OwlGradeSubmissionGrades> currentGrades, Set<OwlGradeSubmissionGrades> previousGrades)
	{
		// step 4 - compare current grades to previous grades for changes
		Map<String, OwlGradeSubmissionGrades> currentGradeMap = currentGrades.stream().collect(Collectors.toMap(OwlGradeSubmissionGrades::getStudentNumber, Function.identity()));
		Map<String, OwlGradeSubmissionGrades> prevGradeMap = previousGrades.stream().collect(Collectors.toMap(OwlGradeSubmissionGrades::getStudentNumber, Function.identity()));

		Set<String> newStudents = new HashSet<>(currentGradeMap.keySet());
		Set<String> sameStudents = new HashSet<>(currentGradeMap.keySet());
		Set<String> changedStudents = new HashSet<>();
		Set<String> missingStudents = new HashSet<>(prevGradeMap.keySet());

		sameStudents.retainAll(prevGradeMap.keySet());
		newStudents.removeAll(sameStudents);
		missingStudents.removeAll(sameStudents);

		for (String s : sameStudents)
		{
			if (!currentGradeMap.get(s).getGrade().equals(prevGradeMap.get(s).getGrade()))
			{
				changedStudents.add(s);
			}
		}

		return new FGChanges(changedStudents.size(), newStudents.size(), missingStudents.size());
	}
}
