package edu.berkeley.myberkeley.classpage.provide;

import com.google.common.collect.ImmutableMap;

import edu.berkeley.myberkeley.classpage.ClassPageProvisionServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class OracleClassPageContainerAttributeProvider extends AbstractClassAttributeProvider implements ClassAttributeProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(OracleClassPageContainerAttributeProvider.class);
  
  protected static final String SELECT_CLASS_SQL = "select * from BSPACE_COURSE_INFO_VW bsi " +
          "where bsi.TERM_YR = ? and bsi.TERM_CD = ? and bsi.COURSE_CNTL_NUM = ?";
  
  protected Map<String, String> ATTRIBUTE_TO_FIELD_MAP = ImmutableMap.of(
    "description", "CATALOG_DESCRIPTION" );
  
  protected OracleClassPageContainerAttributeProvider() {};
  
  public OracleClassPageContainerAttributeProvider(Connection connection) {
    super(connection);
  }
  
  @Override
  public List<Map<String, Object>> getAttributes(String classId) {
    LOGGER.debug("getting class page container attributes for: " + classId);
    List<Map<String, Object>> classPageContainerAttributes = null;;
    PreparedStatement preparedStatement = null;
    try {
      preparedStatement = connection.prepareStatement(SELECT_CLASS_SQL);
      try {
        long term = Long.parseLong(classId.substring(0, 4));
        preparedStatement.setLong(1, term);
      } catch (NumberFormatException e) {
        LOGGER.warn("coursId {} does not begin with a valid year number", classId);;
        return null;
      }
     
      preparedStatement.setString(2, classId.substring(4, 5));
      try {
        long ccn =  Long.valueOf(classId.substring(5));
        preparedStatement.setLong(3, ccn);
      } catch (NumberFormatException e) {
        LOGGER.warn("coursId {} does not end with a valid course control number", classId);
        return null;
      }
//      preparedStatement.
      ResultSet resultSet = preparedStatement.executeQuery();
      if (resultSet.next()) {
        classPageContainerAttributes = getClassPageHeaderAttributesFromResultSet(resultSet, classId, ATTRIBUTE_TO_FIELD_MAP);
      } else {
        classPageContainerAttributes = null;
      }
    } catch (SQLException e) {
      LOGGER.warn(e.getMessage(), e);
    } finally {
      if (preparedStatement != null) {
        try {
          preparedStatement.close();
        } catch (SQLException e) {
          LOGGER.warn(e.getMessage(), e);
        }
      }
    }
    LOGGER.debug("class page container attributes are: " + classPageContainerAttributes);
    return classPageContainerAttributes;
  }
//  TERM_YR
//  TERM_CD
//  COURSE_CNTL_NUM
//  DEPT_NAME
//  CATALOG_ID
//  CATALOG_PREFIX
//  CATALOG_ROOT
//  CATALOG_SUFFIX_1
//  CATALOG_SUFFIX_2
//  PRIMARY_SECONDARY_CD
//  SECTION_NUM
//  COURSE_TITLE
//  LOWER_RANGE_UNIT
//  UPPER_RANGE_UNIT
//  VARIABLE_UNIT_CD
//  FIXED_UNIT
//  INSTRUCTION_FORMAT
//  CRED_CD
//  ENROLL_LIMIT
//  CROSS_LISTED_FLAG
//  INSTR_FUNC
//  SCHEDULE_PRINT_CD
//  SECTION_CANCEL_FLAG
//  COURSE_TITLE_SHORT
//  CATALOG_DESCRIPTION
//  COURSE_OPTION
//  DEPT_DESCRIPTION
  
  protected List<Map<String, Object>> getClassPageHeaderAttributesFromResultSet(ResultSet resultSet, String classId, Map<String, String> attributeFieldMap) throws SQLException {
    List<Map<String, Object>> classPageContaimerAttributesList = new ArrayList<Map<String,Object>>();
    Map<String, Object> classPageContaimerAttributes = new HashMap<String, Object>();
    classPageContaimerAttributes.put(ClassPageProvisionServlet.PARAMS.classid.name(), classId);
    Set<Entry<String, String>> mapEntries = attributeFieldMap.entrySet();
    for (Entry<String, String> mapEntry : mapEntries) {
      classPageContaimerAttributes.put(mapEntry.getKey(), resultSet.getObject(mapEntry.getValue()));
    }
    classPageContaimerAttributesList.add(classPageContaimerAttributes);
    return classPageContaimerAttributesList;
  }

}
