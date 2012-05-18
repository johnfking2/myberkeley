package edu.berkeley.myberkeley.provision.render;

import com.google.common.collect.ImmutableMap;


import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class CourseInfoHeaderRenderer extends AbstractCourseInfoRenderer implements CourseInfoRenderer { 
  
  static final Map<String, String> TOKENS_MAP = ImmutableMap.of(
      "classid", "%%CLASSID%%",
//      "info_last_updated", "%%LAST_UPDATE%%",
      "description", "%%DESCRIPTION%%"
  );

  private static final String TEMPLATE = "{" +
  "  \"classid\" : %%CLASSID%%" +
  "  \"info_last_updated\" : %%LAST_UPDATE%%," +
  "  \"courseinfo\" : {" +
  "    \"format\" : \"3 hours of lecture per week\"," +
  "    \"grading\" : \"Must be taken on a satisfactory / unsatisfactory basis\"," +
  "    \"prereqs\" : \"1 or equivalent\"," +
  "    \"requirements\" : \"American Cultures\"," +
  "    \"term\" : \"Spring\"," +
  "    \"semesters_offered\" : \"Spring, Fall\"," +
  "    \"year\" : 2012," +
  "    \"department\" : %%DEPARTMENT%%," +
  "    \"coursenum\" : \"21AC\"," +
  "    \"units\" : 5" +
  "  }," +
  "  \"description\" : %%DESCRIPTION%%," +
  "}";
  
  private static final Logger LOGGER = LoggerFactory.getLogger(CourseInfoHeaderRenderer.class);
  
  public CourseInfoHeaderRenderer(Repository repository, String template) {
    super(repository, template);
    if (this.template == null) this.template = TEMPLATE;
  }

  @Override
  public JSONObject render(Map<String, Object> attributes) throws JSONException {
    JSONObject renderedJSON = null;
    String workingTemplate = new String(this.template);
    Set<Entry<String, String>> tokenEntries = TOKENS_MAP.entrySet();
    for (Entry<String, String> tokenEntry : tokenEntries) {
      String key = tokenEntry.getKey();
      String token = tokenEntry.getValue();
      String value = (String) attributes.get(key);
      if (value == null) {
        LOGGER.warn("missing attribute: " + key);
      }
      workingTemplate.replace(token, value);
    }
    renderedJSON = new JSONObject(workingTemplate);
    return renderedJSON;
  }
}
