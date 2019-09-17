/******************************************************************************
 * Copyright 2015 sakaiproject.org Licensed under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.sakaiproject.coursedates.tool;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.sakaiproject.coursedates.api.SakaiProxy;
import org.sakaiproject.coursedates.api.model.CourseDatesError;
import org.sakaiproject.coursedates.api.model.CourseDatesValidation;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * MainController
 *
 * This is the controller used by Spring MVC to handle requests
 *
 */
@Slf4j
@Controller
public class MainController {

	@Inject SakaiProxy sakaiProxy;

	@RequestMapping(value = {"/", "/index"}, method = RequestMethod.GET)
	public String showIndex(@RequestParam(required=false) String code, Model model) {
		Locale locale = sakaiProxy.getUserLocale();
		String siteId = sakaiProxy.getCurrentSiteId();
		JSONArray assignmentsJson = sakaiProxy.getAssignmentsForContext(siteId);
		model.addAttribute("userCountry", locale.getCountry());
		model.addAttribute("userLanguage", locale.getLanguage());
		model.addAttribute("userLocale", locale.toString());
		model.addAttribute("assignments", assignmentsJson);
		return "index";
	}

	@RequestMapping(value = {"/date-manager/update", "/index"}, method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody String dateManagerUpdate(HttpServletRequest req, Model model) {
		String jsonResponse = "";
		try {
			String siteId = req.getRequestURI().split("/")[3];

			String jsonParam = req.getParameter("json");
			if (StringUtils.isBlank(jsonParam)) jsonParam = "[]";
			Object json = new JSONParser().parse(jsonParam);

			if (!(json instanceof JSONObject)) {
				throw new RuntimeException("Parse failed");
			}

			JSONArray assignments = (JSONArray) ((JSONObject) json).get("assignments");
			CourseDatesValidation assignmentValidate = sakaiProxy.validateAssignments(siteId, assignments);

			if (assignmentValidate.getErrors().isEmpty()) {
				sakaiProxy.updateAssignments(assignmentValidate);
				jsonResponse = "{\"status\": \"OK\"}";

			} else {
				JSONArray errorReport = new JSONArray();
				List<CourseDatesError> errors = new ArrayList<>();
				errors.addAll(assignmentValidate.getErrors());

				for (CourseDatesError error : errors) {
					JSONObject jsonError = new JSONObject();
					jsonError.put("field", error.field);
					jsonError.put("msg", error.msg);
					jsonError.put("toolId", error.toolId);
					jsonError.put("toolTitle", error.toolTitle);
					jsonError.put("idx", error.idx);
					errorReport.add(jsonError);
				}

				jsonResponse = String.format("{\"status\": \"ERROR\", \"errors\": %s}", errorReport.toJSONString());
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return jsonResponse;
	}
}
