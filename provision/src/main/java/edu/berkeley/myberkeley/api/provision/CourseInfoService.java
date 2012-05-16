package edu.berkeley.myberkeley.api.provision;

import org.apache.sling.commons.json.JSONObject;

public interface CourseInfoService {
  public JSONObject getCourseInfo(String courseId);
  
  public CourseInfoProvisionResult saveCourseInfo(JSONObject courseInfo);
}
