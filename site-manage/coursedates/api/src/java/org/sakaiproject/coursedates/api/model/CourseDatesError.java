package org.sakaiproject.coursedates.api.model;

public class CourseDatesError {
	public String field;
	public String msg;
	public String toolId;
	public String toolTitle;
	public int idx;

	public CourseDatesError(String field, String msg, String toolId, String toolTitle, int idx) {
		this.field = field;
		this.msg = msg;
		this.toolId = toolId;
		this.toolTitle = toolTitle;
		this.idx = idx;
	}
}
