package org.sakaiproject.coursedates.impl;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.TimeZone;

import lombok.extern.slf4j.Slf4j;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.sakaiproject.announcement.api.AnnouncementChannel;
import org.sakaiproject.announcement.api.AnnouncementMessage;
import org.sakaiproject.announcement.api.AnnouncementMessageEdit;
import org.sakaiproject.announcement.api.AnnouncementMessageHeader;
import org.sakaiproject.announcement.api.AnnouncementService;
import org.sakaiproject.api.app.messageforums.BaseForum;
import org.sakaiproject.api.app.messageforums.DiscussionForum;
import org.sakaiproject.api.app.messageforums.DiscussionTopic;
import org.sakaiproject.api.app.messageforums.MessageForumsForumManager;
import org.sakaiproject.api.app.messageforums.Topic;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.model.Assignment;
import org.sakaiproject.calendar.api.Calendar;
import org.sakaiproject.calendar.api.CalendarEvent;
import org.sakaiproject.calendar.api.CalendarEventEdit;
import org.sakaiproject.calendar.api.CalendarService;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentCollectionEdit;
import org.sakaiproject.content.api.ContentEntity;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.coursedates.api.SakaiProxy;
import org.sakaiproject.coursedates.api.model.CourseDatesUpdate;
import org.sakaiproject.coursedates.api.model.CourseDatesValidation;
import org.sakaiproject.coursedates.api.model.CourseDatesError;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.NotificationService;
//import org.sakaiproject.message.api.Message;
//import org.sakaiproject.message.api.MessageService;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.signup.logic.SignupMeetingService;
import org.sakaiproject.signup.model.SignupMeeting;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.time.api.Time;
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

@Slf4j
public class SakaiProxyImpl implements SakaiProxy {

	private static final String DATEPICKER_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private static final String DATEPICKER_DATE_FORMAT = "yyyy-MM-dd";

	@Setter private ToolManager toolManager;
	@Setter private SessionManager sessionManager;
	@Setter private PreferencesService prefService;

	@Setter private AssignmentService assignmentService;
	@Setter private PersistenceService assessmentPersistenceService;
	@Setter private AssessmentFacadeQueriesAPI assessmentServiceQueries;
	@Setter private PublishedAssessmentFacadeQueriesAPI pubAssessmentServiceQueries;
	@Setter private GradebookService gradebookService;
	@Setter private SignupMeetingService signupService;
	@Setter private ContentHostingService contentHostingService;
	@Setter private CalendarService calendarService;
	@Setter private TimeService timeService;
	@Setter private MessageForumsForumManager forumManager;
	@Setter private AnnouncementService announcementService;
	//@Setter private MessageService messageService;

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

	/** ASSIGNMENTS **/
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

	/** ASSESSMENTS **/
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

			if (lateHandling && update.dueDate.isAfter(update.acceptUntilDate)) {//if not null &&
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

	/** GRADEBOOK **/
	@Override
	public JSONArray getGradebookItemsForContext(String siteId) {
		JSONArray jsonAssignments = new JSONArray();
		Collection<org.sakaiproject.service.gradebook.shared.Assignment> gbitems = gradebookService.getAssignments(siteId);
		Locale userLocale = getUserLocale();
		for(org.sakaiproject.service.gradebook.shared.Assignment gbitem : gbitems) {
			if(!gbitem.isExternallyMaintained()) {
				JSONObject assobj = new JSONObject();
				assobj.put("id", gbitem.getId());
				assobj.put("title", gbitem.getName());
				assobj.put("due_date", DateFormatterUtil.format(gbitem.getDueDate(), DATEPICKER_DATE_FORMAT, userLocale));
				assobj.put("tool_title", "Gradebook");
				jsonAssignments.add(assobj);
			}
		}
		return jsonAssignments;
	}

	@Override
	public CourseDatesValidation validateGradebookItems(String siteId, JSONArray gradebookItems) throws Exception {
		CourseDatesValidation gradebookItemsValidate = new CourseDatesValidation();
		List<CourseDatesError> errors = new ArrayList<>();
		List<Object> updates = new ArrayList<>();

		if(!gradebookService.currentUserHasEditPerm(getCurrentSiteId())) {
			errors.add(new CourseDatesError("gbitem", "Update permission denied", "gradebookItems", "Gradebook", 0));
		}

		for (int i = 0; i < gradebookItems.size(); i++) {
			JSONObject jsonItem = (JSONObject)gradebookItems.get(i);

			Long itemId = (Long)jsonItem.get("id");
			if (itemId == null) {
				errors.add(new CourseDatesError("gbitem", "Gradebook item could not be found", "gradebookItems", "Gradebook", i));
				continue;
			}

			TimeZone userTimeZone = getUserTimeZone();
			Instant dueDate = parseStringToInstant((String)jsonItem.get("due_date"), userTimeZone);

			if (dueDate == null) {
				errors.add(new CourseDatesError("due_date", "Could not read Due date", "gradebookItems", "Gradebook", i));
				continue;
			}

			org.sakaiproject.service.gradebook.shared.Assignment gbitem = gradebookService.getAssignment(getCurrentSiteId(), itemId);
			if (gbitem == null) {
				errors.add(new CourseDatesError("gbitem", "Gradebook item could not be found", "gradebookItems", "Gradebook", i));
				continue;
			}

			CourseDatesUpdate update = new CourseDatesUpdate(gbitem, null, dueDate, null/*,(Boolean)jsonAssignment.get("published")*/);
			updates.add(update);
		}

		gradebookItemsValidate.setErrors(errors);
		gradebookItemsValidate.setUpdates(updates);
		return gradebookItemsValidate;
	}

	@Override
	public void updateGradebookItems(CourseDatesValidation gradebookItemsValidate) throws Exception {
		for (CourseDatesUpdate update : (List<CourseDatesUpdate>)(Object) gradebookItemsValidate.getUpdates()) {
			org.sakaiproject.service.gradebook.shared.Assignment assignmentDefinition = (org.sakaiproject.service.gradebook.shared.Assignment) update.object;
			assignmentDefinition.setDueDate(Date.from(update.dueDate));

			gradebookService.updateAssignment(getCurrentSiteId(), assignmentDefinition.getId(), assignmentDefinition);
		}
	}

	/** SIGNUP **/
	public JSONArray getSignupMeetingsForContext(String siteId) {
		JSONArray jsonMeetings = new JSONArray();
		Collection<SignupMeeting> meetings = signupService.getAllSignupMeetings(siteId, getCurrentUserId());
		Locale userLocale = getUserLocale();
		for(SignupMeeting meeting : meetings) {
			JSONObject mobj = new JSONObject();
			mobj.put("id", meeting.getId());
			mobj.put("title", meeting.getTitle());
			mobj.put("due_date", DateFormatterUtil.format(meeting.getEndTime(), DATEPICKER_DATETIME_FORMAT, userLocale));
			mobj.put("open_date", DateFormatterUtil.format(meeting.getStartTime(), DATEPICKER_DATETIME_FORMAT, userLocale));
			mobj.put("tool_title", "Sign-Up");
			jsonMeetings.add(mobj);
		}
		return jsonMeetings;
	}

	public CourseDatesValidation validateSignupMeetings(String siteId, JSONArray signupMeetings) throws Exception {
		CourseDatesValidation meetingsValidate = new CourseDatesValidation();
		List<CourseDatesError> errors = new ArrayList<>();
		List<Object> updates = new ArrayList<>();

		boolean canUpdate = signupService.isAllowedToCreateinSite(getCurrentUserId(), getCurrentSiteId());
		if (!canUpdate) {
			errors.add(new CourseDatesError("signup", "Update permission denied", "signupMeetings", "Sign Up", 0));
		}
		for (int i = 0; i < signupMeetings.size(); i++) {
			JSONObject jsonMeeting = (JSONObject)signupMeetings.get(i);

			Long meetingId = (Long)jsonMeeting.get("id");
			if (meetingId == null) {
				errors.add(new CourseDatesError("signup", "Meeting could not be found", "signupMeetings", "Sign Up", i));
				continue;
			}

			TimeZone userTimeZone = getUserTimeZone();
			Instant openDate = parseStringToInstant((String)jsonMeeting.get("open_date"), userTimeZone);
			Instant dueDate = parseStringToInstant((String)jsonMeeting.get("due_date"), userTimeZone);
			boolean errored = false;
			if (openDate == null) {
				errors.add(new CourseDatesError("open_date", "Could not read Open date", "signupMeetings", "Sign Up", i));
				errored = true;
			}
			if (dueDate == null) {
				errors.add(new CourseDatesError("due_date", "Could not read Due date", "signupMeetings", "Sign Up", i));
				errored = true;
			}
			if (errored) {
				continue;
			}

			SignupMeeting meeting = signupService.loadSignupMeeting(meetingId, getCurrentUserId(), getCurrentSiteId());
			if (meeting == null) {
				errors.add(new CourseDatesError("signup", "Meeting could not be found", "signupMeetings", "Sign Up", i));
				continue;
			}

			CourseDatesUpdate update = new CourseDatesUpdate(meeting, openDate, dueDate, null/*,(Boolean)jsonMeeting.get("published")*/);
			if (!update.openDate.isBefore(update.dueDate)) {
				errors.add(new CourseDatesError("open_date", "Open date must fall before due date", "signupMeetings", "Sign Up", i));
				continue;
			}
			updates.add(update);
		}
		meetingsValidate.setErrors(errors);
		meetingsValidate.setUpdates(updates);
		return meetingsValidate;
	}

	public void updateSignupMeetings(CourseDatesValidation signupValidate) throws Exception {
		for (CourseDatesUpdate update : (List<CourseDatesUpdate>)(Object) signupValidate.getUpdates()) {
			SignupMeeting meeting = (SignupMeeting) update.object;
			meeting.setStartTime(Date.from(update.openDate));
			meeting.setEndTime(Date.from(update.dueDate));

			signupService.updateSignupMeeting(meeting, false);
		}
	}

	/** RESOURCES **/
	public JSONArray getResourcesForContext(String siteId) {
		JSONArray jsonResources = new JSONArray();
		List<ContentEntity> unformattedList = contentHostingService.getAllEntities("/group/"+siteId+"/");
		//contentHostingService.findResources(ResourceProperties.FILE_TYPE, null, null, new HashSet<String>(Arrays.asList(siteId)));
		Locale userLocale = getUserLocale();
		for(ContentEntity res : unformattedList) {
			JSONObject mobj = new JSONObject();
			ResourceProperties contentResourceProps = res.getProperties();
			mobj.put("id", res.getId());
			mobj.put("title", contentResourceProps.getProperty(ResourceProperties.PROP_DISPLAY_NAME));
			if(res.getRetractDate() != null) mobj.put("due_date", DateFormatterUtil.format(new Date(res.getRetractDate().getTime()), DATEPICKER_DATETIME_FORMAT, userLocale));
			else mobj.put("due_date", null);
			if(res.getReleaseDate() != null) mobj.put("open_date", DateFormatterUtil.format(new Date(res.getReleaseDate().getTime()), DATEPICKER_DATETIME_FORMAT, userLocale));
			else mobj.put("open_date", null);
			mobj.put("extraInfo", StringUtils.defaultIfBlank(res.getProperties().getProperty(ResourceProperties.PROP_CONTENT_TYPE), "folder"));
			mobj.put("tool_title", "Resources");
			jsonResources.add(mobj);
		}
		return jsonResources;
	}

	public CourseDatesValidation validateResources(String siteId, JSONArray resources) throws Exception {
		CourseDatesValidation resourcesValidate = new CourseDatesValidation();
		List<CourseDatesError> errors = new ArrayList<>();
		List<Object> updates = new ArrayList<>();

		try {
			for (int i = 0; i < resources.size(); i++) {
				JSONObject jsonResource = (JSONObject)resources.get(i);

				String resourceId = (String)jsonResource.get("id");
				if (resourceId == null) {
					errors.add(new CourseDatesError("resource", "Meeting could not be found", "resources", "Resources", i));
					continue;
				}

				TimeZone userTimeZone = getUserTimeZone();
				Instant openDate = parseStringToInstant((String)jsonResource.get("open_date"), userTimeZone);
				Instant dueDate = parseStringToInstant((String)jsonResource.get("due_date"), userTimeZone);
				boolean errored = false;
				if (openDate == null) {
					errors.add(new CourseDatesError("open_date", "Could not read Open date", "resources", "Resources", i));
					errored = true;
				}
				if (dueDate == null) {
					errors.add(new CourseDatesError("due_date", "Could not read Due date", "resources", "Resources", i));
					errored = true;
				}
				if (errored) {
					continue;
				}

				String entityType = (String)jsonResource.get("extraInfo");
				CourseDatesUpdate update;
				if(!"folder".equals(entityType)) {
					ContentResourceEdit resource = contentHostingService.editResource(resourceId);
					if (resource == null) {
						errors.add(new CourseDatesError("resource", "Resource could not be found", "resources", "Resources", i));
						continue;
					}

					boolean canUpdate = contentHostingService.allowUpdateResource(resourceId);
					if (!canUpdate) {
						errors.add(new CourseDatesError("resource", "Update permission denied", "resources", "Resources", i));
					}
					update = new CourseDatesUpdate(resource, openDate, dueDate, null);
				} else {
					ContentCollectionEdit folder = contentHostingService.editCollection(resourceId);
					if (folder == null) {
						errors.add(new CourseDatesError("resource", "Folder could not be found", "resources", "Resources", i));
						continue;
					}

					boolean canUpdate = contentHostingService.allowUpdateCollection(resourceId);
					if (!canUpdate) {
						errors.add(new CourseDatesError("resource", "Update permission denied", "resources", "Resources", i));
					}
					update = new CourseDatesUpdate(folder, openDate, dueDate, null);
				}

				if (!update.openDate.isBefore(update.dueDate)) {//if not null && -> ojo, si era null le pone la de ahora
					errors.add(new CourseDatesError("open_date", "Open date must fall before due date", "resources", "Resources", i));
					continue;
				}
				update.setExtraInfo(entityType);
				updates.add(update);
			}
		//ENVOLVER TODOS LOS VALIDATES CON UN TRY CATCH PARA EVITAR PETESSS
		} catch(Exception e) {
			errors.add(new CourseDatesError("resource", "Uncaught error", "resources", "Resources", 0));
		}
		
		resourcesValidate.setErrors(errors);
		resourcesValidate.setUpdates(updates);
		return resourcesValidate;
	}

	public void updateResources(CourseDatesValidation resourceValidation) throws Exception {
		for (CourseDatesUpdate update : (List<CourseDatesUpdate>)(Object) resourceValidation.getUpdates()) {
			//if (update.object.getClass().equals(ContentCollectionEdit.class) || "org.sakaiproject.content.impl.BaseContentService$BaseCollectionEdit".equals(update.object.getClass().getName())) {
			if (update.object instanceof ContentCollectionEdit) {
				ContentCollectionEdit cce = (ContentCollectionEdit) update.object;
				cce.setRetractDate(timeService.newTime(Date.from(update.dueDate).getTime()));
				cce.setReleaseDate(timeService.newTime(Date.from(update.openDate).getTime()));
				contentHostingService.commitCollection(cce);
			} else {
				ContentResourceEdit cre = (ContentResourceEdit) update.object;
				cre.setRetractDate(timeService.newTime(Date.from(update.dueDate).getTime()));
				cre.setReleaseDate(timeService.newTime(Date.from(update.openDate).getTime()));
				contentHostingService.commitResource(cre);
			}
		}
	}

	/** CALENDAR EVENTS **/
	private static final int LIST_VIEW_YEAR_RANGE = 18;
	public JSONArray getCalendarEventsForContext(String siteId) {
		JSONArray jsonCalendar = new JSONArray();
		Locale userLocale = getUserLocale();
		int startYear = timeService.newTime().breakdownLocal().getYear() - LIST_VIEW_YEAR_RANGE / 2;
	 	int endYear = timeService.newTime().breakdownLocal().getYear() + LIST_VIEW_YEAR_RANGE / 2;
		Time startingListViewDate = timeService.newTimeLocal(startYear,/* startMonth, startDay,*/0, 0, 0, 0, 0, 0);
		Time endingListViewDate = timeService.newTimeLocal(endYear, 12, 31, 23, 59, 59, 99);
		try {
			Calendar c = getCalendar();
			if (c == null) {
				return jsonCalendar;
			}
			List<CalendarEvent> calendarEvents = c.getEvents(timeService.newTimeRange(startingListViewDate, endingListViewDate), null);
			for (CalendarEvent calendarEvent : calendarEvents) {
				JSONObject cobj = new JSONObject();
				cobj.put("id", calendarEvent.getId());
				cobj.put("title", calendarEvent.getDisplayName());
				cobj.put("open_date", DateFormatterUtil.format(new Date(calendarEvent.getRange().firstTime().getTime()), DATEPICKER_DATETIME_FORMAT, userLocale));
				//cobj.put("open_date", calendarEvent.getRange().firstTime().getTime());//pasar directamente el time??
				cobj.put("due_date", DateFormatterUtil.format(new Date(calendarEvent.getRange().lastTime().getTime()), DATEPICKER_DATETIME_FORMAT, userLocale));
				cobj.put("tool_title", "Calendar");
				jsonCalendar.add(cobj);
			}
		} catch(Exception e) {
			log.error("Error getting Calendar events for site {} : {}", siteId, e);
		}
		return jsonCalendar;
	}

	public CourseDatesValidation validateCalendarEvents(String siteId, JSONArray calendarEvents) throws Exception {
		CourseDatesValidation calendarValidate = new CourseDatesValidation();
		List<CourseDatesError> errors = new ArrayList<>();
		List<Object> updates = new ArrayList<>();

		Calendar c = getCalendar();
		if (c != null) {
			boolean canUpdate = calendarService.allowEditCalendar(c.getReference());
			if (!canUpdate) {
				errors.add(new CourseDatesError("calendar", "Update permission denied", "calendarEvents", "Calendar", 0));
			}
		}
		for (int i = 0; i < calendarEvents.size(); i++) {			
			JSONObject jsonEvent = (JSONObject)calendarEvents.get(i);

			String eventId = (String)jsonEvent.get("id");
			if (eventId == null) {
				errors.add(new CourseDatesError("calendar", "Event could not be found", "calendarEvents", "Calendar", i));
				continue;
			}

			TimeZone userTimeZone = getUserTimeZone();
			Instant openDate = parseStringToInstant((String)jsonEvent.get("open_date"), userTimeZone);
			Instant dueDate = parseStringToInstant((String)jsonEvent.get("due_date"), userTimeZone);
			boolean errored = false;
			if (openDate == null) {
				errors.add(new CourseDatesError("open_date", "Could not read Open date", "calendarEvents", "Calendar", i));
				errored = true;
			}
			if (dueDate == null) {
				errors.add(new CourseDatesError("due_date", "Could not read Due date", "calendarEvents", "Calendar", i));
				errored = true;
			}
			if (errored) {
				continue;
			}

			boolean canUpdate = c.allowEditEvent(eventId);
			if (!canUpdate) {
				errors.add(new CourseDatesError("calendar", "Update permission for event denied", "calendarEvents", "Calendar", i));
			}

			CalendarEventEdit calendarEvent = c.getEditEvent(eventId, calendarService.EVENT_MODIFY_CALENDAR);
			if (calendarEvent == null) {
				errors.add(new CourseDatesError("calendar", "Event could not be found", "calendarEvents", "Calendar", i));
				continue;
			}

			CourseDatesUpdate update = new CourseDatesUpdate(calendarEvent, openDate, dueDate, null);
			if (!update.openDate.isBefore(update.dueDate)) {
				errors.add(new CourseDatesError("open_date", "Open date must fall before due date", "calendarEvents", "Calendar", i));
				continue;
			}
			updates.add(update);
		}		
		
		calendarValidate.setErrors(errors);
		calendarValidate.setUpdates(updates);
		return calendarValidate;
	}

	public void updateCalendarEvents(CourseDatesValidation calendarValidation) throws Exception {
		Calendar c = getCalendar();
		if (c != null) {
			for (CourseDatesUpdate update : (List<CourseDatesUpdate>)(Object) calendarValidation .getUpdates()) {
				CalendarEventEdit edit = (CalendarEventEdit) update.object;
				long date1 = Date.from(update.openDate).getTime();
				long date2 = Date.from(update.dueDate).getTime() - date1;
				edit.setRange(timeService.newTimeRange(date1, date2));
				c.commitEvent(edit);
			}
		}
	}

	private Map<String, Calendar> calendarMap = new HashMap<>();
	private Calendar getCalendar() {
		if(calendarMap.get(getCurrentSiteId()) != null) { return calendarMap.get(getCurrentSiteId()); }
		try {
			String calendarId = calendarService.calendarReference(getCurrentSiteId(), SiteService.MAIN_CONTAINER);
			Calendar c = calendarService.getCalendar(calendarId);
			calendarMap.put(getCurrentSiteId(), c);
			return c;
		} catch (Exception ex) {
			log.warn("getCalendar : exception {}", ex.getMessage());
		}
		return null;
	}

	/** FORUMS **/
	public JSONArray getForumsForContext(String siteId) {
		JSONArray jsonForums = new JSONArray();
		Locale userLocale = getUserLocale();
		for (DiscussionForum forum : forumManager.getForumsForMainPage()) {
			JSONObject fobj = new JSONObject();
			fobj.put("id", forum.getId());
			fobj.put("title", forum.getTitle());
			if(forum.getAvailabilityRestricted()) {
				fobj.put("due_date", DateFormatterUtil.format(forum.getCloseDate(), DATEPICKER_DATETIME_FORMAT, userLocale));
				fobj.put("open_date", DateFormatterUtil.format(forum.getOpenDate(), DATEPICKER_DATETIME_FORMAT, userLocale));
			} else {
				fobj.put("due_date", null);
				fobj.put("open_date", null);
			}
			fobj.put("extraInfo", "forum");
			fobj.put("tool_title", "Forums");
			for (Object o : forum.getTopicsSet()) {
				DiscussionTopic topic = (DiscussionTopic)o;
				JSONObject tobj = new JSONObject();
				tobj.put("id", topic.getId());
				tobj.put("title", topic.getTitle());
				if(topic.getAvailabilityRestricted()) {
					tobj.put("due_date", DateFormatterUtil.format(topic.getCloseDate(), DATEPICKER_DATETIME_FORMAT, userLocale));
					tobj.put("open_date", DateFormatterUtil.format(topic.getOpenDate(), DATEPICKER_DATETIME_FORMAT, userLocale));
				} else {
					tobj.put("due_date", null);
					tobj.put("open_date", null);
				}
				tobj.put("extraInfo", "topic");
				tobj.put("tool_title", "Forums");
				jsonForums.add(tobj);
			}
			jsonForums.add(fobj);
		}
		return jsonForums;
	}

	public CourseDatesValidation validateForums(String siteId, JSONArray forums) throws Exception {
		CourseDatesValidation forumValidate = new CourseDatesValidation();
		List<CourseDatesError> errors = new ArrayList<>();
		List<Object> updates = new ArrayList<>();

		for (int i = 0; i < forums.size(); i++) {			
			JSONObject jsonForum = (JSONObject)forums.get(i);

			Long forumId = (Long)jsonForum.get("id");
			if (forumId == null) {
				errors.add(new CourseDatesError("forum", "Forum or topic could not be found", "forums", "Forums", i));
				continue;
			}

			TimeZone userTimeZone = getUserTimeZone();
			Instant openDate = parseStringToInstant((String)jsonForum.get("open_date"), userTimeZone);
			Instant dueDate = parseStringToInstant((String)jsonForum.get("due_date"), userTimeZone);
			boolean errored = false;
			if (openDate == null) {
				errors.add(new CourseDatesError("open_date", "Could not read Open date", "forums", "Forums", i));
				errored = true;
			}
			if (dueDate == null) {
				errors.add(new CourseDatesError("due_date", "Could not read Due date", "forums", "Forums", i));
				errored = true;
			}
			if (errored) {
				continue;
			}

			String entityType = (String)jsonForum.get("extraInfo");
			System.out.println("entityType " + entityType);
			CourseDatesUpdate update;
			if("forum".equals(entityType)) {
				BaseForum forum = forumManager.getForumById(true, forumId);
				if (forum == null) {
					errors.add(new CourseDatesError("forum", "Forum could not be found", "forums", "Forums", i));
					continue;
				}

				/*boolean canUpdate = contentHostingService.allowUpdateResource(resourceId);
				if (!canUpdate) {
					errors.add(new CourseDatesError("forum", "Update permission denied", "forums", "Forums", i));
				}*/
				update = new CourseDatesUpdate(forum, openDate, dueDate, null);
			} else {
				Topic topic = forumManager.getTopicById(true, forumId);
				if (topic == null) {
					errors.add(new CourseDatesError("forum", "Topic could not be found", "forums", "Forums", i));
					continue;
				}

/*				boolean canUpdate = contentHostingService.allowUpdateCollection(resourceId);
				if (!canUpdate) {
					errors.add(new CourseDatesError("forum", "Update permission denied", "forums", "Forums", i));
				}*/
				update = new CourseDatesUpdate(topic, openDate, dueDate, null);
			}

			if (!update.openDate.isBefore(update.dueDate)) {
				errors.add(new CourseDatesError("open_date", "Open date must fall before close date", "forums", "Forums", i));
				continue;
			}
			updates.add(update);
		}		
		
		forumValidate.setErrors(errors);
		forumValidate.setUpdates(updates);
		return forumValidate;
	}

	public void updateForums(CourseDatesValidation forumValidation) throws Exception {
		for (CourseDatesUpdate update : (List<CourseDatesUpdate>)(Object) forumValidation.getUpdates()) {System.out.println(update.object.getClass() + " - " + update.object.getClass().getName());
			//if (update.object.getClass().equals(BaseForum.class)) {System.out.println("11");
			if (update.object instanceof BaseForum) {
				DiscussionForum forum = (DiscussionForum) update.object;
				if(forum.getAvailabilityRestricted()) {
					forum.setOpenDate(Date.from(update.openDate));
					forum.setCloseDate(Date.from(update.dueDate));
				}
				forumManager.saveDiscussionForum(forum);
			} else {
				DiscussionTopic topic = (DiscussionTopic) update.object;
				if(topic.getAvailabilityRestricted()) {
					topic.setOpenDate(Date.from(update.openDate));
					topic.setCloseDate(Date.from(update.dueDate));
				}
				forumManager.saveDiscussionForumTopic(topic);
			}
		}
	}

	/** ANNOUNCEMENTS **/
	public JSONArray getAnnouncementsForContext(String siteId) {
		JSONArray jsonAnnouncements = new JSONArray();
		String anncRef = announcementService.channelReference(siteId, SiteService.MAIN_CONTAINER);
		try {
			//List announcements = announcementService.getMessages(anncRef, null, 0, true, false, true);
			Locale userLocale = getUserLocale();
			/*for(Object o : announcements) {
				AnnouncementMessage announcement = (AnnouncementMessage) o;
				JSONObject aobj = new JSONObject();
				aobj.put("id", announcement.getId());
				AnnouncementMessageHeader header = announcement.getAnnouncementHeader();
				aobj.put("title", header.getSubject());
				System.out.println("aa" + header.getSubject());
				aobj.put("due_date", announcement.getProperties().getInstantProperty("retractDate"));
				aobj.put("open_date", announcement.getProperties().getInstantProperty("releaseDate"));
				aobj.put("tool_title", "Announcements");
				jsonAnnouncements.add(aobj);
			}
			if(announcements == null || announcements.size() == 0) {
				System.out.println("tiro por aqui");*/
				List announcements = announcementService.getMessages(anncRef, null, false, true);
				for(Object o : announcements) {
					AnnouncementMessage announcement = (AnnouncementMessage) o;
				JSONObject aobj = new JSONObject();
				aobj.put("id", announcement.getId());
				AnnouncementMessageHeader header = announcement.getAnnouncementHeader();
				aobj.put("title", header.getSubject());
				if(announcement.getProperties().getProperty(AnnouncementService.RETRACT_DATE) != null) {
					aobj.put("due_date", announcement.getProperties().getInstantProperty(AnnouncementService.RETRACT_DATE));
				} else {
					aobj.put("due_date", null);
				}
				if(announcement.getProperties().getProperty(AnnouncementService.RELEASE_DATE) != null) {
					aobj.put("open_date", announcement.getProperties().getInstantProperty(AnnouncementService.RELEASE_DATE));
				} else {
					aobj.put("open_date", null);
				}
				aobj.put("tool_title", "Announcements");
				jsonAnnouncements.add(aobj);
				}
			//}
		} catch (Exception e) {
			log.error("getAnnouncementsForContext error for context {} : {}", siteId, e);
		}
		return jsonAnnouncements;
	}

	public CourseDatesValidation validateAnnouncements(String siteId, JSONArray announcements) throws Exception {
		CourseDatesValidation announcementValidate = new CourseDatesValidation();
		List<CourseDatesError> errors = new ArrayList<>();
		List<Object> updates = new ArrayList<>();

		String anncRef = announcementService.channelReference(siteId, SiteService.MAIN_CONTAINER);
		/*boolean canUpdate = announcementService.allowEditChanel(anncRef);
		if (!canUpdate) {
			errors.add(new CourseDatesError("announcement", "Update permission denied", "announcements", "Announcements", 0));
		}*/ //use messageService.allowEditChanel(anncRef) ??
		for (int i = 0; i < announcements.size(); i++) {
			JSONObject jsonAnnouncement = (JSONObject)announcements.get(i);

			String announcementId = (String)jsonAnnouncement.get("id");
			if (announcementId == null) {
				errors.add(new CourseDatesError("announcement", "Announcement could not be found", "announcements", "Announcements", i));
				continue;
			}

			TimeZone userTimeZone = getUserTimeZone();
			Instant openDate = parseStringToInstant((String)jsonAnnouncement.get("open_date"), userTimeZone);
			Instant dueDate = parseStringToInstant((String)jsonAnnouncement.get("due_date"), userTimeZone);
			boolean errored = false;
			if (openDate == null) {
				errors.add(new CourseDatesError("open_date", "Could not read Open date", "announcements", "Announcements", i));
				errored = true;
			}
			if (dueDate == null) {
				errors.add(new CourseDatesError("due_date", "Could not read Due date", "announcements", "Announcements", i));
				errored = true;
			}
			if (errored) {
				continue;
			}

			AnnouncementChannel aChannel = announcementService.getAnnouncementChannel(anncRef);
			AnnouncementMessageEdit announcement = aChannel.editAnnouncementMessage(announcementId);
			if (announcement == null) {
				errors.add(new CourseDatesError("announcement", "Announcement could not be found", "announcements", "Announcements", i));
				continue;
			}

			CourseDatesUpdate update = new CourseDatesUpdate(announcement, openDate, dueDate, null);
			if (!update.openDate.isBefore(update.dueDate)) {
				errors.add(new CourseDatesError("open_date", "Open date must fall before due date", "announcements", "Announcements", i));
				continue;
			}
			updates.add(update);
		}
		announcementValidate.setErrors(errors);
		announcementValidate.setUpdates(updates);
		return announcementValidate;
	}

	public void updateAnnouncements(CourseDatesValidation announcementValidate) throws Exception {
		String anncRef = announcementService.channelReference(getCurrentSiteId(), SiteService.MAIN_CONTAINER);
		AnnouncementChannel aChannel = announcementService.getAnnouncementChannel(anncRef);
		for (CourseDatesUpdate update : (List<CourseDatesUpdate>)(Object) announcementValidate.getUpdates()) {
			AnnouncementMessageEdit msg = (AnnouncementMessageEdit) update.object;
			msg.getPropertiesEdit().addProperty(AnnouncementService.RELEASE_DATE, timeService.newTime(Date.from(update.openDate).getTime()).toString());
			msg.getPropertiesEdit().addProperty(AnnouncementService.RETRACT_DATE, timeService.newTime(Date.from(update.dueDate).getTime()).toString());
			aChannel.commitMessage(msg, NotificationService.NOTI_IGNORE);
		}
	}

}
