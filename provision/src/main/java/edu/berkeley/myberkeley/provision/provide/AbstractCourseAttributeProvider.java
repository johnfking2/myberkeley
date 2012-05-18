package edu.berkeley.myberkeley.provision.provide;

import edu.berkeley.myberkeley.api.provision.JdbcConnectionService;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;

import java.util.List;
import java.util.Map;

public abstract class AbstractCourseAttributeProvider implements CourseAttributeProvider {


  @Reference
 protected JdbcConnectionService jdbcConnectionService;

  public abstract List<Map<String, Object>> getAttributes(String classId);
}
