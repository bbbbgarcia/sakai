package org.sakaiproject.office365.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.office365.model.OfficeItem;
import org.sakaiproject.office365.model.OfficeToken;
import org.sakaiproject.office365.model.OfficeUser;
import org.sakaiproject.office365.repository.OfficeUserRepository;

/**
 * Implementation of the Office365Service interface.

 * @see Office365Service
 */
@Slf4j
public class Office365ServiceImpl implements Office365Service {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(Include.NON_NULL);

	@Setter private OfficeUserRepository officeRepo;

    @Getter @Setter
    private ServerConfigurationService serverConfigurationService;

	@Getter @Setter
    private MemoryService memoryService;

	private Cache<String, String> tokenCache;
	private Cache<String, OfficeUser> officeUserCache;
	private Cache<String, List<OfficeItem>> driveRootItemsCache;
	private Cache<String, List<OfficeItem>> driveChildrenItemsCache;

	private String clientId = null;
	private String clientSecret = null;
	private String redirectUri = null;
	private static final String state = "12345";//TODO random + check it on other calls?
	private String bearer = null;
	
	public void init() {
        log.debug("Office365ServiceImpl init");
		OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		clientId = serverConfigurationService.getString(OFFICE_PREFIX + OFFICE_CLIENT_ID, null);
		clientSecret = serverConfigurationService.getString(OFFICE_PREFIX + OFFICE_CLIENT_SECRET, null);
		redirectUri = serverConfigurationService.getString(OFFICE_PREFIX + OFFICE_REDIRECT_URI, "localhost:8080/portal/office365");

		tokenCache = memoryService.<String, String>getCache("org.sakaiproject.office365.service.tokenCache");
		driveRootItemsCache = memoryService.<String, List<OfficeItem>>getCache("org.sakaiproject.office365.service.driveRootItemsCache");
		driveChildrenItemsCache = memoryService.<String, List<OfficeItem>>getCache("org.sakaiproject.office365.service.driveChildrenItemsCache");
		officeUserCache = memoryService.<String, OfficeUser>getCache("org.sakaiproject.office365.service.officeUserCache");
    }

	public String authenticate() {
		log.debug("authenticate");
		if(!isConfigured()){
			return null;
		}
	
		Map<String,String> params = new HashMap<>();
    	params.put(OFFICE_CLIENT_ID, clientId);
    	params.put(OFFICE_RESPONSE_MODE, OFFICE_RESPONSE_MODE_DEFAULT);
    	params.put(OFFICE_RESPONSE_TYPE, OFFICE_RESPONSE_TYPE_DEFAULT);
    	params.put(OFFICE_SCOPE, OFFICE_SCOPE_DEFAULT_VALUES);
    	params.put(OFFICE_STATE, state);
    	params.put(OFFICE_REDIRECT_URI, redirectUri);
    	String authUrl = ENDPOINT_LOGIN + ENDPOINT_AUTHORIZE + Q_MARK + formUrlencodedString(params);
		log.debug("authUrl : {}", authUrl);
		return authUrl;
	}

	public boolean token(String userId, String code) {
		String prevToken = tokenCache.get(userId);
		if(prevToken != null) {
			log.debug("token : Reusing previous token " + prevToken);
			return true;
		}
		
		Map<String,String> params = new HashMap<>();
    	params.put(OFFICE_CLIENT_ID, clientId);
    	params.put(OFFICE_SCOPE, OFFICE_SCOPE_DEFAULT_VALUES);
   		params.put(OFFICE_CODE, code);
   		params.put(OFFICE_GRANT_TYPE, OFFICE_GRANT_TYPE_DEFAULT);
    	params.put(OFFICE_CLIENT_SECRET, clientSecret);
    	params.put(OFFICE_REDIRECT_URI, redirectUri);
		try {
			StringEntity entity = new StringEntity(formUrlencodedString(params));
			String postResponse = makePostCall(ENDPOINT_LOGIN + ENDPOINT_TOKEN, entity);
			log.debug(postResponse);
			OfficeToken ot = OBJECT_MAPPER.readValue(postResponse, OfficeToken.class);
			log.debug(ot.toString());
			if(ot == null){
				return false;
			}
			bearer = ot.getCurrentToken();
			OfficeUser ou = getCurrentDriveUser();
			ou.setSakaiUserId(userId);
			ou.setToken(ot.getCurrentToken());
			ou.setRefreshToken(ot.getRefreshToken());
			log.debug(ou.toString());
			ou = officeRepo.save(ou);
			if(ou != null){
				tokenCache.put(userId, ot.getCurrentToken());
				return true;
			}
		} catch(Exception e) {
			e.printStackTrace();
			log.warn("Office365: Error while retrieving or saving the token for user {} : {}", userId, e.getMessage());
		}
		return false;
	}

	public OfficeUser refreshToken(String userId){		
		OfficeUser officeUser = officeUserCache.get(userId);
		if(officeUser != null) {
			log.debug("refreshToken : Reusing previous user data " + officeUser);
			return officeUser;
		}
		
		OfficeUser ou = getOfficeUser(userId);
		if(ou == null){
			log.debug("No Office account found for user {}", userId);
			return null;
		}
		log.debug(ou.toString());
		Map<String,String> params = new HashMap<>();
    	params.put(OFFICE_CLIENT_ID, clientId);
    	params.put(OFFICE_SCOPE, OFFICE_SCOPE_DEFAULT_VALUES);
   		params.put(OFFICE_REFRESH_TOKEN, ou.getRefreshToken());
   		params.put(OFFICE_GRANT_TYPE, OFFICE_REFRESH_TOKEN);
    	params.put(OFFICE_CLIENT_SECRET, clientSecret);
    	params.put(OFFICE_REDIRECT_URI, redirectUri);
		try {
			StringEntity entity = new StringEntity(formUrlencodedString(params));
			String postResponse = makePostCall(ENDPOINT_LOGIN + ENDPOINT_TOKEN, entity);
			log.debug(postResponse);
			OfficeToken ot = OBJECT_MAPPER.readValue(postResponse, OfficeToken.class);
			log.debug(ot.toString());
			if(ot == null){
				return null;
			}
			bearer = ot.getCurrentToken();
			ou.setToken(ot.getCurrentToken());
			log.debug(ou.toString());
			officeRepo.update(ou);
			tokenCache.put(userId, ot.getCurrentToken());
			officeUserCache.put(userId, ou);
			return ou;
		} catch(Exception e) {
			e.printStackTrace();
			log.warn("Office365: Error while retrieving or saving the token for user {} : {}", userId, e.getMessage());
		}
		return null;
	}

	private OfficeUser getCurrentDriveUser(){
		OfficeUser ou = null;
		try {
			List<NameValuePair> params = new ArrayList<>();
			String getResponse = makeGetCall(ENDPOINT_GRAPH + ENDPOINT_ME, params);
			ou = OBJECT_MAPPER.readValue(getResponse, OfficeUser.class);
		} catch(Exception e) {
			log.error("getCurrentDriveUser: {}", e.getMessage());
		}
		return ou;
	}

	public OfficeUser getOfficeUser(String userId) {
		return officeRepo.findBySakaiId(userId);
	}

	public List<OfficeItem> getDriveRootItems(String userId) {
		List<OfficeItem> cachedItems = driveRootItemsCache.get(userId);
		if(cachedItems != null) {
			log.debug("getDriveRootItems : Returning cached items " + cachedItems);
			return cachedItems;
		}
		try {
			OfficeUser ou = refreshToken(userId);
			if(ou == null){
				return null;
			}
			List<NameValuePair> params = new ArrayList<>();
			bearer = ou.getToken();
			String getResponse = makeGetCall(ENDPOINT_GRAPH + ENDPOINT_DRIVES + ou.getOfficeUserId() + ENDPOINT_ROOT_CHILDREN, params);
			JsonNode jsonNode = OBJECT_MAPPER.readValue(getResponse, JsonNode.class);
			JsonNode valueNode = jsonNode.get(JSON_ENTRY_VALUE);
			if(valueNode == null) {
				log.warn("Couldn't retrieve root items for user id {} : response is {}", userId, getResponse);
				return null;
			}
			List<OfficeItem> items = OBJECT_MAPPER.readValue(valueNode.toString(), OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, OfficeItem.class));
			driveRootItemsCache.put(userId, items);
			return items;
		} catch(Exception e) {
			log.error("getDriveRootItems: id {} - error {}", userId, e.getMessage());
		}
		return null;
	}

	public List<OfficeItem> getDriveChildrenItems(String userId, String itemId, int depth) {
		String cacheId = userId + "#" + itemId;
		List<OfficeItem> cachedItems = driveChildrenItemsCache.get(cacheId);
		if(cachedItems != null) {
			log.debug("getDriveChildrenItems : Returning cached items " + cachedItems);
			return cachedItems;
		}
		try {
			OfficeUser ou = refreshToken(userId);
			if(ou == null){
				return null;
			}
			List<NameValuePair> params = new ArrayList<>();
			bearer = ou.getToken();
			String getResponse = makeGetCall(ENDPOINT_GRAPH + ENDPOINT_DRIVES + ou.getOfficeUserId() + ENDPOINT_ITEMS + itemId + ENDPOINT_CHILDREN, params);
			JsonNode jsonNode = OBJECT_MAPPER.readValue(getResponse, JsonNode.class);
			JsonNode valueNode = jsonNode.get(JSON_ENTRY_VALUE);
			if(valueNode == null) {
				log.warn("Couldn't retrieve children items for item id {} : response is {}", itemId, getResponse);
				return null;
			}
			List<OfficeItem> items = OBJECT_MAPPER.readValue(valueNode.toString(), OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, OfficeItem.class));
			items.forEach(it -> it.setDepth(depth+1));
			driveChildrenItemsCache.put(cacheId, items);
			return items;			
		} catch(Exception e) {
			log.error("getDriveChildrenItems: id {} - error {}", userId, e.getMessage());
		}
		return null;
	}

	private String formUrlencodedString(Map<String,String> params){
		StringBuilder param = new StringBuilder("");
    	for (Map.Entry<String, String> item : params.entrySet()) {
    	    if (param.toString().length() != 0) {
    	        param.append('&');
    	    }
    	    param.append(item.getKey());
    	    param.append('=');
    	    param.append(item.getValue().toString());
    	}
    	log.debug("formUrlencodedString : " + param.toString());
		return param.toString();
	}

	private boolean isConfigured(){
		if (StringUtils.isBlank(clientId) || StringUtils.isBlank(clientSecret) || StringUtils.isBlank(redirectUri)) {
			log.warn("OFFICE CONFIGURATION IS MISSING");
			return false;
		}
		return true;
	}

	public boolean revokeOfficeConfiguration(String userId){
		log.info("doRevokeOffice for user {}", userId);
		try {
			// delete office user ddbb entry
			officeRepo.delete(getOfficeUser(userId).getOfficeUserId());			
			cleanOfficeCacheForUser(userId);
			return true;
		} catch (Exception e) {
			log.warn("Error while trying to remove Office configuration : {}", e.getMessage());
		}
		return false;
	}

	public void cleanOfficeCacheForUser(String userId){
		log.debug("cleanOfficeCacheForUser {}", userId);
		// clean caches
		tokenCache.remove(userId);
		officeUserCache.remove(userId);
		driveRootItemsCache.remove(userId);
		/*driveChildrenItemsCache.forEach((k,v) -> {
			if(k.startsWith(userId+"#")) {
				System.out.println("Key: " + k + ": Value: " + v);
				driveChildrenItemsCache.remove(k);
			}
		});*/
		//driveChildrenItemsCache.entrySet().removeIf(e -> e.startsWith(userId+"#"));
		driveChildrenItemsCache.clear();
		// clean driveChildrenItemsCache where key starts with userId??
	}

	private String makePostCall(String endpoint, StringEntity body) throws Exception {
		if (!isConfigured()) {
            return null;
        }
        try {
            URIBuilder uriBuilder = new URIBuilder(endpoint);
            URI apiUri = uriBuilder.build();

	        HttpPost request = new HttpPost(apiUri);
	        request.addHeader("content-type", "application/x-www-form-urlencoded");
	        
			request.setEntity(body);
			
			// Configure request timeouts.
			RequestConfig requestConfig = RequestConfig.custom().build();
			
			request.setConfig(requestConfig);
			CloseableHttpResponse response = null;
			log.debug(request.toString());
			InputStream stream = null;
			CloseableHttpClient client = HttpClients.createDefault();
			try {
			    response = client.execute(request);
			    HttpEntity entity = response.getEntity();
			    if (entity != null) {
			        stream = entity.getContent();
			     	String streamString = IOUtils.toString(stream, "UTF-8");
			       	//System.out.println(streamString);
			       	return streamString;
		        }
		    } catch (Exception e) {
		    	log.warn("Could not fetch results from Office API." + e.getMessage());
		    } finally {
		        try {
		            response.close();
		            stream.close();
		        } catch (Exception e) {
		            log.warn("Error while closing HttpResponse." + e.getMessage());
		        }
		    }
		
		} catch (URISyntaxException e) {
			log.error("Incorrect Office API url syntax.", e);
		}
        return null;
	}
	
	private String makeGetCall(String endpoint, List<NameValuePair> params) throws Exception {
		if (!isConfigured()) {
            return null;
        }
		
        try {
        	URIBuilder uriBuilder = new URIBuilder(endpoint).addParameters(params);
            URI apiUri = uriBuilder.build();

            HttpGet request = new HttpGet(apiUri);
            request.addHeader("Authorization", "Bearer " + bearer);

            // Configure request timeouts.
            RequestConfig requestConfig = RequestConfig.custom().build();

            request.setConfig(requestConfig);
            CloseableHttpResponse response = null;
            log.debug(request.toString());
            InputStream stream = null;
            try {
            	CloseableHttpClient client = HttpClients.createDefault();
            	response = client.execute(request);
                HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        stream = entity.getContent();
                       	String streamString = IOUtils.toString(stream, "UTF-8");
                       	//System.out.println(streamString);
                       	return streamString;
                    }
                } catch (Exception e) {
                	log.warn("Could not fetch results from Office API." + e.getMessage());
                } finally {
                    try {
                        response.close();
                        stream.close();
                    } catch (Exception e) {
                        log.warn("Error while closing HttpResponse." + e.getMessage());
                    }
                }
            
        } catch (URISyntaxException e) {
            log.error("Incorrect Office API url syntax.", e);
        }

		return null;
	}

}