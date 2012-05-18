package edu.berkeley.myberkeley.api.provision;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CourseAttributeProvider {
  public Map<String, Object> getCourseHeaderAttributes(String courseId);
  public List<Map<String, Object>> getInstructorAttributes(Set<String> instructorLdapIds);
  public Map<String, Object> getScheduleAttributes(String courseId);
  public List<Map<String, Object>> getSectionAttributes(Set<String> sectionCCNs);
}
