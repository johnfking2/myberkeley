package edu.berkeley.myberkeley.provision;

import edu.berkeley.myberkeley.api.provision.CourseInfoProvisionResult;
import edu.berkeley.myberkeley.api.provision.CourseInfoService;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that allows provision of course info from an external data source
 * (Oracle, currently), and retrieval of the class Info.
 * A GET request will return the provision JSON structure for the course.
 * A POST request will create or update the course.
 */
@SlingServlet(methods = { "GET", "POST" }, paths = {"/system/myberkeley/courseInfo"},
    generateService = true, generateComponent = true)

public class CourseInfoProvisionServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = -6006111536170097906L;
  
  private static final Logger LOGGER = LoggerFactory.getLogger(CourseInfoProvisionServlet.class);
  
  static final String CLASS_PAGE_PARAM_NAME = "classPage";
  static final String CLASS_IDS_PARAM_NAME = "classIds";
  static final String CLASS_ID_PARAM_NAME = "classId";

  @Reference
  transient CourseInfoService courseInfoService;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    PrintWriter writer = response.getWriter();
    // authorication check here
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    String classId = request.getParameter(CLASS_ID_PARAM_NAME);
    if (classId != null) {
      JSONObject courseInfo = this.courseInfoService.getCourseInfo(classId);
      if (courseInfo != null) {
        try {
          if (Arrays.asList(request.getRequestPathInfo().getSelectors()).contains("tidy")) {
            writer.print(courseInfo.toString(2));
          } else {
            writer.print(courseInfo.toString());
          }
        } catch (JSONException e) {
          LOGGER.warn(e.getMessage(), e);
          response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "failed to return courseInfo due to JSONException");
        }
      } else {
        writer.print("courseInfo not found for classId: " + classId);
      }
    } else {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing "
          + CLASS_ID_PARAM_NAME + " parameter");
    }
  }
  
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    // do the authn check here???
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    String courseInfoStr = request.getParameter(CLASS_PAGE_PARAM_NAME);
    String[] courseds = request.getParameterValues(CLASS_IDS_PARAM_NAME);
    if (courseInfoStr != null) {
      provisionCourseInfo(courseInfoStr, request, response);
    }
    else if (courseds != null) {
      provisionCourseInfo(courseds, request, response);
    }
    else {
      String message = "Missing " + CLASS_PAGE_PARAM_NAME + " or " + CLASS_IDS_PARAM_NAME + " parameter";
      LOGGER.warn(message);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
    }
    
  }

  private void provisionCourseInfo(String courseInfoStr, SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
    CourseInfoProvisionResult result = null;
    try {
      LOGGER.debug("provisioning:\n", courseInfoStr);
      JSONObject courseInfo = new JSONObject(courseInfoStr);
      result = this.courseInfoService.provisionCourseInfo(courseInfo);
      ExtendedJSONWriter jsonWriter = new ExtendedJSONWriter(response.getWriter());
      jsonWriter.setTidy(Arrays.asList(request.getRequestPathInfo().getSelectors()).contains("tidy"));
      jsonWriter.object();
      jsonWriter.key("synchronizationState");
      jsonWriter.value(result.getSynchronizationState().toString());
      jsonWriter.key(CLASS_ID_PARAM_NAME);
      jsonWriter.value(result.getClassId());
      jsonWriter.endObject();
    } catch (JSONException e) {
      LOGGER.warn(e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to provision: " + courseInfoStr + " due to JSON Parsing error");
    }
    
  }

  private void provisionCourseInfo(String[] classIds, SlingHttpServletRequest request,SlingHttpServletResponse response) throws IOException {
    CourseInfoProvisionResult result = null;
    for (int i = 0; i < classIds.length; i++) {
      result = this.courseInfoService.provisionCourseInfo(classIds[i]);
    }
  }
}
