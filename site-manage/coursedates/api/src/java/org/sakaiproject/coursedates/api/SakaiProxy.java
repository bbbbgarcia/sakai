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
}
