package edu.berkeley.myberkeley.classpage.render;


import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;


abstract class AbstractClassPageRenderer implements ClassPageRenderer{
  
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClassPageRenderer.class);
  
  protected Repository respository;
  
  // to allow passing in of template from a file later
  protected String template;
  
  protected AbstractClassPageRenderer(Repository repository, String template) {
    this.respository = repository;
    this.template = template;
  }
  

  protected JSONObject render(Map<String, Object> attributes, Map<String, String> tokensMap) throws JSONException {
    JSONObject renderedJSON = null;
    String workingTemplate = new String(this.template);
    Set<Entry<String, String>> tokenEntries = tokensMap.entrySet();
    for (Entry<String, String> tokenEntry : tokenEntries) {
      String key = tokenEntry.getKey();
      String token = tokenEntry.getValue();
      String value = (String) attributes.get(key);
      if (value == null) {
        LOGGER.warn("missing attribute: " + key);
      }
      workingTemplate = workingTemplate.replace(token, value);
      LOGGER.debug("rendered class page template::\n" + workingTemplate);
    }
    workingTemplate = falsifyUnusedTokens(workingTemplate);
    renderedJSON = new JSONObject(workingTemplate);
    return renderedJSON;
  }


  private String falsifyUnusedTokens(String workingTemplate) {
    String falsifiedTemplate = null;
    String regex = "%%.*%%";
    falsifiedTemplate = workingTemplate.replaceAll(regex, Boolean.FALSE.toString());
    return falsifiedTemplate;
  }
}
