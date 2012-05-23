package edu.berkeley.myberkeley.classpage.render;

import com.google.common.collect.ImmutableMap;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ClassPageCourseInfoRenderer extends AbstractClassPageRenderer implements
    ClassPageRenderer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClassPageContainerRenderer.class);
  
  protected static final Map<String, String> TOKENS_MAP = ImmutableMap.of(
      "title", "%%TITLE%%",
      "term", "%%TERM%%"
      
  );
  
  private static final String TEMPLATE = "{" +
  "  'title' : %%TITLE%%'" +
  "  'format' : %%FORMAT%%,'" +
  "  'grading' : %%GRADING%%,'" +
  "  'prereqs' : %%PREREQS%%,'" +
  "  'requirements' : %%REQUIREMENTS%%,'" +
  "  'term' : %%TERM%%,'" +
  "  'semesters_offered' : %%SEMESTERS_OFFERED%%,'" +
  "  'year' : %%YEAR%%,'" +
  "  'department' : %%DEPARTMENT%%,'" +
  "  'coursenum' : %%COURSENUM%%,'" +
  "  'units' : %%UNITS%%'" +
  "}";
  
  public ClassPageCourseInfoRenderer(Repository repository, String template) {
    super(repository, template);
    if (this.template == null) this.template = TEMPLATE;
  }

  @Override
  public JSONObject render(Map<String, Object> attributes) throws JSONException {
    LOGGER.debug("rendering class page container JSON");
    return super.render(attributes, TOKENS_MAP);
  }

}
