package edu.berkeley.myberkeley.provision;

import edu.berkeley.myberkeley.api.provision.CourseAttributeProvider;
import edu.berkeley.myberkeley.api.provision.JdbcConnectionService;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component(label = "CalCentral :: Oracle Course Attribute Provider", description = "Provide CalCentral course attributes from Oracle connection")
@Service
public class OracleCourseAttributeProvider implements CourseAttributeProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(OracleCourseAttributeProvider.class);
  
  public static final String SELECT_COURSE_SQL = "select * from BSPACE_COURSE_INFO_VW bsi" +
          "where bsi.TERM_YR = ? and bsi.TERm_CD = ? and bsi.COURSE_CNTL_NUM = ?";
  @Reference
  JdbcConnectionService jdbcConnectionService;
  
  @Override
  public Map<String, Object> getCourseHeaderAttributes(String courseId) {
    Map<String, Object> courseHeaderAttributes = null;;
    Connection connection = null;
    PreparedStatement preparedStatement = null;
    try {
      connection = jdbcConnectionService.getConnection();
      preparedStatement = connection.prepareStatement(SELECT_COURSE_SQL);
      try {
        long term = Long.parseLong(courseId.substring(0, 3));
        preparedStatement.setLong(1, term);
      } catch (NumberFormatException e) {
        LOGGER.warn("coursId {} does not begin with a valid year number", courseId);;
        return null;
      }
     
      preparedStatement.setString(2, courseId.substring(4, 5));
      try {
        long ccn =  Long.valueOf(courseId.substring(5));
        preparedStatement.setLong(3, ccn);
      } catch (NumberFormatException e) {
        LOGGER.warn("coursId {} does not end with a valid course control number", courseId);;
        return null;
      }
      
      ResultSet resultSet = preparedStatement.executeQuery();
      if (resultSet.next()) {
        courseHeaderAttributes = getCourseHeaderAttributesFromResultSet(resultSet, courseId);
      } else {
        courseHeaderAttributes = null;
      }
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public List<Map<String, Object>> getInstructorAttributes(Set<String> instructorLdapIds) {
    return null;
  }

  @Override
  public Map<String, Object> getScheduleAttributes(String courseId) {
    return null;
  }

  @Override
  public List<Map<String, Object>> getSectionAttributes(Set<String> sectionCCNs) {
    return null;
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
  private Map<String, Object> getCourseHeaderAttributesFromResultSet(ResultSet resultSet, String courseId) {
    Map<String, Object> courseHeaderAttributes = new HashMap<String, Object>();
    courseHeaderAttributes.put("courseid", courseId);
    return courseHeaderAttributes;
  }
}
