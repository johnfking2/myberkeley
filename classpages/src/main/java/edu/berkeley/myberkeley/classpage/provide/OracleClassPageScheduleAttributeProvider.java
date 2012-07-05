package edu.berkeley.myberkeley.classpage.provide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class OracleClassPageScheduleAttributeProvider extends
    OracleClassPageContainerAttributeProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(OracleClassPageScheduleAttributeProvider.class);

  public OracleClassPageScheduleAttributeProvider() {
  }

  public OracleClassPageScheduleAttributeProvider(Connection connection) {
    super(connection);
  }

  @Override
  protected void expandAttributes(Map<String, Object> attributes) {
    expandLocationAttributes(attributes);
    expandScheduleAttributes(attributes);
  }

}
