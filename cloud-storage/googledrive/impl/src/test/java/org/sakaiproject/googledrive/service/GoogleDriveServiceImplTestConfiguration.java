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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.googledrive.repository.GoogleDriveUserRepository;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.UserDirectoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleDriveServiceImplTestConfiguration {

    @Bean(name = "org.sakaiproject.memory.api.MemoryService")
    public MemoryService memoryService() {
        return mock(MemoryService.class);
    }

    @Bean(name = "org.sakaiproject.user.api.UserDirectoryService")
    public UserDirectoryService userDirectoryService() {
        return mock(UserDirectoryService.class);
    }

    @Bean(name = "org.sakaiproject.component.api.ServerConfigurationService")
    public ServerConfigurationService serverConfigurationService() {
    	ServerConfigurationService serverConfigurationService = mock(ServerConfigurationService.class);
    	Mockito.when(serverConfigurationService.getServerUrl()).thenReturn("https://localhost:8080");
    	return serverConfigurationService;
    }

    @Bean(name = "org.sakaiproject.tool.api.SessionManager")
    public SessionManager sessionManager() {
    	SessionManager sessionManager = mock(SessionManager.class);
    	Mockito.when(sessionManager.getCurrentSessionUserId()).thenReturn("james-bond");
    	return sessionManager;
    }

    @Bean(name = "org.sakaiproject.googledrive.repository.GoogleDriveUserRepository")
    public GoogleDriveUserRepository googledriveRepo() {
        return mock(GoogleDriveUserRepository.class);
    }

    @Bean
    public GoogleDriveService googleDriveService() {
        return new GoogleDriveServiceImpl();
    }

}
