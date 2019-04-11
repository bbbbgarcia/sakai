package org.sakaiproject.office365.model;

import java.util.Comparator;

public class OfficeItemComparator implements Comparator<OfficeItem> {
    @Override
    public int compare(OfficeItem o1, OfficeItem o2) {
    	String o1Name = o1.getOfficeItemId();
		String o2Name = o2.getOfficeItemId();
		if(!o1.isFolder()){
			o1Name = o1.getParent().getParentId() + o1Name;
		}
		if(!o2.isFolder()){
			o2Name = o2.getParent().getParentId() + o2Name;
		}
        return o1Name.compareTo(o2Name);
    }
}