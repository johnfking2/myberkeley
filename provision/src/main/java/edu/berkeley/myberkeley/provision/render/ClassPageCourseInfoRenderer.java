package edu.berkeley.myberkeley.provision.render;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.lite.Repository;

import java.util.Map;

public class ClassPageCourseInfoRenderer extends AbstractClassPageRenderer implements
    ClassPageRenderer {

  private static final String TEMPLATE = "{" + 
  "  'format' : '3 hours of lecture per week'," + 
  "  'grading' : 'Must be taken on a satisfactory / unsatisfactory basis'," + 
  "  'prereqs' : '1 or equivalent'," + 
  "  'requirements' : 'American Cultures'," + 
  "  'term' : 'Spring'," + 
  "  'semesters_offered' : 'Spring, Fall'," + 
  "  'year' : 2012," + 
  "  'department' : 'Ethnic Studies'," + 
  "  'coursenum' : '21AC'," + 
  "  'units' : 5" + 
  "}";
  
  public ClassPageCourseInfoRenderer(Repository repository, String template) {
    super(repository, template);
    if (this.template == null) this.template = TEMPLATE;
  }

  @Override
  public JSONObject render(Map<String, Object> attributeMap) throws JSONException {
    
    return new JSONObject(this.template);
  }

}
