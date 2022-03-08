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

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.googledrive.repository.GoogleDriveUserRepository;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.UserDirectoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {GoogleDriveServiceImplTestConfiguration.class})
public class GoogleDriveServiceImplTest {

    @Autowired private UserDirectoryService userDirectoryService;
    @Autowired private MemoryService memoryService;
    @Autowired private SessionManager sessionManager;
    @Autowired private ServerConfigurationService serverConfigurationService;
    @Autowired private GoogleDriveUserRepository googledriveRepo;

    @Autowired
    GoogleDriveService googleDriveService;

    @Before
    public void setUp() {
    	Mockito.when(sessionManager.getCurrentSessionUserId()).thenReturn("james-bond");
    	Mockito.when(serverConfigurationService.getServerUrl()).thenReturn("https://localhost:8080");
    	System.out.println("weeeee je heeu hehueh uh h");
    }


    @Test
    public void test() {
        System.out.println("Meeeee " + googleDriveService.formAuthenticationUrl());
    }

}
