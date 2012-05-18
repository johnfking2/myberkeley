package edu.berkeley.myberkeley.api.provision;

import org.apache.sling.commons.json.JSONObject;

public interface CourseInfoService {
  public JSONObject getCourseInfo(String classId);
  
  public CourseInfoProvisionResult provisionCourseInfo(JSONObject classPage);
  
  public CourseInfoProvisionResult provisionCourseInfo(String classId);
}
