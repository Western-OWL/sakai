package org.sakaiproject.component.gradebook.owl;

import java.text.DecimalFormat;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
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
import org.sakaiproject.tool.gradebook.GradeMapping;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.sakaiproject.tool.gradebook.facades.Authz;
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
	private static final String SAK_PROP_SUBMIT_USERNAME_PREFIX_MAP = "gradebook.courseGradeSubmission.submitUsername.prefixMap";
	private static final Map<String, Set<String>> submitUsernamePrefixMap = initSubmitUsernamePrefixMap( SAK_PROP_SUBMIT_USERNAME_PREFIX_MAP );
	private static final String REGISTRAR_GRADE_CODES = "gradebook.courseGradeSubmission.registrarGradeCodes";
	private static final List<String> registrarGradeCodes = readListFromProperty(REGISTRAR_GRADE_CODES);
	private static final String SAKAI_PASS_GRADE_CODE = "P";
	private static final String SAKAI_FAIL_GRADE_CODE = "NP";
	private static final String REGISTRAR_PASS_GRADE_CODE = "PAS";
	private static final String REGISTRAR_FAIL_GRADE_CODE = "FAI";

	private final GradebookServiceHibernateImpl gbServ;
	private final OwlGradebookServiceImpl owlgbServ;
	private final SiteService siteServ;
	private final UserDirectoryService userDirServ;
	private final CandidateDetailProvider cdp;
	private final CourseManagementService cms;
	private final SecurityService secServ;

	FinalGradeChangesReporter(GradebookServiceHibernateImpl gbService, OwlGradebookServiceImpl owlgbService, SiteService siteService, UserDirectoryService userDirService)
	{
		gbServ = gbService;
		owlgbServ = owlgbService;
		siteServ = siteService;
		userDirServ = userDirService;
		cdp = (CandidateDetailProvider) ComponentManager.get("org.sakaiproject.user.api.CandidateDetailProvider");
		cms = (CourseManagementService) ComponentManager.get(CourseManagementService.class);
		secServ = (SecurityService) ComponentManager.get("org.sakaiproject.authz.api.SecurityService");
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

	// adapted from CourseGradeSubmitter.getCurrentCourseGrades()
	private Set<OwlGradeSubmissionGrades> getCurrentGrades(String siteId, String sectionEid)
	{
		// step 1

		Set<OwlGradeSubmissionGrades> finalGrades = new HashSet<>();

		Set<Membership> providedMembers = cms.getSectionMemberships(sectionEid);

		// NOTE: we are actually getting the student numbers in this method call, just like GBNG does
		var courseGrades = getSectionCourseGrades(siteId, providedMembers, sectionEid);

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

				grade.setGrade(courseGradeToRegistrarGrade(record, gbServ.getGradebook(siteId)));
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

	// adapted from CourseGradeSubmitter.courseGradeToRegistrarGrade()
	private String courseGradeToRegistrarGrade(FGInfo record, Gradebook gb) throws InvalidGradeException, MissingCourseGradeException
	{
		GradeMapping map = gb.getSelectedGradeMapping();
		CourseGrade courseGrade = record.cg;
		Optional<String> override = getOverride(courseGrade);
		String finalGrade;
		if (override.isPresent()) // we have an overridden grade, as a string
		{
			finalGrade = overriddenGradeAsGradeString(override.get(), map);
		}
		else // we have a numeric grade, or no grade was recorded
		{
			// getCalculatedGrade was originally a non-null toString'd Double, so this should be safe
			if (!getCalculatedGrade(courseGrade).isPresent())
			{
				throw new MissingCourseGradeException("No course grade entered for student: " + record.userEid);
			}

			finalGrade = formatForRegistrar(courseGrade);
		}

		if (finalGrade.isEmpty() || finalGrade.length() != 3)
		{
			throw new InvalidGradeException("Could not find valid course grade for student: " + record.userEid);
		}

		return finalGrade;
	}

	// adapted from OwlGbCourseGrade.getOverride()
	private static Optional<String> getOverride(CourseGrade cg)
	{
		return cg == null ? Optional.empty() : Optional.ofNullable(cg.getEnteredGrade());
	}

	// adapted from OwlGbCourseGrade.getOverride()
	private static Optional<Double> getCalculatedGrade(CourseGrade cg)
	{
		if (cg == null)
		{
			return Optional.empty();
		}

		double grade = NumberUtils.toDouble(cg.getCalculatedGrade(), Double.MIN_VALUE);
		return grade == Double.MIN_VALUE ? Optional.empty() : Optional.of(grade);
	}

	// adapted from FinalGradeFormatter.formatForRegistrar()
	private static String formatForRegistrar(CourseGrade cg)
	{
		return getOverride(cg).map(o -> overrideToRegistrarFinal(o))
				.orElseGet(() -> getCalculatedGrade(cg)
						.map(c -> padNumeric(Math.round(c)))
						.orElse(""));
	}

	// from CourseGradeSubmitter.overriddenGradeAsGradeString()
	private String overriddenGradeAsGradeString(String grade, GradeMapping gradeMapping) throws IllegalArgumentException, InvalidGradeException
	{
		if (grade == null)
		{
			throw new IllegalArgumentException("Grade cannot be null");
		}
		if (gradeMapping == null || gradeMapping.getGradeMap().isEmpty())
		{
			throw new IllegalStateException("No letter grade mapping defined for this gradebook");
		}

		String finalGrade;
		String g = grade.trim();
		if (isNumber(g) || registrarGradeCodes.contains(g)) // we have a valid numeric grade
		{
			finalGrade = overrideToRegistrarFinal(g);
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
			finalGrade = padNumeric(Math.round(doubleGrade));
		}
		else
		{
			throw new InvalidGradeException("Could not parse valid grade from trimmed argument: " + g);
		}

		return finalGrade;
	}

	// from FinalGradeFormatter.padNumeric()
	private static String padNumeric(long grade)
	{
		DecimalFormat formatNoDecimals = new DecimalFormat("000");
		return formatNoDecimals.format(grade);
	}

	// from FinalGradeFormatter.overrideToRegistrarFinal()
	private static String overrideToRegistrarFinal(String override)
	{
		String g = override.trim();
		try
		{
			return padNumeric(Long.parseLong(g));
		}
		catch (NumberFormatException nfe)
		{
			while (g.length() < 3)
			{
				g += " "; // pad with trailing spaces to fill 3 characters
			}

			return g;
		}
	}

	// from CourseGradeSubmitter.isNumber()
	private boolean isNumber(String value)
	{
		boolean result = true;
		try { Double.parseDouble(value); }
		catch (NumberFormatException nfe) { result = false; }
		return result;
	}

	// adapted from OwlFinalGradesService.getSectionCourseGrades()
	private List<FGInfo> getSectionCourseGrades(String siteId, Set<Membership> sectionMembers, String sectionEid)
	{
		try
		{
			// Get the list of gradeable users for the section (adapted from GradebookNgBusinessService.getGradeableUsers())
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
					log.error("Unable to get user by UUID: {}", uuid);
				}
			}

			// Get the course grades for the gradeable users
			Map<String, CourseGrade> grades = getCourseGrades(siteId, userUuids);
			List<FGInfo> gradeList = new ArrayList<>(grades.size());
			// NOTE: in GBNG there is a check here to see if the current user has permission to see student numbers,
			// which is omitted for the job. This can result in discrepancies between the job results and the UI.
			for (Entry<String, CourseGrade> entry : grades.entrySet())
			{
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

	private Map<String, CourseGrade> getCourseGrades(String siteId, List<String> userUuids)
	{
		// if the course grade for this site is not released, a permission check occurs to see if the current
		// user has gb perms...when run under the job this seems to not be the case and thus this method returns no grades...
		// so, we have a private method that uses a securityadvisor to make sure we have the necessary perms

		SecurityAdvisor yesMan = (String userID, String function, String reference) ->
		{
			if (Authz.PERMISSION_GRADE_ALL.equals(function) || Authz.PERMISSION_GRADE_SECTION.equals(function) || Authz.PERMISSION_EDIT_ASSIGNMENTS.equals(function))
			{
				return SecurityAdvisor.SecurityAdvice.ALLOWED;
			}

			return SecurityAdvisor.SecurityAdvice.PASS;
		};

		try
		{
			secServ.pushAdvisor(yesMan);
			return gbServ.getCourseGradeForStudents(siteId, userUuids);
		}
		finally
		{
			secServ.popAdvisor(yesMan);
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

	// adapted from CourseGradeSubmitter
	private String getStudentNumber(String studentEid, String number) throws MissingStudentNumberException
	{
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
		// the original code would return true if the student was in any roster in the site.
		// However, since we will only be dealing with students that have already been shown to be members
		// of the specific roster we are concerned with, it should be safe to only check that roster.
		Set<Membership> members = cms.getSectionMemberships(sectionEid);
		return members.stream().anyMatch(m -> m.getUserId().equals(user.getEid()) && "S".equals(m.getRole()));
	}

	// adapted from CourseGradeSubmitter.getGradeChangeReport()/getPreviousCourseGrades()
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

	// adapted from CourseGradeSubmitter.checkForGradeChanges()
	private FGChanges checkForChanges(Set<OwlGradeSubmissionGrades> currentGrades, Set<OwlGradeSubmissionGrades> previousGrades)
	{
		// step 4 - compare current grades to previous grades for changes
		Map<String, OwlGradeSubmissionGrades> currentGradeMap = new HashMap<>(currentGrades.size());
		Map<String, OwlGradeSubmissionGrades> prevGradeMap = new HashMap<>(previousGrades.size());
		for (OwlGradeSubmissionGrades grade : currentGrades)
		{
			currentGradeMap.put(grade.getStudentNumber(), grade);
		}
		for (OwlGradeSubmissionGrades grade : previousGrades)
		{
			prevGradeMap.put(grade.getStudentNumber(), grade);
		}

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
