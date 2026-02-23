package org.sakaiproject.component.gradebook.owl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.sakaiproject.component.cover.ComponentManager;
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
import org.sakaiproject.user.api.UserDirectoryService;

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
	private final GradebookServiceHibernateImpl gbServ;
	private final OwlGradebookServiceImpl owlgbServ;
	private final SiteService siteServ;
	private final UserDirectoryService userDirServ;

	FinalGradeChangesReporter(GradebookServiceHibernateImpl gbService, OwlGradebookServiceImpl owlgbService, SiteService siteService, UserDirectoryService userDirService)
	{
		gbServ = gbService;
		owlgbServ = owlgbService;
		siteServ = siteService;
		userDirServ = userDirService;
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
		var cms = (CourseManagementService) ComponentManager.get(CourseManagementService.class);
		Set<Membership> providedMembers = cms.getSectionMemberships(sectionEid);

		// Next is: List<OwlGbStudentCourseGradeInfo> courseGrades = owlbus.fg.getSectionCourseGrades(section);
		// OWLTODO: this info class is part of tool and we likely don't need most of it, so we can probably
		// just create a stripped down replacement class to perform the same role here
		var courseGrades = getSectionCourseGrades(siteId, providedMembers);

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
				/*if (!isOfficialStudent(studentEid, currentSectionProvidedMembers))
                {
                    continue; // skip unofficial students
                }*/
				// OWLTODO: figure out skipping...

				OwlGradeSubmissionGrades grade = new OwlGradeSubmissionGrades();
				grade.setStudentEid(studentEid);

				/*if (sectionAndUsernameMatchesPrefixList(student, selectedSectionEid)) // OWL-1212 substitute username for student number in the database  --plukasew
                {
                    grade.setStudentNumber(studentEid);
                }
                else
                {
                    grade.setStudentNumber(getStudentNumber(student));
                }*/
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
	private List<FGInfo> getSectionCourseGrades(String siteId, Set<Membership> sectionMembers)
	{
		try
		{
			// Get the list of gradeable users for the section (compare with GradebookNgBusinessService.getGradeableUsers())
			Site site = siteServ.getSite(siteId);
			List<String> userUuids = new ArrayList<>(site.getUsersIsAllowed("section.role.student"));
			// OWLTODO: retain only those users belonging to the section in question

			// Get the course grades for the gradeable users
			Map<String, CourseGrade> grades = gbServ.getCourseGradeForStudents(siteId, userUuids);
			List<FGInfo> gradeList = new ArrayList<>(grades.size());
			for (Entry<String, CourseGrade> entry : grades.entrySet())
			{
				//Optional<OwlGbUser>
				FGInfo fg = new FGInfo("fake", "fake", entry.getValue());
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
