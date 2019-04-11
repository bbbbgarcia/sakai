package org.sakaiproject.office365.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfficeItem {

	@JsonProperty("id")
    private String officeItemId;

    private String name;
    
    private Integer size;
	
	@JsonProperty(value = "@microsoft.graph.downloadUrl")//this is always public
	//@JsonProperty(value = "webUrl")//this checks against onedrive permissions
    private String downloadUrl;
    
    private OfficeFolder folder;
    private OfficeFile file;

	@JsonProperty(value = "parentReference")
	private OfficeParent parent;

    public boolean isFolder() {
    	return folder != null;
    }
    public boolean hasChildren() {
    	return isFolder() && folder.childCount != 0;
    }
	
	private int depth = 0;
	private boolean expanded = false;

	@Override
	public boolean equals(Object obj) {
		boolean retVal = false;
		if (obj instanceof OfficeItem){
			OfficeItem ptr = (OfficeItem) obj;
			return this.officeItemId.equals(ptr.getOfficeItemId());
		}
		return retVal;
	}
}