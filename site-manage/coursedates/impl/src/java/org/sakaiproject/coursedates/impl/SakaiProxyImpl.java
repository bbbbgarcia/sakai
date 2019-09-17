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
import org.sakaiproject.coursedates.api.model.AssignmentUpdate;
import org.sakaiproject.coursedates.api.model.CourseDatesValidation;
import org.sakaiproject.coursedates.api.model.CourseDatesError;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.Preferences;
import org.sakaiproject.user.api.PreferencesService;
import org.sakaiproject.util.DateFormatterUtil;

public class SakaiProxyImpl implements SakaiProxy {

	private static final String DATEPICKER_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

	@Setter private ToolManager toolManager;
	@Setter private SessionManager sessionManager;
	@Setter private PreferencesService prefService;
	@Setter private AssignmentService assignmentService;

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
		return prefService.getLocale(getCurrentUserId());
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
		Locale userLocale = getUserLocale();
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

			if (assignmentId == null) {
				errors.add(new CourseDatesError("assignment", "Assignment could not be found", "assignments", "Assignments", i));
				continue;
			}

			String assignmentReference = assignmentService.assignmentReference(siteId, assignmentId);
			boolean canUpdate = assignmentService.allowUpdateAssignment(assignmentReference);

			if (!canUpdate) {
				errors.add(new CourseDatesError("assignment", "Update permission denied", "assignments", "Assignments", i));
				continue;
			}

			TimeZone userTimeZone = getUserTimeZone();

			Instant openDate = parseStringToInstant((String)jsonAssignment.get("open_date"), userTimeZone);
			Instant dueDate = parseStringToInstant((String)jsonAssignment.get("due_date"), userTimeZone);
			Instant acceptUntil = parseStringToInstant((String)jsonAssignment.get("accept_until"), userTimeZone);

			boolean errored = false;

			if (openDate == null) {
				errors.add(new CourseDatesError("open_date", "Could not read Open date", "assignments", "Assignments", i));
				errored = true;
			}
			if (dueDate == null) {
				errors.add(new CourseDatesError("due_date", "Could not read Due date", "assignments", "Assignments", i));
				errored = true;
			}
			if (acceptUntil == null) {
				errors.add(new CourseDatesError("accept_until", "Could not read Accept Until date", "assignments", "Assignments", i));
				errored = true;
			}

			if (errored) {
				continue;
			}

			Assignment assignment = assignmentService.getAssignment(assignmentId);

			if (assignment == null) {
				errors.add(new CourseDatesError("assignment", "Assignment could not be found", "assignments", "Assignments", i));
				continue;
			}

			AssignmentUpdate update = new AssignmentUpdate(assignment, openDate, dueDate, acceptUntil/*,(Boolean)jsonAssignment.get("published")*/);

			if (!update.openDate.isBefore(update.dueDate)) {
				errors.add(new CourseDatesError("open_date", "Open date must fall before due date", "assignments", "Assignments", i));
				continue;
			}

			if (update.dueDate.isAfter(update.acceptUntilDate)) {
				errors.add(new CourseDatesError("due_date", "Due date cannot fall after Accept Until date", "assignments", "Assignments", i));
				continue;
			}

			updates.add(update);
		}

		assignmentValidate.setErrors(errors);
		assignmentValidate.setUpdates(updates);
		return assignmentValidate;
	}

	@Override
	public void updateAssignments(CourseDatesValidation assignmentValidate) throws Exception {
		for (AssignmentUpdate update : (List<AssignmentUpdate>)(Object) assignmentValidate.getUpdates()) {
			Assignment assignment = update.assignment;

			assignment.setOpenDate(update.openDate);
			assignment.setDueDate(update.dueDate);
			assignment.setCloseDate(update.acceptUntilDate);

			assignmentService.updateAssignment(assignment);
		}
	}
}
