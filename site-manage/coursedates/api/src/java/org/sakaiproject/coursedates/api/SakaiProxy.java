package org.sakaiproject.coursedates.api;

import java.time.Instant;
import java.util.Locale;
import java.util.TimeZone;

import org.json.simple.JSONArray;

import org.sakaiproject.coursedates.api.model.CourseDatesValidation;

public interface SakaiProxy {
	// Global methods
	public String getCurrentUserId();
	public String getCurrentSiteId();
	public Locale getUserLocale();
	public Instant parseStringToInstant(String timestamp, TimeZone userTimeZone);

	// Assignments methods
	public JSONArray getAssignmentsForContext(String siteId);
	public CourseDatesValidation validateAssignments(String siteId, JSONArray assignments) throws Exception;
	public void updateAssignments(CourseDatesValidation assignmentsValidation) throws Exception;

	// Assessments methods
	public JSONArray getAssessmentsForContext(String siteId);
	public CourseDatesValidation validateAssessments(String siteId, JSONArray assessments) throws Exception;
	public void updateAssessments(CourseDatesValidation assessmentsValidation) throws Exception;

	// Gradebook methods
	public JSONArray getGradebookItemsForContext(String siteId);
	public CourseDatesValidation validateGradebookItems(String siteId, JSONArray gradebookItems) throws Exception;
	public void updateGradebookItems(CourseDatesValidation gradebookItemsValidation) throws Exception;

	// Signup methods
	public JSONArray getSignupMeetingsForContext(String siteId);
	public CourseDatesValidation validateSignupMeetings(String siteId, JSONArray signupMeetings) throws Exception;
	public void updateSignupMeetings(CourseDatesValidation signupValidation) throws Exception;

	// Resources methods
	public JSONArray getResourcesForContext(String siteId);
	public CourseDatesValidation validateResources(String siteId, JSONArray resources) throws Exception;
	public void updateResources(CourseDatesValidation resourceValidation) throws Exception;

	// Calendar methods
	public JSONArray getCalendarEventsForContext(String siteId);
	public CourseDatesValidation validateCalendarEvents(String siteId, JSONArray calendarEvents) throws Exception;
	public void updateCalendarEvents(CourseDatesValidation calendarValidation) throws Exception;

	// Forum methods
	public JSONArray getForumsForContext(String siteId);
	public CourseDatesValidation validateForums(String siteId, JSONArray forums) throws Exception;
	public void updateForums(CourseDatesValidation forumValidation) throws Exception;

	// Announcement methods
	public JSONArray getAnnouncementsForContext(String siteId);
	public CourseDatesValidation validateAnnouncements(String siteId, JSONArray announcements) throws Exception;
	public void updateAnnouncements(CourseDatesValidation announcementValidation) throws Exception;
}
