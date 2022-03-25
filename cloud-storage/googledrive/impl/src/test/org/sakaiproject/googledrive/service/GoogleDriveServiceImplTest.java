/**
 * Copyright (c) 2003-2022 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sakaiproject.googledrive.service;

import static org.mockito.Mockito.*;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.util.store.DataStore;
import com.google.api.services.drive.Drive;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.googledrive.repository.GoogleDriveUserRepository;
import org.sakaiproject.googledrive.model.GoogleDriveItem;
import org.sakaiproject.googledrive.model.GoogleDriveUser;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {GoogleDriveServiceImplTestConfiguration.class})
public class GoogleDriveServiceImplTest extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired private UserDirectoryService userDirectoryService;
    @Autowired private MemoryService memoryService;
    @Autowired private SessionManager sessionManager;
    @Autowired private ServerConfigurationService serverConfigurationService;
    private GoogleDriveUserRepository googledriveRepo;

    @Autowired private GoogleDriveService googleDriveService;

	private String userId;
	private Cache<String, Drive> googledriveUserCache;
	private Cache<String, List<GoogleDriveItem>> driveRootItemsCache;
	private Cache<String, List<GoogleDriveItem>> driveChildrenItemsCache;
	private Cache<String, GoogleDriveItem> driveItemsCache;
	
	private Map<String, GoogleAuthorizationCodeFlow> googleAuthorizationCodeFlowMap;
	private GoogleAuthorizationCodeFlow flow;
	
    @Before
    public void setUp() {
		flow = mock(GoogleAuthorizationCodeFlow.class);
		googleAuthorizationCodeFlowMap = new HashMap<>();

		GoogleAuthorizationCodeRequestUrl url = mock(GoogleAuthorizationCodeRequestUrl.class);
		url.setScheme("http");
		url.setHost("localhost");
		url.setPort(8080);
		ReflectionTestUtils.setField(url, "verbatim", true);
		ReflectionTestUtils.setField(url, "pathParts", List.of("/sakai-googledrive-tool"));//url.setPathParts(List.of("", "sakai-googledrive-tool"));

		when(flow.newAuthorizationUrl()).thenReturn(url);
		when(url.setRedirectUri(any())).thenReturn(url);
		when(url.setState(any())).thenReturn(url);

		googleAuthorizationCodeFlowMap.put("org", flow);
    	when(sessionManager.getCurrentSessionUserId()).thenReturn("james-bond");
    	when(serverConfigurationService.getServerUrl()).thenReturn("http://localhost:8080");
		ReflectionTestUtils.setField(googleDriveService, "googleAuthorizationCodeFlowMap", googleAuthorizationCodeFlowMap);
		ReflectionTestUtils.setField(googleDriveService, "redirectUri", serverConfigurationService.getServerUrl() + "/sakai-googledrive-tool");
 
 		userId = UUID.randomUUID().toString();
		try {
			flow.createAndStoreCredential(mock(GoogleTokenResponse.class), userId);
		} catch(Exception e){
			e.printStackTrace();
		}
		googledriveUserCache = mock(Cache.class);
		when(googledriveUserCache.get(userId)).thenReturn(mock(Drive.class));

		driveRootItemsCache = mock(Cache.class);
		driveChildrenItemsCache = mock(Cache.class);
		driveItemsCache = mock(Cache.class);
		ReflectionTestUtils.setField(googleDriveService, "googledriveUserCache", googledriveUserCache);
		ReflectionTestUtils.setField(googleDriveService, "driveRootItemsCache", driveRootItemsCache);
		ReflectionTestUtils.setField(googleDriveService, "driveChildrenItemsCache", driveChildrenItemsCache);
		ReflectionTestUtils.setField(googleDriveService, "driveItemsCache", driveItemsCache);
		
		googledriveRepo = mock(GoogleDriveUserRepository.class);
		ReflectionTestUtils.setField(googleDriveService, "googledriveRepo", googledriveRepo);
    }

    @Test
    public void testGoogleDriveServiceIsValid() {
        Assert.assertNotNull(googleDriveService);
    }

    @Test
    public void testGoogleDriveNotEnabledForUser() throws Exception {
		User u = mock(User.class);
		when(u.getEid()).thenReturn("eee@arg");
		when(userDirectoryService.getUser(any())).thenReturn(u);
        Assert.assertFalse(googleDriveService.isGoogleDriveEnabledForUser());
    }

    @Test
    public void testGoogleDriveEnabledForUser() throws Exception {
		User u = mock(User.class);
		when(u.getEid()).thenReturn("eee@org");
		when(userDirectoryService.getUser(any())).thenReturn(u);
        Assert.assertTrue(googleDriveService.isGoogleDriveEnabledForUser());
    }

    @Test
    public void testFormAuthenticationUrl() throws Exception {
		User u = mock(User.class);
		when(u.getEid()).thenReturn("eee@org");
		when(userDirectoryService.getUser(any())).thenReturn(u);
		String url = googleDriveService.formAuthenticationUrl();
        Assert.assertNotNull(url);
		Assert.assertEquals(url, serverConfigurationService.getServerUrl() + "/sakai-googledrive-tool");
    }

    @Test
    public void testGetGoogleDriveUser() throws Exception {
		GoogleDriveUser gdu = new GoogleDriveUser();
		gdu.setSakaiUserId(userId);
		Mockito.doReturn(gdu).when(googledriveRepo).findBySakaiId(userId);
		GoogleDriveUser gduReturned = googleDriveService.getGoogleDriveUser(userId);
        Assert.assertNotNull(gduReturned);
		Assert.assertEquals(gduReturned, gdu);
    }

    @Test
    public void testGetGoogleDriveUserNotFound() throws Exception {
		GoogleDriveUser gduReturned = googleDriveService.getGoogleDriveUser(userId);
        Assert.assertNull(gduReturned);
    }
	
    @Test
    public void testCleanGoogleDriveCacheForUser() throws Exception {		
		googleDriveService.cleanGoogleDriveCacheForUser(userId);

		verify(googledriveUserCache, times(1)).remove(userId);
		verify(driveRootItemsCache, times(1)).remove(userId);
		verify(driveChildrenItemsCache, times(1)).clear();
		verify(driveItemsCache, times(1)).clear();
    }

    @Test
    public void testRevokeGoogleDriveConfiguration() throws Exception {
		boolean result = googleDriveService.revokeGoogleDriveConfiguration(userId);

		verify(googledriveUserCache, times(1)).remove(userId);
		verify(driveRootItemsCache, times(1)).remove(userId);
		verify(driveChildrenItemsCache, times(1)).clear();
		verify(driveItemsCache, times(1)).clear();
		
		Assert.assertFalse(result);//TODO
    }

}
