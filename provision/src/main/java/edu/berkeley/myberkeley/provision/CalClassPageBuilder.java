package edu.berkeley.myberkeley.provision;

import edu.berkeley.myberkeley.api.provision.ClassPageBuilder;
import edu.berkeley.myberkeley.api.provision.JdbcConnectionService;
import edu.berkeley.myberkeley.provision.provide.ClassAttributeProvider;
import edu.berkeley.myberkeley.provision.provide.OracleClassPageContainerAttributeProvider;
import edu.berkeley.myberkeley.provision.render.ClassPageContainerRenderer;
import edu.berkeley.myberkeley.provision.render.ClassPageCourseInfoRenderer;
import edu.berkeley.myberkeley.provision.render.ClassPageRenderer;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(metatype = true,
    label = "CalCentral :: Class Page Builder", description = "Builds a JSON Object that represents a class page")
@Service
public class CalClassPageBuilder implements ClassPageBuilder {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(CalClassPageBuilder.class);
  
  private String classId;
  
  private JSONObject classPage;
  
  private Map<Section, ClassAttributeProvider> attributeProviders;
  
  private Map<Section, ClassPageRenderer> renderers;
  
  @Reference
  Repository repository;
  
  @Reference
  JdbcConnectionService jdbcConnectionService;
  
  Connection dbConnection;
  
  @Override
  public ClassPageBuilder begin(String classId) {
    this.classId = classId;
    ClassAttributeProvider attributeProvider = this.attributeProviders.get(Section.container);
    ClassPageRenderer renderer = this.renderers.get(Section.container);
    List<Map<String, Object>> attributes = attributeProvider.getAttributes(this.classId);
    JSONObject container = null;
    try {
      container = renderer.render(attributes.get(0));
    } catch (JSONException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    this.classPage = container;
    return this;
  }

  @Override
  public ClassPageBuilder insert(Section section) {
    JSONObject renderedSection = null;
    ClassAttributeProvider attributeProvider = this.attributeProviders.get(section);
    ClassPageRenderer renderer = this.renderers.get(section);
    List<Map<String, Object>> attributesList = attributeProvider.getAttributes(this.classId);
    if (attributesList.size() == 1) {
      try {
        renderedSection = renderer.render(attributesList.get(0));
        this.classPage.put(section.name(), renderedSection);
      } catch (JSONException e) {
        LOGGER.warn(e.getMessage(), e);
      }
    } else {
//      handle the tuples
    }
;    return this;
  }

  @Override
  public JSONObject end() {
    JSONObject returnCopy = null;
    try {
      returnCopy = new JSONObject(this.classPage.toString());
    } catch (JSONException e) {
      LOGGER.warn(e.getMessage(), e);
    } finally {
      this.classId = null;
      this.classPage = null;
    }
    return returnCopy;
  }

  @Activate @Modified
  protected void activate(ComponentContext componentContext) {
    // wire up later to allow pipeline configurataion and template passing??
    
    this.classId = null;
    this.classPage = null;
    
    Dictionary<?, ?> props = componentContext.getProperties();
    
    try {
      this.dbConnection = this.jdbcConnectionService.getConnection();
    } catch (SQLException e) {
      LOGGER.warn(e.getMessage(), e);
      deactivate(componentContext);
    }
    // cab't get an ImmutableMap to handle types so using plain HashMaqp    
    this.attributeProviders = new HashMap<Section, ClassAttributeProvider>();
    this.attributeProviders.put(Section.container, new OracleClassPageContainerAttributeProvider(this.dbConnection));

    this.renderers = new HashMap<Section, ClassPageRenderer>();
    this.renderers.put(Section.container, new ClassPageContainerRenderer(repository, null));
    this.renderers.put(Section.courseinfo, new ClassPageCourseInfoRenderer(repository, null));
      
  }
  
  @SuppressWarnings("deprecation")
  @Deactivate
  protected void deactivate(ComponentContext componentContext) {
    if (this.dbConnection != null) {
      try {
        this.dbConnection.close();
      } catch (SQLException e) {
        LOGGER.info("Exception while closing dbConnection", e);
        throw new ComponentException("Could not close connection");
      } finally {
        this.dbConnection = null; 
        this.jdbcConnectionService = null;
      }
    }
  }
}
