package org.sakaiproject.office365.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfficeParent {

	@JsonProperty(value = "id")
	public String parentId;
}
