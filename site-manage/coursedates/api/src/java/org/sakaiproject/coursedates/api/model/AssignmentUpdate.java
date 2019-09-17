package org.sakaiproject.coursedates.api.model;

import java.time.Instant;

import org.sakaiproject.assignment.api.model.Assignment;

public class AssignmentUpdate {
	public Assignment assignment;
	public Instant openDate;
	public Instant dueDate;
	public Instant acceptUntilDate;

	public AssignmentUpdate(Assignment assignment, Instant openDate, Instant dueDate, Instant acceptUntilDate/*, boolean published*/) {
		this.assignment = assignment;
		this.openDate = openDate;
		this.dueDate = dueDate;
		this.acceptUntilDate = acceptUntilDate;
	}
}
