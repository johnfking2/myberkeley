package edu.berkeley.myberkeley.provision.provide;

import java.util.List;
import java.util.Map;

/**
 *
 */
public interface ClassAttributeProvider {
  /**
   * 
   * @param classId
   * @return
   */

  public List<Map<String, Object>> getAttributes(String classId);
  
  
}
