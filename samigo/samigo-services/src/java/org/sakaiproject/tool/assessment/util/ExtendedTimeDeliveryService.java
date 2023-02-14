/*
 * Copyright (c) 2016, The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.sakaiproject.tool.assessment.util;

import java.util.*;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.assessment.facade.*;
import org.sakaiproject.tool.assessment.data.dao.assessment.ExtendedTime;
import org.sakaiproject.tool.assessment.data.ifc.assessment.AssessmentAccessControlIfc;
import org.sakaiproject.tool.assessment.services.PersistenceService;
import org.sakaiproject.tool.assessment.services.assessment.PublishedAssessmentService;

/**
 * This class will instantiate with all the proper values for the current user's
 * extended time values for the given published assessment.
 * 
 * @author pdagnall1
 * @author Leonardo Canessa
 *
 */
@Slf4j
public class ExtendedTimeDeliveryService {
	private static final int MINS_IN_HOUR = 60;
	private static final int SECONDS_IN_MIN = 60;

	private String siteId;
	private SiteService siteService;

	private boolean hasExtendedTime;
	private Integer timeLimit;
	private Date startDate;
	private Date dueDate;
	private Date retractDate;

	@Getter
	private Long publishedAssessmentId;
	@Getter
	private String agentId;

	/**
	 * Creates an ExtendedTimeService object using the userId in the agentFacade as the current user
	 * @param publishedAssessment a published assessment object
	 */
	public ExtendedTimeDeliveryService(PublishedAssessmentFacade publishedAssessment) {
		this(publishedAssessment, AgentFacade.getAgentString());
	}

	/**
	 * Creates an ExtendedTimeDeliveryService object based on a specific agentId (userId)
	 * @param publishedAssessment a published assessment object
	 * @param agentId a specific userId to look up
	 *
	 */
	public ExtendedTimeDeliveryService(PublishedAssessmentFacade publishedAssessment, String agentId) {
		this(publishedAssessment, new ExtendedTimeDeliveryServiceInfo(agentId));
	}

	/**
	 * Creates an ExtendedTimeDeliveryService object based on a ExtendedTimeDeliveryServiceInfo, which encapsulates a userId and other info to eliminate redundant calls.
	 * @param publishedAssessment a published assessment object
	 * @param info information to prevent repeating queries in this c'tor
	 */
	public ExtendedTimeDeliveryService(PublishedAssessmentFacade publishedAssessment, ExtendedTimeDeliveryServiceInfo info) {
		if (publishedAssessment == null) {
			throw new IllegalArgumentException("Cannot create ExtendedTimeDeliveryService with null publishedAssessment");
		}

		String agentId = info.getUserId();

		PublishedAssessmentService publishedAssessmentService = new PublishedAssessmentService();
		if (!assessmentInitialized(publishedAssessment)) {
			publishedAssessment = publishedAssessmentService
					.getPublishedAssessmentQuick(publishedAssessment.getPublishedAssessmentId().toString());;
		}
		siteService = ComponentManager.get(SiteService.class);

		// Grab the site id from the publishedAssessment because the user may
		// not be in a site
		// if they're taking the test via url.
		publishedAssessmentId = publishedAssessment.getPublishedAssessmentId();
		String pubId = publishedAssessmentId.toString();
		siteId = publishedAssessmentService.getPublishedAssessmentSiteId(pubId);

		this.agentId = agentId;

		ExtendedTimeFacade extendedTimeFacade = PersistenceService.getInstance().getExtendedTimeFacade();

		// Get groups containing the student
		Collection<String> groupIDs;
		if (info.isGroupsKnown()) {
			groupIDs = info.getGroupIDs();
		} else {
			try {
				groupIDs = siteService.getSite(siteId).getGroupsWithMember(agentId).stream().map(Group::getId).collect(Collectors.toList());
				info.setGroupIDs(groupIDs);
			} catch (IdUnusedException e) {
				log.error("Failed to retrieve groups for siteId {} with member {}", siteId, agentId, e);
				groupIDs = Collections.emptyList();
			}
		}

		List<ExtendedTime> extendedTimes = Collections.emptyList();
		if (CollectionUtils.isEmpty(groupIDs)) {
			ExtendedTime userExtension = extendedTimeFacade.getEntryForPubAndUser(publishedAssessment, agentId);
			if (userExtension != null) {
				extendedTimes = new ArrayList<>(1);
				extendedTimes.add(userExtension);
			}
		} else {
			extendedTimes = extendedTimeFacade.getEntriesForPubAndUserOrGroups(publishedAssessment, agentId, groupIDs);
		}

		Optional<ExtendedTime> selectedExtension = pickExtendedTime(publishedAssessment, extendedTimes);
		if (selectedExtension.isPresent()) {
			ExtendedTime useMe = selectedExtension.get();
			this.timeLimit = useMe.getTimeHours() * MINS_IN_HOUR * SECONDS_IN_MIN + useMe.getTimeMinutes() * SECONDS_IN_MIN;
			this.startDate = useMe.getStartDate();
			this.dueDate = useMe.getDueDate();
			this.retractDate = useMe.getRetractDate();
			this.hasExtendedTime = true;
		} else {
			this.timeLimit = 0;
			this.startDate = publishedAssessment.getStartDate();
			this.dueDate = publishedAssessment.getDueDate();
			this.retractDate = publishedAssessment.getRetractDate();
						this.hasExtendedTime = false;
		}
	}

	/**
	 * @param extendedTime must not be null
	 * @return the time limit in seconds for the specified ExtendedTime instance; and Integer.MAX_VALUE if both the timeMinutes and timeHours are 0 or null
	 */
	public static Integer getTimeLimitInSeconds(ExtendedTime extendedTime) {
		Objects.requireNonNull(extendedTime, "extendedTime must not be null");

		Integer timeMinutes = extendedTime.getTimeMinutes();
		Integer timeHours = extendedTime.getTimeHours();

		int minutes = timeMinutes == null ? 0 : timeMinutes;
		int hours = timeHours == null ? 0 : timeHours;
		if (minutes == 0 && hours == 0) {
			return Integer.MAX_VALUE;
		}
		return minutes * SECONDS_IN_MIN + hours * MINS_IN_HOUR * SECONDS_IN_MIN;
	}

	/**
	 * Select an appropriate extension instance.
	 * Order of precedence:
	 * 1: An extension set specifically on the student.
	 * 2: An extension set on one of the student's groups
	 * If there is no student-specific extension, but they are in multiple groups with extensions, use this order of precedence to select one:
	 * a) Longest time limit.
	 * b) Longest availability window.
	 * If there are multiple that share the greatest availability window,
	 * select the instance that ends the latest so that the student has more time to prepare
	 */
	public static Optional<ExtendedTime> pickExtendedTime(PublishedAssessmentFacade publishedAssessment, List<ExtendedTime> extendedTimes) {
		if (CollectionUtils.isEmpty(extendedTimes)) {
			return Optional.empty();
		}
		if (extendedTimes.size() == 1) {
			return Optional.of(extendedTimes.get(0));
		}

		// Highest precedence: user-specific instances
		List<ExtendedTime> remainingCandidates = extendedTimes.stream().filter(et -> !StringUtils.isBlank(et.getUser())).collect(Collectors.toList());

		if (remainingCandidates.size() == 1) {
			return Optional.of(remainingCandidates.get(0));
		}


		// Next precedence: extensions that permit the longest time limit
		remainingCandidates = filterLongestTimeLimit(publishedAssessment, extendedTimes);

		if (remainingCandidates.size() == 1) {
			return Optional.of(remainingCandidates.get(0));
		}

		// Next precedence: those with the greatest availability window duration
		return findMostGracefulAvailabilityWindow(publishedAssessment, remainingCandidates);
	}

	/**
	 * @return a sublist of the remainingCandidates containing only those allowing the greatest time limit.
	 */
	private static List<ExtendedTime> filterLongestTimeLimit(PublishedAssessmentFacade publishedAssessment, List<ExtendedTime> remainingCandidates) {
		/*
		 * On assessments with time limits, extensions with a time limit of 0 grant unlimited time.
		 * On assessments without time limits, extensions can assign time limits to users / groups.
		 * So whether an assessment has a time limit or not, extensions must be filtered as follows:
		 *
		 * Cases:
		 * Extensions with time limits | Filter to include
		 * None                        | All
		 * Some                        | Those without time limits
		 * All                         | Extensions that share the greatest time limit
		 */

		// Get the maximum time limit (converts no time limit -> Integer.MAX_VALUE)
		final int greatestTimeLimit = remainingCandidates.stream().mapToInt(
				et -> getTimeLimitInSeconds(et)
		).max().getAsInt();

		// Return all the extended time candidates that match the greatest time limit
		return remainingCandidates.stream().filter(et -> {
			return greatestTimeLimit == getTimeLimitInSeconds(et);
		}).collect(Collectors.toList());
	}

	/**
	 * Returns the element in remainingCandidates with the greatest availability window.
	 * If multiple elements share the longest availability window duration,
	 * we return one such element whose dates are furthest in the future
	 * (E.g. so that students have more time to prepare).
	 */
	private static Optional<ExtendedTime> findMostGracefulAvailabilityWindow(PublishedAssessmentFacade publishedAssessment, List<ExtendedTime> remainingCandidates) {
		// If an extension's start / due / retract date is not present, it falls back to the assessment's value.
		Date pubStartDate = publishedAssessment.getStartDate();
		Date pubDueDate = publishedAssessment.getDueDate();
		Date pubRetractDate = publishedAssessment.getRetractDate();
		Integer lateHandling = publishedAssessment.getLateHandling();

		// Matching logic of DeliveryBean.isAvailable()
		// Without a start date, use the smallest representable time (1970 AD)
		long pubStartTime = pubStartDate == null ? 0 : pubStartDate.getTime();

		Date pubEndTimeSource = null;
		if (AssessmentAccessControlIfc.ACCEPT_LATE_SUBMISSION.equals(publishedAssessment.getLateHandling())) {
			// Matching logic of DeliveryBean.isRetracted()
			pubEndTimeSource = pubRetractDate;
		} else {
			// Matching logic of DeliveryBean.checkBeforeProceed() "check 8"
			// NOT_ACCEPT_LATE_SUBMISSION - taking the assessment at the due date will trigger the assessment to submit, or send them to an appropriate page. The retract date also applies.
			// So the Due Date is effectively the end of the availability window, and the retract date restricts access
			pubEndTimeSource = pubDueDate != null ? pubDueDate : pubRetractDate;
		}
		// Without an end date, use the largest representable time (~292 million years AD)
		long pubEndTime = pubEndTimeSource == null ? Long.MAX_VALUE : pubEndTimeSource.getTime();

		ExtendedTime extendedTimeWithGreatestDuration = null;
		long greatestDuration = 0;
		long latestEndTime = 0;
		long latestDueDate = 0;
		boolean extendedTimeUpdated = false;
		for (ExtendedTime et : remainingCandidates) {
			extendedTimeUpdated = false;
			// Same logic for pubStartTime / pubEndTime applies - matches DeliveryBean.isAvailable(), checkBeforeProceed()'s "check 8", etc.
			long startTime = et.getStartDate() == null ? pubStartTime : et.getStartDate().getTime();

			Date endTimeSource = null;
			if (AssessmentAccessControlIfc.ACCEPT_LATE_SUBMISSION.equals(publishedAssessment.getLateHandling())) {
				endTimeSource = et.getRetractDate();
			} else {
				// Not accept late submission - due date takes precedence.
				if (et.getDueDate() != null) {
					endTimeSource = et.getDueDate();
				} else {
					// Use a retract date only if a due date doesn't come into play.
					endTimeSource = pubDueDate == null ? et.getRetractDate() : pubDueDate;
				}
			}
			long endTime = endTimeSource == null ? pubEndTime : endTimeSource.getTime();

			long availabilityWindowDuration = endTime - startTime;
			if (availabilityWindowDuration > greatestDuration) {
				greatestDuration = availabilityWindowDuration;
				extendedTimeWithGreatestDuration = et;
				extendedTimeUpdated = true;
			} else if (availabilityWindowDuration == greatestDuration) {
				// The duration is the same.
				// Pick the latest window - the student will have more time to prepare
				if (endTime > latestEndTime) {
					latestEndTime = endTime;
					extendedTimeWithGreatestDuration = et;
					extendedTimeUpdated = true;
				} else if (endTime == latestEndTime) {
					// Windows are identical.
					// Consider if the windows end on retract dates, but the due dates differ.
					// Use the latest due date in this scenario.
					if (et.getDueDate() != null && et.getDueDate().getTime() > latestDueDate) {
						extendedTimeWithGreatestDuration = et;
						extendedTimeUpdated = true;
					}
				}
			}
			// If we've selected a new ExtendedTime instance, update 'latestDueDate':
			// We might come across another ExtendedTime instance with an equivalent window,
			// but a later due date within the window.
			if (extendedTimeUpdated) {
				if (et.getDueDate() != null) {
					latestDueDate = et.getDueDate().getTime();
				} else {
					latestDueDate = pubDueDate == null ? pubEndTime : pubDueDate.getTime();
				}
			}
		}

		if (extendedTimeWithGreatestDuration != null) {
			return Optional.of(extendedTimeWithGreatestDuration);
		}

		log.warn("No optimal extended time; shouldn't be possible");
		return Optional.of(remainingCandidates.get(0));
	}

	private List<String> getGroups(List<ExtendedTime> extendedTimeList) {
		List<String> list = new ArrayList<>();
		extendedTimeList.forEach(extendedTime -> {
			if(!"".equals(extendedTime.getGroup())) {
				list.add(extendedTime.getGroup());
			}
		});

		return list;
	}

	// Depending on the scope the assessment info sometimes is not initialized.
	private boolean assessmentInitialized(PublishedAssessmentFacade publishedAssessment) {
		if (publishedAssessment == null) {
			return false;
		}
		if (publishedAssessment.getStartDate() != null) {
			return true;
		}
		if (publishedAssessment.getDueDate() != null) {
			return true;
		}
		if (publishedAssessment.getRetractDate() != null) {
			return true;
		}

		return publishedAssessment.getTimeLimit() != null;
	}

	public Integer getTimeLimit() {
		return timeLimit;
	}

	public void setTimeLimit(Integer timeLimit) {
		this.timeLimit = timeLimit;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public Date getRetractDate() {
		return retractDate;
	}

	public void setRetractDate(Date retractDate) {
		this.retractDate = retractDate;
	}

	public boolean hasExtendedTime() {
		return hasExtendedTime;
	}

	public void setHasExtendedTime(boolean hasExtendedTime) {
		this.hasExtendedTime = hasExtendedTime;
	}
}
