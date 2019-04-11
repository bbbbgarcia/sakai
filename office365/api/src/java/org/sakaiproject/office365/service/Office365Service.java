package org.sakaiproject.office365.service;

import java.util.List;

import org.sakaiproject.office365.model.OfficeItem;
import org.sakaiproject.office365.model.OfficeUser;

/**
 * Interface for communicating with the Office365 API.
 */
public interface Office365Service {

	// OFFICE CONSTANTS
	public final String OFFICE_PREFIX = "office365.";
	public final String OFFICE_ENABLED = OFFICE_PREFIX + "enabled";
	public final String OFFICE_CLIENT_ID = "client_id";
	public final String OFFICE_CLIENT_SECRET = "client_secret";
	public final String OFFICE_CODE = "code";
	public final String OFFICE_GRANT_TYPE = "grant_type";
	public final String OFFICE_GRANT_TYPE_DEFAULT = "authorization_code";
	public final String OFFICE_REDIRECT_URI = "redirect_uri";
	public final String OFFICE_REFRESH_TOKEN = "refresh_token";
	public final String OFFICE_RESPONSE_MODE = "response_mode";
	public final String OFFICE_RESPONSE_MODE_DEFAULT = "query";
	public final String OFFICE_RESPONSE_TYPE = "response_type";
	public final String OFFICE_RESPONSE_TYPE_DEFAULT = "code";
	public final String OFFICE_SCOPE = "scope";
	public final String OFFICE_SCOPE_DEFAULT_VALUES = "offline_access user.read files.read.all";//all in one variable, separate if necessary
	public final String OFFICE_STATE = "state";

	// ENDPOINTS
	public final String ENDPOINT_AUTHORIZE = "authorize";
	public final String ENDPOINT_GRAPH = "https://graph.microsoft.com/v1.0/";
	public final String ENDPOINT_LOGIN = "https://login.microsoftonline.com/common/oauth2/v2.0/";
	public final String ENDPOINT_DRIVES = "drives/";
	public final String ENDPOINT_ME = "me";
	public final String ENDPOINT_CHILDREN = "/children";
	public final String ENDPOINT_ITEMS = "/items/";
	public final String ENDPOINT_ROOT_CHILDREN = "/root/children";
	public final String ENDPOINT_TOKEN = "token";
	public final String JSON_ENTRY_VALUE = "value";
	public final String Q_MARK = "?";
	public final String SEPARATOR = "/";

	// FUNCTIONS
	public String authenticate();
	public List<OfficeItem> getDriveRootItems(String userId);
	public List<OfficeItem> getDriveChildrenItems(String userId, String itemId, int depth);
	public OfficeUser getOfficeUser(String userId);
	public OfficeUser refreshToken(String userId);
	public boolean token(String userId, String code);
	public boolean revokeOfficeConfiguration(String userId);
	public void cleanOfficeCacheForUser(String userId);

}