package edu.berkeley.myberkeley.classpage.provide;

import java.util.List;
import java.util.Map;

/**
 *
 */
public interface ClassAttributeProvider {
  public List<Map<String, Object>> getAttributes(String classId);
}
