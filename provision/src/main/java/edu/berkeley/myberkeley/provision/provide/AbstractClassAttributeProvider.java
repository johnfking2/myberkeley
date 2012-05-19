package edu.berkeley.myberkeley.provision.provide;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public abstract class AbstractClassAttributeProvider implements ClassAttributeProvider {


  protected Connection connection;
  
  protected AbstractClassAttributeProvider() {};
  
  public AbstractClassAttributeProvider(Connection connection) {
    this.connection = connection;
  }

  public abstract List<Map<String, Object>> getAttributes(String classId);
}
