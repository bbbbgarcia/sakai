package org.sakaiproject.office365.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfficeFolder {
	public Integer childCount;
}
