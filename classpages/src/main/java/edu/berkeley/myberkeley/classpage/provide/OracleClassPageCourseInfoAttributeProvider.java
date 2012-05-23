package edu.berkeley.myberkeley.classpage.provide;

import com.google.common.collect.ImmutableMap;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * subclass because it uses the same query as superclass, just different fields for attributes
 */
public class OracleClassPageCourseInfoAttributeProvider extends
    OracleClassPageContainerAttributeProvider {

  
  protected Map<String, String> ATTRIBUTE_TO_FIELD_MAP = ImmutableMap.of(
      "title", "COURSE_TITLE",
      "term", "TERM_CD",
      "year", "TERM_YR",
      "department", "DEPT_NAME",
      "coursenum", "COURSE_CNTL_NUM");
  
//TERM_YR
//TERM_CD
//COURSE_CNTL_NUM
//DEPT_NAME
//CATALOG_ID
//CATALOG_PREFIX
//CATALOG_ROOT
//CATALOG_SUFFIX_1
//CATALOG_SUFFIX_2
//PRIMARY_SECONDARY_CD
//SECTION_NUM
//COURSE_TITLE
//LOWER_RANGE_UNIT
//UPPER_RANGE_UNIT
//VARIABLE_UNIT_CD
//FIXED_UNIT
//INSTRUCTION_FORMAT
//CRED_CD
//ENROLL_LIMIT
//CROSS_LISTED_FLAG
//INSTR_FUNC
//SCHEDULE_PRINT_CD
//SECTION_CANCEL_FLAG
//COURSE_TITLE_SHORT
//CATALOG_DESCRIPTION
//COURSE_OPTION
//DEPT_DESCRIPTION
  
  private OracleClassPageCourseInfoAttributeProvider() {
  }

  public OracleClassPageCourseInfoAttributeProvider(Connection connection) {
    super(connection);
  }

  
  @Override
  protected List<Map<String, Object>> getClassPageHeaderAttributesFromResultSet(
      ResultSet resultSet, String classId, Map<String, String> attributeFieldMap)
      throws SQLException {

    return super.getClassPageHeaderAttributesFromResultSet(resultSet, classId,
        ATTRIBUTE_TO_FIELD_MAP);
  }

  
}
