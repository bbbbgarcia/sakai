/**********************************************************************************
 * $URL: https://source.sakaiproject.org/svn/content/branches/SAK-12239/content-impl/impl/src/java/org/sakaiproject/content/impl/ContentServiceSqlOracle.java $
 * $Id: ContentServiceSqlOracle.java 38956 2007-12-03 18:10:39Z jimeng@umich.edu $
 ***********************************************************************************
 *
 * Copyright (c) 2007, 2008 Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.content.impl;

/**
 * methods for accessing content data in an oracle database.
 */
public class ContentServiceSqlOracle extends ContentServiceSqlDefault
{
	/**
	 * returns the sql statement which inserts the individual-dropbox-id, site-level dropbox-id and last-update fields into the content_dropbox_changes table.
	 */
	public String getInsertIndividualDropboxChangeSql() 
	{
		return "merge into CONTENT_DROPBOX_CHANGES using dual on (dual.dummy is not null and CONTENT_DROPBOX_CHANGES.DROPBOX_ID = ?) when not matched then insert (DROPBOX_ID, IN_COLLECTION, LAST_UPDATE) values (?, ?, ?) when matched then update set CONTENT_DROPBOX_CHANGES.IN_COLLECTION = ?, LAST_UPDATE = ?";
		// return "insert into CONTENT_DROPBOX_CHANGES (DROPBOX_ID, IN_COLLECTION, LAST_UPDATE) values (? , ? , ?) on duplicate key update IN_COLLECTION = ?, LAST_UPDATE = ?";
	}

	/**
	 * returns the sql statement which retrieves the number of content resources from the content_resource table.
	 */
	public String getNumContentResources1Sql()
	{
		return "select count(IN_COLLECTION) from CONTENT_RESOURCE where IN_COLLECTION like ? ESCAPE '\\'";
	}

	/**
	 * returns the sql statement which retrieves the number of content resources from the content_collection table.
	 */
	public String getNumContentResources2Sql()
	{
		return "select count(IN_COLLECTION) from CONTENT_COLLECTION where IN_COLLECTION like ? ESCAPE '\\'";
	}

	/**
	 * return only one file_path from the content_resource table.
	 */
	public String getOnlyOneFilePath(String resourceTableName){
		return "select file_path from " + resourceTableName + " where resource_sha256 = ? order by file_path desc fetch first 1 rows only";
	}

}
