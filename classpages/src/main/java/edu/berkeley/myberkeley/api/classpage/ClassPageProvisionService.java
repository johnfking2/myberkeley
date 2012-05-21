package edu.berkeley.myberkeley.api.classpage;


import org.apache.sling.commons.json.JSONObject;

public interface ClassPageProvisionService {
  public static final String CLASS_PAGE_PARAM_NAME = "classPage";
  public static final String CLASS_IDS_PARAM_NAME = "classids";
  public static final String CLASS_ID_PARAM_NAME = "classid";
  
  public JSONObject getClassPage(String classId);
  
  public ClassPageProvisionResult provisionClassPage(JSONObject classPage);
  
  public ClassPageProvisionResult provisionClassPage(String classId);
}
