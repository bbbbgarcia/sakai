package org.sakaiproject.coursedates.api.model;

import java.time.Instant;

public class CourseDatesUpdate {
	public Object object;
	public Instant openDate;
	public Instant dueDate;
	public Instant acceptUntilDate;

	public CourseDatesUpdate(Object object, Instant openDate, Instant dueDate, Instant acceptUntilDate) {
		this.object = object;
		this.openDate = openDate;
		this.dueDate = dueDate;
		this.acceptUntilDate = acceptUntilDate;
	}
}
