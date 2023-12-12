/**
 * Copyright (c) 2003-2017 The Apereo Foundation
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
package org.sakaiproject.gradebookng.tool.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.gradebookng.business.GbRole;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.exception.GbAccessDeniedException;
import org.sakaiproject.gradebookng.business.model.GbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.util.GbStopWatch;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.CategoryDefinition;
import org.sakaiproject.service.gradebook.shared.GradebookInformation;
import org.sakaiproject.service.gradebook.shared.SortType;
import org.sakaiproject.rubrics.api.RubricsConstants;
import org.sakaiproject.rubrics.api.RubricsService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.tool.gradebook.Gradebook;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode @Getter
public class GbGradeTableData {
	private List<Assignment> assignments;
	private List<GbStudentGradeInfo> grades;
	private List<CategoryDefinition> categories;
	private GradebookInformation gradebookInformation;
	private GradebookUiSettings uiSettings;
	private GbRole role;
	private Map<String, String> toolNameToIconCSS;
	private String defaultIconCSS;
	private boolean isUserAbleToEditAssessments;
	private Map<String, Double> courseGradeMap;
	private Map<String, Boolean> hasAssociatedRubricMap;
	private boolean isStudentNumberVisible;
	private boolean isSectionsVisible;
	private String gradebookUid;

	public GbGradeTableData(final String currentGradebookUid, final String currentSiteId, final GradebookNgBusinessService businessService,
			final GradebookUiSettings settings, final ToolManager toolManager, final RubricsService rubricsService) {
		final GbStopWatch stopwatch = new GbStopWatch();
		stopwatch.time("GbGradeTableData init", stopwatch.getTime());

		this.gradebookUid = currentGradebookUid;
		uiSettings = settings;

		SortType sortBy = SortType.SORT_BY_SORTING;
		if (settings.isCategoriesEnabled() && settings.isGroupedByCategory()) {
			// Pre-sort assignments by the categorized sort order
			sortBy = SortType.SORT_BY_CATEGORY;
		}

		try {
			role = businessService.getUserRole(currentSiteId);
		} catch (GbAccessDeniedException e) {
			throw new RuntimeException(e);
		}

		isUserAbleToEditAssessments = businessService.isUserAbleToEditAssessments(currentSiteId);
		assignments = businessService.getGradebookAssignments(currentGradebookUid, currentSiteId, sortBy);
		assignments.stream()
			.filter(assignment -> assignment.isExternallyMaintained())
			.forEach(assignment -> assignment.setExternalToolTitle(businessService.getExternalAppName(assignment.getExternalAppName()))
		);
		stopwatch.time("getGradebookAssignments", stopwatch.getTime());

		grades = businessService.buildGradeMatrix(currentGradebookUid, currentSiteId, 
				assignments,
				businessService.getGradeableUsers(currentGradebookUid, currentSiteId, uiSettings.getGroupFilter()),
				settings);
		stopwatch.time("buildGradeMatrix", stopwatch.getTime());

		categories = businessService.getGradebookCategories(currentGradebookUid, currentSiteId);
		stopwatch.time("getGradebookCategories", stopwatch.getTime());

		gradebookInformation = businessService.getGradebookSettings(currentGradebookUid, currentSiteId);
		stopwatch.time("getGradebookSettings", stopwatch.getTime());

		toolNameToIconCSS = businessService.getIconClassMap();
		defaultIconCSS = businessService.getDefaultIconClass();
		stopwatch.time("toolNameToIconCSS", stopwatch.getTime());

		final Gradebook gradebook = businessService.getGradebook(currentGradebookUid, currentSiteId);
		courseGradeMap = gradebook.getSelectedGradeMapping().getGradeMap();

		hasAssociatedRubricMap = buildHasAssociatedRubricMap(assignments, toolManager, rubricsService);

		isStudentNumberVisible = businessService.isStudentNumberVisible(currentSiteId);

		isSectionsVisible = businessService.isSectionsVisible(currentSiteId);

	}

	public HashMap<String, Boolean> buildHasAssociatedRubricMap(final List<Assignment> assignments, final ToolManager toolManager, final RubricsService rubricsService) {
		HashMap<String, Boolean> map = new HashMap<String, Boolean>();
		for (Assignment assignment : assignments) {
			String externalAppName = assignment.getExternalAppName();
			if(assignment.isExternallyMaintained()) {
				boolean hasAssociatedRubric = StringUtils.equals(externalAppName, toolManager.getLocalizedToolProperty("sakai.assignment", "title")) ? rubricsService.hasAssociatedRubric(externalAppName, assignment.getExternalId()) : false;
				map.put(assignment.getExternalId(), hasAssociatedRubric);
			} else {
				Long assignmentId = assignment.getId();
				boolean hasAssociatedRubric = rubricsService.hasAssociatedRubric(RubricsConstants.RBCS_TOOL_GRADEBOOKNG, assignmentId.toString());
				map.put(assignmentId.toString(), hasAssociatedRubric);
			}
		}
		return map;
	}
}
