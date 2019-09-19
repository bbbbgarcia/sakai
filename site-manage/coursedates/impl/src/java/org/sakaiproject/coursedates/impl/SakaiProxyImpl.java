package org.sakaiproject.coursedates.impl;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import lombok.Setter;

import org.apache.commons.lang3.StringUtils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.model.Assignment;
import org.sakaiproject.coursedates.api.SakaiProxy;
import org.sakaiproject.coursedates.api.model.CourseDatesUpdate;
import org.sakaiproject.coursedates.api.model.CourseDatesValidation;
import org.sakaiproject.coursedates.api.model.CourseDatesError;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.tool.assessment.data.ifc.assessment.AssessmentAccessControlIfc;
import org.sakaiproject.tool.assessment.facade.AssessmentFacade;
import org.sakaiproject.tool.assessment.facade.AssessmentFacadeQueries;
import org.sakaiproject.tool.assessment.facade.AssessmentFacadeQueriesAPI;
import org.sakaiproject.tool.assessment.facade.PublishedAssessmentFacade;
import org.sakaiproject.tool.assessment.facade.PublishedAssessmentFacadeQueries;
import org.sakaiproject.tool.assessment.facade.PublishedAssessmentFacadeQueriesAPI;
import org.sakaiproject.tool.assessment.services.PersistenceService;
import org.sakaiproject.user.api.Preferences;
import org.sakaiproject.user.api.PreferencesService;
import org.sakaiproject.util.DateFormatterUtil;

public class SakaiProxyImpl implements SakaiProxy {

	private static final String DATEPICKER_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

	@Setter private ToolManager toolManager;
	@Setter private SessionManager sessionManager;
	@Setter private PreferencesService prefService;

	@Setter private AssignmentService assignmentService;
	@Setter private PersistenceService assessmentPersistenceService;
	@Setter private AssessmentFacadeQueriesAPI assessmentServiceQueries;
	@Setter private PublishedAssessmentFacadeQueriesAPI pubAssessmentServiceQueries;

	public void init() {
		setAssessmentServiceQueries(assessmentPersistenceService.getAssessmentFacadeQueries());
		setPubAssessmentServiceQueries(assessmentPersistenceService.getPublishedAssessmentFacadeQueries());
	}

	@Override
	public String getCurrentSiteId() {
		return toolManager.getCurrentPlacement().getContext();
	}

	@Override
	public String getCurrentUserId() {
		return sessionManager.getCurrentSessionUserId();
	}

	@Override
	public Locale getUserLocale() {
		Locale locale = prefService.getLocale(getCurrentUserId());
		if (locale == null) locale = Locale.US;
		return locale;
	}

	private TimeZone getUserTimeZone() {
		TimeZone timezone;
		final Preferences prefs = prefService.getPreferences(getCurrentUserId());
		final ResourceProperties props = prefs.getProperties(TimeService.APPLICATION_ID);
		final String tzPref = props.getProperty(TimeService.TIMEZONE_KEY);

		if (StringUtils.isNotBlank(tzPref)) {
			timezone = TimeZone.getTimeZone(tzPref);
		} else {
			timezone = TimeZone.getDefault();
		}

		return timezone;
	}

	@Override
	public Instant parseStringToInstant(String timestamp, TimeZone userTimeZone) {
		try {
			return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
				.withZone(userTimeZone.toZoneId())
				.parse(timestamp, Instant::from);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	@Override
	public JSONArray getAssignmentsForContext(String siteId) {
		JSONArray jsonAssignments = new JSONArray();
		Collection<Assignment> assignments = assignmentService.getAssignmentsForContext(siteId);
		Locale userLocale = this.getUserLocale();
		for(Assignment assignment : assignments) {
			JSONObject assobj = new JSONObject();
			assobj.put("id", assignment.getId());
			assobj.put("title", assignment.getTitle());
			assobj.put("due_date", DateFormatterUtil.format(Date.from(assignment.getDueDate()), DATEPICKER_DATETIME_FORMAT, userLocale));
			assobj.put("open_date", DateFormatterUtil.format(Date.from(assignment.getOpenDate()), DATEPICKER_DATETIME_FORMAT, userLocale));
			assobj.put("accept_until", DateFormatterUtil.format(Date.from(assignment.getCloseDate()), DATEPICKER_DATETIME_FORMAT, userLocale));
			assobj.put("tool_title", "Assignments");
			jsonAssignments.add(assobj);
		}
		return jsonAssignments;
	}

	@Override
	public CourseDatesValidation validateAssignments(String siteId, JSONArray assignments) throws Exception {
		CourseDatesValidation assignmentValidate = new CourseDatesValidation();
		List<CourseDatesError> errors = new ArrayList<>();
		List<Object> updates = new ArrayList<>();

		for (int i = 0; i < assignments.size(); i++) {
			JSONObject jsonAssignment = (JSONObject)assignments.get(i);

			String assignmentId = (String)jsonAssignment.get("id");
			int idx = Integer.parseInt(jsonAssignment.get("idx").toString());

			if (assignmentId == null) {
				errors.add(new CourseDatesError("assignment", "Assignment could not be found", "assignments", "Assignments", idx));
				continue;
			}

			String assignmentReference = assignmentService.assignmentReference(siteId, assignmentId);
			boolean canUpdate = assignmentService.allowUpdateAssignment(assignmentReference);

			if (!canUpdate) {
				errors.add(new CourseDatesError("assignment", "Update permission denied", "assignments", "Assignments", idx));
				continue;
			}

			TimeZone userTimeZone = getUserTimeZone();

			Instant openDate = parseStringToInstant((String)jsonAssignment.get("open_date"), userTimeZone);
			Instant dueDate = parseStringToInstant((String)jsonAssignment.get("due_date"), userTimeZone);
			Instant acceptUntil = parseStringToInstant((String)jsonAssignment.get("accept_until"), userTimeZone);

			boolean errored = false;

			if (openDate == null) {
				errors.add(new CourseDatesError("open_date", "Could not read Open date", "assignments", "Assignments", idx));
				errored = true;
			}
			if (dueDate == null) {
				errors.add(new CourseDatesError("due_date", "Could not read Due date", "assignments", "Assignments", idx));
				errored = true;
			}
			if (acceptUntil == null) {
				errors.add(new CourseDatesError("accept_until", "Could not read Accept Until date", "assignments", "Assignments", idx));
				errored = true;
			}

			if (errored) {
				continue;
			}

			Assignment assignment = assignmentService.getAssignment(assignmentId);

			if (assignment == null) {
				errors.add(new CourseDatesError("assignment", "Assignment could not be found", "assignments", "Assignments", idx));
				continue;
			}

			CourseDatesUpdate update = new CourseDatesUpdate(assignment, openDate, dueDate, acceptUntil/*,(Boolean)jsonAssignment.get("published")*/);

			if (!update.openDate.isBefore(update.dueDate)) {
				errors.add(new CourseDatesError("open_date", "Open date must fall before due date", "assignments", "Assignments", idx));
				continue;
			}

			if (update.dueDate.isAfter(update.acceptUntilDate)) {
				errors.add(new CourseDatesError("due_date", "Due date cannot fall after Accept Until date", "assignments", "Assignments", idx));
				continue;
			}

			updates.add(update);
		}

		assignmentValidate.setErrors(errors);
		assignmentValidate.setUpdates(updates);
		return assignmentValidate;
	}

	@Override
	public void updateAssignments(CourseDatesValidation assignmentValidation) throws Exception {
		for (CourseDatesUpdate update : (List<CourseDatesUpdate>)(Object) assignmentValidation.getUpdates()) {
			Assignment assignment = (Assignment) update.object;
			assignment.setOpenDate(update.openDate);
			assignment.setDueDate(update.dueDate);
			assignment.setCloseDate(update.acceptUntilDate);
			assignmentService.updateAssignment(assignment);
		}
	}

	@Override
	public JSONArray getAssessmentsForContext(String siteId) {
		JSONArray jsonAssessments = new JSONArray();
		List<AssessmentFacade> assessments = assessmentServiceQueries.getAllAssessments(AssessmentFacadeQueries.TITLE);
		List<PublishedAssessmentFacade> pubAssessments = pubAssessmentServiceQueries.getAllPublishedAssessments(PublishedAssessmentFacadeQueries.TITLE);
		Locale userLocale = this.getUserLocale();
		for (AssessmentFacade assessment : assessments) {
				AssessmentAccessControlIfc control = assessment.getAssessmentAccessControl();
				boolean lateHandling = (control.getLateHandling() != null && control.getLateHandling() == AssessmentAccessControlIfc.ACCEPT_LATE_SUBMISSION);
				JSONObject assobj = new JSONObject();
				assobj.put("id", assessment.getAssessmentBaseId());
				// TODO: Don't show "Draft" inserting it into assessment title
				assobj.put("title", "Draft - " + assessment.getTitle());
				assobj.put("due_date", DateFormatterUtil.format(control.getDueDate(), DATEPICKER_DATETIME_FORMAT, userLocale));
				assobj.put("open_date", DateFormatterUtil.format(control.getStartDate(), DATEPICKER_DATETIME_FORMAT, userLocale));
				assobj.put("accept_until", DateFormatterUtil.format(control.getRetractDate(), DATEPICKER_DATETIME_FORMAT, userLocale));
				assobj.put("is_draft", true);
				assobj.put("late_handling", lateHandling);
				assobj.put("tool_title", "Tests & Quizzes");
				jsonAssessments.add(assobj);
		}
		for (PublishedAssessmentFacade assessment : pubAssessments) {
				AssessmentAccessControlIfc control = assessment.getAssessmentAccessControl();
				boolean lateHandling = (control.getLateHandling() != null && control.getLateHandling() == AssessmentAccessControlIfc.ACCEPT_LATE_SUBMISSION);
				JSONObject assobj = new JSONObject();
				assobj.put("id", assessment.getPublishedAssessmentId());
				assobj.put("title", assessment.getTitle());
				assobj.put("due_date", DateFormatterUtil.format(control.getDueDate(), DATEPICKER_DATETIME_FORMAT, userLocale));
				assobj.put("open_date", DateFormatterUtil.format(control.getStartDate(), DATEPICKER_DATETIME_FORMAT, userLocale));
				assobj.put("accept_until", DateFormatterUtil.format(control.getRetractDate(), DATEPICKER_DATETIME_FORMAT, userLocale));
				assobj.put("is_draft", false);
				assobj.put("late_handling", lateHandling);
				assobj.put("tool_title", "Tests & Quizzes");
				jsonAssessments.add(assobj);
		}
		return jsonAssessments;
	}

	@Override
	public CourseDatesValidation validateAssessments(String siteId, JSONArray assessments) throws Exception {
		CourseDatesValidation assessmentValidate = new CourseDatesValidation();
		List<CourseDatesError> errors = new ArrayList<>();
		List<Object> updates = new ArrayList<>();

		for (int i = 0; i < assessments.size(); i++) {
			JSONObject jsonAssessment = (JSONObject)assessments.get(i);

			Long assessmentId = Long.parseLong(jsonAssessment.get("id").toString());
			int idx = Integer.parseInt(jsonAssessment.get("idx").toString());

			/* VALIDATE IF USER CAN UPDATE THE ASSESSMENT */

			TimeZone userTimeZone = getUserTimeZone();

			Instant openDate = parseStringToInstant((String)jsonAssessment.get("open_date"), userTimeZone);
			Instant dueDate = parseStringToInstant((String)jsonAssessment.get("due_date"), userTimeZone);
			Instant acceptUntil = parseStringToInstant((String)jsonAssessment.get("accept_until"), userTimeZone);
						boolean isDraft = Boolean.parseBoolean(jsonAssessment.get("is_draft").toString());

						Object assessment;
						AssessmentAccessControlIfc control;
						if (isDraft) {
							assessment = assessmentServiceQueries.getAssessment(assessmentId);
							control = ((AssessmentFacade) assessment).getAssessmentAccessControl();
						} else {
							assessment = pubAssessmentServiceQueries.getPublishedAssessment(assessmentId);
							control = ((PublishedAssessmentFacade) assessment).getAssessmentAccessControl();
						}
						boolean lateHandling = control.getLateHandling() != null && control.getLateHandling() == AssessmentAccessControlIfc.ACCEPT_LATE_SUBMISSION;

			if (assessment == null) {
				errors.add(new CourseDatesError("assessment", "Assignment could not be found", "assessments", "Tests & Quizzes", idx));
				continue;
			}

			boolean errored = false;

			if (openDate == null) {
				errors.add(new CourseDatesError("open_date", "Could not read Open date", "assessments", "Tests & Quizzes", idx));
				errored = true;
			}
			if (dueDate == null) {
				errors.add(new CourseDatesError("due_date", "Could not read Due date", "assessments", "Tests & Quizzes", idx));
				errored = true;
			}
			if (acceptUntil == null && lateHandling) {
				errors.add(new CourseDatesError("accept_until", "Could not read Accept Until date", "assessments", "Tests & Quizzes", i));
				errored = true;
			}

			if (errored) {
				continue;
			}

			CourseDatesUpdate update = new CourseDatesUpdate(assessment, openDate, dueDate, acceptUntil/*,(Boolean)jsonAssignment.get("published")*/);

			if (!update.openDate.isBefore(update.dueDate)) {
				errors.add(new CourseDatesError("open_date", "Open date must fall before due date", "assessments", "Tests & Quizzes", idx));
				continue;
			}

			if (lateHandling && update.dueDate.isAfter(update.acceptUntilDate)) {
				errors.add(new CourseDatesError("due_date", "Due date cannot fall after Accept Until date", "assessments", "Tests & Quizzes", i));
				continue;
			}

			updates.add(update);
		}

		assessmentValidate.setErrors(errors);
		assessmentValidate.setUpdates(updates);
		return assessmentValidate;
	}

	@Override
	public void updateAssessments(CourseDatesValidation assessmentsValidation) throws Exception {
		for (CourseDatesUpdate update : (List<CourseDatesUpdate>)(Object) assessmentsValidation.getUpdates()) {
			if (update.object.getClass().equals(AssessmentFacade.class)) {
				AssessmentFacade assessment = (AssessmentFacade) update.object;
				AssessmentAccessControlIfc control = assessment.getAssessmentAccessControl();
				boolean lateHandling = control.getLateHandling() != null && control.getLateHandling() == AssessmentAccessControlIfc.ACCEPT_LATE_SUBMISSION;
				control.setStartDate(Date.from(update.openDate));
				control.setDueDate(Date.from(update.dueDate));
				if (lateHandling) control.setRetractDate(Date.from(update.acceptUntilDate));
				assessment.setAssessmentAccessControl(control);
				assessmentServiceQueries.saveOrUpdate(assessment);

			} else {
				PublishedAssessmentFacade assessment = (PublishedAssessmentFacade) update.object;
				AssessmentAccessControlIfc control = assessment.getAssessmentAccessControl();
				boolean lateHandling = control.getLateHandling() != null && control.getLateHandling() == AssessmentAccessControlIfc.ACCEPT_LATE_SUBMISSION;
				control.setStartDate(Date.from(update.openDate));
				control.setDueDate(Date.from(update.dueDate));
				if (lateHandling) control.setRetractDate(Date.from(update.acceptUntilDate));
				assessment.setAssessmentAccessControl(control);
				pubAssessmentServiceQueries.saveOrUpdate(assessment);
			}
		}
	}
}
