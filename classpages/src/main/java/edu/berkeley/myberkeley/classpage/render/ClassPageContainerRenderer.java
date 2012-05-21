package edu.berkeley.myberkeley.classpage.render;

import com.google.common.collect.ImmutableMap;


import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ClassPageContainerRenderer extends AbstractClassPageRenderer implements ClassPageRenderer { 
  
  private static final Logger LOGGER = LoggerFactory.getLogger(ClassPageContainerRenderer.class);
  
  static final Map<String, String> TOKENS_MAP = ImmutableMap.of(
      "classid", "%%CLASSID%%",
//      "info_last_updated", "%%LAST_UPDATE%%",
      "description", "%%DESCRIPTION%%"
  );

  private static final String TEMPLATE = "{" + 
  "  'classid' : %%CLASSID%%," + 
  "  'info_last_updated' : 'Mar 29, 2012'," + 
  "  'courseinfo' : {}," + 
  "  'description' : %%DESCRIPTION%%," + 
  "  'instructors' : []," + 
  "  'schedule' : {}," + 
  "  'sections' : []" + 
  "}";
  
  
  public ClassPageContainerRenderer(Repository repository, String template) {
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
