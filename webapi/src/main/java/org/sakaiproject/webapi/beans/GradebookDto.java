package org.sakaiproject.webapi.beans;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GradebookDto {

	private String uid;

	private String name;

	private List<ItemDto> items;

}
