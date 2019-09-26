package org.sakaiproject.coursedates.api.model;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

public class CourseDatesUpdate {
	public Object object;
	public Instant openDate;
	public Instant dueDate;
	public Instant acceptUntilDate;

	@Getter @Setter
	public String extraInfo;//forum_topic, draft_published, folder_file

	public CourseDatesUpdate(Object object, Instant openDate, Instant dueDate, Instant acceptUntilDate) {
		this.object = object;
		this.openDate = openDate;
		this.dueDate = dueDate;
		this.acceptUntilDate = acceptUntilDate;
	}
}
