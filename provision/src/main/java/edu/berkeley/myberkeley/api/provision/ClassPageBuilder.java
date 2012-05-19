package edu.berkeley.myberkeley.api.provision;

import org.apache.sling.commons.json.JSONObject;

public interface ClassPageBuilder {
  
  public enum Section {
    container,
    courseinfo,
    instructors,
    schedule,
    sections
  }
  
  public ClassPageBuilder begin(String classId);
  
  public ClassPageBuilder insert(Section section);
  
  public JSONObject end(); 
  
}
