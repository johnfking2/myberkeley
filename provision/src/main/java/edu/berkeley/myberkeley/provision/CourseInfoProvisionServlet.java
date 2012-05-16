package edu.berkeley.myberkeley.provision;

import edu.berkeley.myberkeley.api.provision.CourseInfoProvisionResult;
import edu.berkeley.myberkeley.api.provision.CourseInfoService;
import edu.berkeley.myberkeley.api.provision.SynchronizationState;

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
  
  static final String COURSE_INFO_PARAM_NAME = "ccourseInfo";
  static final String COURSE_IDS_PARAM_NAME = "courseIds";
  static final String COURSE_ID_PARAM_NAME = "courseId";

  @Reference
  transient CourseInfoService courseInfoService;

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    // do the authn check here???
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    String courseInfoStr = request.getParameter(COURSE_INFO_PARAM_NAME);
    String[] courseds = request.getParameterValues(COURSE_IDS_PARAM_NAME);
    if (courseInfoStr != null) {
      provisionCourseInfo(courseInfoStr, request, response);
    }
    else if (courseds !=null) {
      provisionCourseInfo(courseds, request, response);
    }
    else {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing " + COURSE_INFO_PARAM_NAME + " or " + COURSE_IDS_PARAM_NAME + " parameter");
    }
    
  }

  private void provisionCourseInfo(String courseInfoStr, SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
    CourseInfoProvisionResult result = null;
    try {
      JSONObject courseInfo = new JSONObject(courseInfoStr);
      result = this.courseInfoService.saveCourseInfo(courseInfo);
      ExtendedJSONWriter jsonWriter = new ExtendedJSONWriter(response.getWriter());
      jsonWriter.setTidy(Arrays.asList(request.getRequestPathInfo().getSelectors()).contains("tidy"));
      jsonWriter.object();
      jsonWriter.key("synchronizationState");
      jsonWriter.value(result.getSynchronizationState().toString());
      jsonWriter.key("courseId");
      jsonWriter.value(result.getCourseId());
      jsonWriter.endObject();
    } catch (JSONException e) {
      LOGGER.warn(e.getMessage(), e);
      result = new CourseInfoProvisionResult(null, SynchronizationState.error);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to provision: " + courseInfoStr + " due to JSON Parsing error");
    }
    
  }

  private void provisionCourseInfo(String[] courseIds, SlingHttpServletRequest request,SlingHttpServletResponse response) throws IOException {
    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"provisionCourseInfo() not implemented yet");
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    // authorication check here
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    String courseId = request.getParameter(COURSE_ID_PARAM_NAME);
    if (courseId != null) {
      JSONObject courseInfo = this.courseInfoService.getCourseInfo(courseId);
      if (courseInfo != null) {
        PrintWriter writer = response.getWriter();
        if ( Arrays.asList(request.getRequestPathInfo().getSelectors()).contains("tidy") ) {
          writer.print(courseInfo.toString(2));
        }
        else{
          writer.print(courseInfo.toString());        
        }
      }
      else {
        // handle error
      }
    }
    else {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing " + COURSE_ID_PARAM_NAME +" parameter");
    }
    }
    
  }
  
  
}
