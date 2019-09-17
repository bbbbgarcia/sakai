package org.sakaiproject.coursedates.api.model;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class CourseDatesValidation {
	private List<CourseDatesError> errors;
	private List<Object> updates;
}
