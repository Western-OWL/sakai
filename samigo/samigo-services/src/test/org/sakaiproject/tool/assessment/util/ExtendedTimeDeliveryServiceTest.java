package org.sakaiproject.tool.assessment.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.sakaiproject.tool.assessment.data.dao.assessment.ExtendedTime;
import org.sakaiproject.tool.assessment.data.ifc.assessment.AssessmentAccessControlIfc;
import org.sakaiproject.tool.assessment.facade.PublishedAssessmentFacade;

public class ExtendedTimeDeliveryServiceTest {

	static final PublishedAssessmentFacade ASSESSMENT_TIME_LIMIT_ONLY = new PublishedAssessmentFacade();
	static final PublishedAssessmentFacade ASSESSMENT_TIME_LIMIT_AND_DATES = new PublishedAssessmentFacade();
	static final PublishedAssessmentFacade ASSESSMENT_START_DATE_ONLY = new PublishedAssessmentFacade();
	static final PublishedAssessmentFacade ASSESSMENT_DUE_DATE_ONLY = new PublishedAssessmentFacade();
	static final PublishedAssessmentFacade ASSESSMENT_RETRACT_DATE_ONLY = new PublishedAssessmentFacade();
	static final PublishedAssessmentFacade ASSESSMENT_DATES = new PublishedAssessmentFacade();
	// Same as ASSESSMENT_DATES, but late handling = NOT_ACCEPT_LATE_SUBMISSION
	static final PublishedAssessmentFacade ASSESSMENT_DATES_NALS = new PublishedAssessmentFacade();

	static final Integer DATA_TIME_LIMIT = 180;
	static final Date DATA_START_DATE = new GregorianCalendar(2021, Calendar.MARCH, 11).getTime();
	static final Date DATA_DUE_DATE = new GregorianCalendar(2021, Calendar.MARCH, 18).getTime();
	static final Date DATA_RETRACT_DATE = new GregorianCalendar(2021, Calendar.MARCH, 20).getTime();

	// 120 seconds < assessment time limit
	static final int SHORT_TIME_LIMIT_HOURS = 0;
	static final int SHORT_TIME_LIMIT_MINUTES = 2;

	// 240 seconds > assessment time limit
	static final int LONG_TIME_LIMIT_HOURS = 0;
	static final int LONG_TIME_LIMIT_MINUTES = 4;

	static final Date GRACEFUL_START_DATE = new GregorianCalendar(2021, Calendar.MARCH, 10).getTime();
	static final Date GRACEFUL_DUE_DATE = new GregorianCalendar(2021, Calendar.MARCH, 19).getTime();
	static final Date GRACEFUL_RETRACT_DATE = new GregorianCalendar(2021, Calendar.MARCH, 21).getTime();

	static final Date RESTRICTING_START_DATE = new GregorianCalendar(2021, Calendar.MARCH, 12).getTime();
	static final Date RESTRICTING_DUE_DATE = new GregorianCalendar(2021, Calendar.MARCH, 17).getTime();
	static final Date RESTRICTING_RETRACT_DATE = new GregorianCalendar(2021, Calendar.MARCH, 19).getTime();

	static final ExtendedTime EXT_NO_TIME_LIMIT = new ExtendedTime();
	static final ExtendedTime EXT_SHORT_TIME_LIMIT = new ExtendedTime();
	static final ExtendedTime EXT_LONG_TIME_LIMIT = new ExtendedTime();
	static final ExtendedTime EXT_USER = new ExtendedTime();
	static final ExtendedTime EXT_GRACEFUL_WINDOW = new ExtendedTime();
	static final ExtendedTime EXT_GRACEFUL_START_DATE = new ExtendedTime();
	static final ExtendedTime EXT_GRACEFUL_DUE_DATE = new ExtendedTime();
	static final ExtendedTime EXT_GRACEFUL_RETRACT_DATE = new ExtendedTime();
	static final ExtendedTime EXT_START_AND_RETRACT_DATE = new ExtendedTime();
	static final ExtendedTime EXT_SHORT_TIME_LIMIT_WITH_GRACEFUL_WINDOW = new ExtendedTime();
	static final ExtendedTime EXT_LONG_TIME_LIMIT_WITH_RESTRICTING_WINDOW = new ExtendedTime();
	static final ExtendedTime EXT_LONG_TIME_LIMIT_WITH_GRACEFUL_WINDOW = new ExtendedTime();

	static {
		EXT_NO_TIME_LIMIT.setTimeHours(0);
		EXT_NO_TIME_LIMIT.setTimeMinutes(0);

		EXT_SHORT_TIME_LIMIT.setTimeHours(SHORT_TIME_LIMIT_HOURS);
		EXT_SHORT_TIME_LIMIT.setTimeMinutes(SHORT_TIME_LIMIT_MINUTES);

		EXT_LONG_TIME_LIMIT.setTimeHours(LONG_TIME_LIMIT_HOURS);
		EXT_LONG_TIME_LIMIT.setTimeMinutes(LONG_TIME_LIMIT_MINUTES);

		EXT_USER.setUser("test");

		EXT_GRACEFUL_WINDOW.setStartDate(GRACEFUL_START_DATE);
		EXT_GRACEFUL_WINDOW.setDueDate(GRACEFUL_DUE_DATE);
		EXT_GRACEFUL_WINDOW.setRetractDate(GRACEFUL_RETRACT_DATE);

		EXT_GRACEFUL_START_DATE.setStartDate(GRACEFUL_START_DATE);

		EXT_GRACEFUL_DUE_DATE.setDueDate(GRACEFUL_DUE_DATE);

		EXT_GRACEFUL_RETRACT_DATE.setRetractDate(GRACEFUL_RETRACT_DATE);

		EXT_START_AND_RETRACT_DATE.setStartDate(GRACEFUL_START_DATE);
		EXT_START_AND_RETRACT_DATE.setRetractDate(GRACEFUL_RETRACT_DATE);

		EXT_SHORT_TIME_LIMIT_WITH_GRACEFUL_WINDOW.setTimeHours(SHORT_TIME_LIMIT_HOURS);
		EXT_SHORT_TIME_LIMIT_WITH_GRACEFUL_WINDOW.setTimeMinutes(SHORT_TIME_LIMIT_MINUTES);
		EXT_SHORT_TIME_LIMIT_WITH_GRACEFUL_WINDOW.setStartDate(GRACEFUL_START_DATE);
		EXT_SHORT_TIME_LIMIT_WITH_GRACEFUL_WINDOW.setDueDate(GRACEFUL_DUE_DATE);
		EXT_SHORT_TIME_LIMIT_WITH_GRACEFUL_WINDOW.setRetractDate(GRACEFUL_RETRACT_DATE);

		EXT_LONG_TIME_LIMIT_WITH_RESTRICTING_WINDOW.setTimeHours(LONG_TIME_LIMIT_HOURS);
		EXT_LONG_TIME_LIMIT_WITH_RESTRICTING_WINDOW.setTimeMinutes(LONG_TIME_LIMIT_MINUTES);
		EXT_LONG_TIME_LIMIT_WITH_RESTRICTING_WINDOW.setStartDate(RESTRICTING_START_DATE);
		EXT_LONG_TIME_LIMIT_WITH_RESTRICTING_WINDOW.setDueDate(RESTRICTING_DUE_DATE);
		EXT_LONG_TIME_LIMIT_WITH_RESTRICTING_WINDOW.setRetractDate(RESTRICTING_RETRACT_DATE);

		EXT_LONG_TIME_LIMIT_WITH_GRACEFUL_WINDOW.setTimeHours(LONG_TIME_LIMIT_HOURS);
		EXT_LONG_TIME_LIMIT_WITH_GRACEFUL_WINDOW.setTimeMinutes(LONG_TIME_LIMIT_MINUTES);
		EXT_LONG_TIME_LIMIT_WITH_GRACEFUL_WINDOW.setStartDate(GRACEFUL_START_DATE);
		EXT_LONG_TIME_LIMIT_WITH_GRACEFUL_WINDOW.setDueDate(GRACEFUL_DUE_DATE);
		EXT_LONG_TIME_LIMIT_WITH_GRACEFUL_WINDOW.setRetractDate(GRACEFUL_RETRACT_DATE);

		ASSESSMENT_TIME_LIMIT_ONLY.setTimeLimit(DATA_TIME_LIMIT);
		ASSESSMENT_TIME_LIMIT_ONLY.setLateHandling(AssessmentAccessControlIfc.ACCEPT_LATE_SUBMISSION);

		ASSESSMENT_TIME_LIMIT_AND_DATES.setTimeLimit(DATA_TIME_LIMIT);
		ASSESSMENT_TIME_LIMIT_AND_DATES.setStartDate(DATA_START_DATE);
		ASSESSMENT_TIME_LIMIT_AND_DATES.setDueDate(DATA_DUE_DATE);
		ASSESSMENT_TIME_LIMIT_AND_DATES.setRetractDate(DATA_RETRACT_DATE);
		ASSESSMENT_TIME_LIMIT_AND_DATES.setLateHandling(AssessmentAccessControlIfc.ACCEPT_LATE_SUBMISSION);

		ASSESSMENT_START_DATE_ONLY.setStartDate(DATA_START_DATE);
		ASSESSMENT_START_DATE_ONLY.setLateHandling(AssessmentAccessControlIfc.ACCEPT_LATE_SUBMISSION);

		ASSESSMENT_DUE_DATE_ONLY.setDueDate(DATA_DUE_DATE);
		ASSESSMENT_DUE_DATE_ONLY.setLateHandling(AssessmentAccessControlIfc.ACCEPT_LATE_SUBMISSION);

		ASSESSMENT_RETRACT_DATE_ONLY.setRetractDate(DATA_RETRACT_DATE);
		ASSESSMENT_RETRACT_DATE_ONLY.setLateHandling(AssessmentAccessControlIfc.ACCEPT_LATE_SUBMISSION);

		ASSESSMENT_DATES.setStartDate(DATA_START_DATE);
		ASSESSMENT_DATES.setDueDate(DATA_DUE_DATE);
		ASSESSMENT_DATES.setRetractDate(DATA_RETRACT_DATE);
		ASSESSMENT_DATES.setLateHandling(AssessmentAccessControlIfc.ACCEPT_LATE_SUBMISSION);

		ASSESSMENT_DATES_NALS.setStartDate(DATA_START_DATE);
		ASSESSMENT_DATES_NALS.setDueDate(DATA_DUE_DATE);
		ASSESSMENT_DATES_NALS.setRetractDate(DATA_RETRACT_DATE);
		ASSESSMENT_DATES_NALS.setLateHandling(AssessmentAccessControlIfc.NOT_ACCEPT_LATE_SUBMISSION);
	}

	/**
	 * Test empty list, and a collection with one element
	 */
	@Test
	public void testPickExtendedTimeShortCircuits() {
		// Empty set:
		List<ExtendedTime> extendedTimes = new ArrayList<>();
		Optional<ExtendedTime> result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_TIME_LIMIT_ONLY, extendedTimes);
		Assert.assertEquals(Optional.empty(), result);

		// List with 1 element
		ExtendedTime instance = new ExtendedTime();
		instance.setTimeHours(1);
		instance.setTimeMinutes(30);
		instance.setDueDate(new Date());
		extendedTimes.add(instance);
		result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_TIME_LIMIT_ONLY, extendedTimes);
		Assert.assertEquals(instance, result.get());
	}

	/**
	 * Ensure user specific ExtendedTime instances take precedence
	 */
	@Test
	public void testPickExtendedTimeUserPrecedence() {
		List<ExtendedTime> extendedTimes = new ArrayList<>();
		extendedTimes.add(EXT_LONG_TIME_LIMIT);
		extendedTimes.add(EXT_USER);
		extendedTimes.add(EXT_GRACEFUL_WINDOW);

		Optional<ExtendedTime> result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_TIME_LIMIT_ONLY, extendedTimes);
		Assert.assertEquals(EXT_USER, result.get());
		result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_TIME_LIMIT_AND_DATES, extendedTimes);
		Assert.assertEquals(EXT_USER, result.get());
		result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_DATES, extendedTimes);
		Assert.assertEquals(EXT_USER, result.get());
	}

	/**
	 * Ensure the longest time limit takes precedence
	 */
	@Test
	public void testPickExtendedTimeTimeLimitPrecedence() {
		List<ExtendedTime> extendedTimes = new ArrayList<>();
		extendedTimes.add(EXT_LONG_TIME_LIMIT);
		extendedTimes.add(EXT_SHORT_TIME_LIMIT);

		// Longer time limit takes precedence
		Optional<ExtendedTime> result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_TIME_LIMIT_ONLY, extendedTimes);
		Assert.assertEquals(EXT_LONG_TIME_LIMIT, result.get());
		result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_TIME_LIMIT_AND_DATES, extendedTimes);
		Assert.assertEquals(EXT_LONG_TIME_LIMIT, result.get());

		// Add an extension with no time limit
		extendedTimes.add(EXT_NO_TIME_LIMIT);
		result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_TIME_LIMIT_ONLY, extendedTimes);
		Assert.assertEquals(EXT_NO_TIME_LIMIT, result.get());
		result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_TIME_LIMIT_AND_DATES, extendedTimes);
		Assert.assertEquals(EXT_NO_TIME_LIMIT, result.get());
	}

	@Test
	public void testPickExtendedTimeWindowPrecedence() {
		List<ExtendedTime> extendedTimes = new ArrayList<>();
		extendedTimes.add(EXT_GRACEFUL_START_DATE);
		extendedTimes.add(EXT_GRACEFUL_DUE_DATE);
		extendedTimes.add(EXT_GRACEFUL_RETRACT_DATE);

		Optional<ExtendedTime> result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_START_DATE_ONLY, extendedTimes);
		Assert.assertTrue(EXT_GRACEFUL_START_DATE.equals(result.get()));
		result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_DUE_DATE_ONLY, extendedTimes);
		Assert.assertTrue(EXT_GRACEFUL_DUE_DATE.equals(result.get()));
		result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_RETRACT_DATE_ONLY, extendedTimes);
		Assert.assertTrue(EXT_GRACEFUL_RETRACT_DATE.equals(result.get()));
	}

	@Test
	public void testPickExtendedTimeEqualWindowDurationPrecedence() {
		List<ExtendedTime> extendedTimes = new ArrayList<>();
		extendedTimes.add(EXT_GRACEFUL_START_DATE);
		extendedTimes.add(EXT_GRACEFUL_DUE_DATE);
		extendedTimes.add(EXT_GRACEFUL_RETRACT_DATE);

		// graceful start date and graceful retract date offer equal durations; but a later window is more graceful than an earlier window.
		Optional<ExtendedTime> result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_DATES, extendedTimes);
		Assert.assertEquals(EXT_GRACEFUL_RETRACT_DATE, result.get());
		result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_DATES_NALS, extendedTimes);
		Assert.assertEquals(EXT_GRACEFUL_DUE_DATE, result.get());
	}

	@Test
	public void testPickExtendedTimeOpenEndedWindowPrecedence() {
		List<ExtendedTime> extendedTimes = new ArrayList<>();
		extendedTimes.add(EXT_GRACEFUL_START_DATE);
		extendedTimes.add(EXT_GRACEFUL_RETRACT_DATE);

		/*
		 * Referring to DeliveryBean.isAvailable(), ExtendedTime can enforce start dates / retract dates, even if the assessment isn't configured to use them.
		 * When choosing between limiting the start date and limiting the retract date, limiting the start date is preferable to give the student unlimited time:
		 * ENTITY                    | START DATE | RETRACT DATE
		 * Assessment Settings       | -INFINITY  | INFINITY
		 * EXT_GRACEFUL_START_DATE   | Start Date | INFINITY
		 * EXT_GRACEFUL_RETRACT_DATE | -INFINITY  | Retract Date
		 */
		Optional<ExtendedTime> result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_TIME_LIMIT_ONLY, extendedTimes);
		Assert.assertEquals(EXT_GRACEFUL_START_DATE, result.get());
	}

	/**
	 * NOT_ACCEPT_LATE_SUBMISSION tests
	 * Assessments with NALS submit on the due date; precedence is not impacted by the retract date.
	 */
	@Test
	public void testPickExtendedTimeNALS() {
		List<ExtendedTime> extendedTimes = new ArrayList<>();
		extendedTimes.add(EXT_GRACEFUL_START_DATE);
		extendedTimes.add(EXT_GRACEFUL_RETRACT_DATE);

		// Retract date doesn't extend the window under NALS; start date should take precedence
		Optional<ExtendedTime> result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_DATES_NALS, extendedTimes);
		Assert.assertEquals(EXT_GRACEFUL_START_DATE, result.get());

		// An extended due date provides a more graceful window than an extended start date.
		extendedTimes.add(EXT_GRACEFUL_DUE_DATE);
		result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_DATES_NALS, extendedTimes);
		Assert.assertEquals(EXT_GRACEFUL_DUE_DATE, result.get());
	}

	/**
	 * If late submissions are accepted and all the start and retract dates are the same, a later due date should take precedence.
	 */
	@Test
	public void testPickedExtendedTimeDueDateShift() {
		List<ExtendedTime> extendedTimes = new ArrayList<>();
		extendedTimes.add(EXT_START_AND_RETRACT_DATE);
		extendedTimes.add(EXT_GRACEFUL_WINDOW);

		Optional<ExtendedTime> result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_DATES, extendedTimes);
		Assert.assertEquals(EXT_GRACEFUL_WINDOW, result.get());
	}

	/**
	 * Tests when both time limits and windows come into play
	 */
	@Test
	public void testPickExtendedTimeMixed() {
		/*
		 * If an assessment has a time limit, extensions can clear it.
		 * If an assessment has no time limit, extensions can introduce one.
		 *
		 * Cases:
		 * Assessment has a time limit | Extensions with time limits | Result
		 * No                          | None                        | Greatest window
		 * Yes                         | None                        | Greatest window
		 * No                          | Some                        | Greatest window without a time limit
		 * Yes                         | Some                        | Greatest window without a time limit
		 * No                          | All                         | Greatest time limit
		 * Yes                         | All                         | Greatest time limit
		 */

		List<ExtendedTime> extendedTimes = new ArrayList<>();
		extendedTimes.add(EXT_GRACEFUL_RETRACT_DATE);
		extendedTimes.add(EXT_GRACEFUL_WINDOW);

		Optional<ExtendedTime> result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_DATES, extendedTimes);
		Assert.assertEquals(EXT_GRACEFUL_WINDOW, result.get());
		result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_TIME_LIMIT_AND_DATES, extendedTimes);
		Assert.assertEquals(EXT_GRACEFUL_WINDOW, result.get());

		extendedTimes.clear();
		extendedTimes.add(EXT_LONG_TIME_LIMIT_WITH_RESTRICTING_WINDOW);
		extendedTimes.add(EXT_SHORT_TIME_LIMIT_WITH_GRACEFUL_WINDOW);
		extendedTimes.add(EXT_GRACEFUL_WINDOW);

		result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_DATES, extendedTimes);
		Assert.assertEquals(EXT_GRACEFUL_WINDOW, result.get());
		result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_TIME_LIMIT_AND_DATES, extendedTimes);
		Assert.assertEquals(EXT_GRACEFUL_WINDOW, result.get());

		extendedTimes.clear();
		extendedTimes.add(EXT_SHORT_TIME_LIMIT_WITH_GRACEFUL_WINDOW);
		extendedTimes.add(EXT_SHORT_TIME_LIMIT);
		extendedTimes.add(EXT_LONG_TIME_LIMIT);

		result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_DATES, extendedTimes);
		Assert.assertEquals(EXT_LONG_TIME_LIMIT, result.get());
		result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_TIME_LIMIT_AND_DATES, extendedTimes);
		Assert.assertEquals(EXT_LONG_TIME_LIMIT, result.get());

		// Bonus - ensure when time limits are equal, windows come into play
		extendedTimes.add(EXT_LONG_TIME_LIMIT_WITH_RESTRICTING_WINDOW);
		result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_DATES, extendedTimes);
		Assert.assertEquals(EXT_LONG_TIME_LIMIT, result.get());

		extendedTimes.add(EXT_LONG_TIME_LIMIT_WITH_GRACEFUL_WINDOW);
		result = ExtendedTimeDeliveryService.pickExtendedTime(ASSESSMENT_DATES, extendedTimes);
		Assert.assertEquals(EXT_LONG_TIME_LIMIT_WITH_GRACEFUL_WINDOW, result.get());
	}
}
