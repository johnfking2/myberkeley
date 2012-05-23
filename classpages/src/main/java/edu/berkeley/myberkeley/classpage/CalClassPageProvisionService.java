package edu.berkeley.myberkeley.classpage;

import edu.berkeley.myberkeley.api.classpage.ClassPageProvisionResult;
import edu.berkeley.myberkeley.api.classpage.ClassPageProvisionService;
import edu.berkeley.myberkeley.api.provision.JdbcConnectionService;
import edu.berkeley.myberkeley.api.provision.SynchronizationState;
import edu.berkeley.myberkeley.classpage.provide.ClassAttributeProvider;
import edu.berkeley.myberkeley.classpage.provide.OracleClassPageContainerAttributeProvider;
import edu.berkeley.myberkeley.classpage.provide.OracleClassPageCourseInfoAttributeProvider;
import edu.berkeley.myberkeley.classpage.render.ClassPageContainerRenderer;
import edu.berkeley.myberkeley.classpage.render.ClassPageCourseInfoRenderer;
import edu.berkeley.myberkeley.classpage.render.ClassPageRenderer;

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
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(metatype = true,
    label = "CalCentral :: Class Page Provision Service", description = "Create & Store Class Page Data")
@Service
public class CalClassPageProvisionService implements ClassPageProvisionService {

  public static final String STORE_NAME = "_myberkeley_classpage";
  public static final String STORE_RESOURCETYPE = "myberkeley/c";
  public static final String CLASS_PAGE_PROP_NAME = "classPzge";
  
  public enum Part {
    container,
    courseinfo,
    instructors,
    schedule,
    sections
  }
  
  private static final Logger LOGGER = LoggerFactory.getLogger(CalClassPageProvisionService.class);
  
  Map<Part, ClassAttributeProvider> attributeProviders;
  
  Map<Part, ClassPageRenderer> renderers;
  
  @Reference
  Repository repository;
  
  @Reference
  JdbcConnectionService jdbcConnectionService;
  
  Connection connection;
  
  @Override
  public JSONObject getClassPage(String classId) {
    JSONObject classPage = null;
    Session adminSession = null;
    try {
      adminSession = repository.login();
      ContentManager cm = adminSession.getContentManager();
      Content classPageContent = cm.get(STORE_NAME + "/" + classId);
      if (classPageContent != null) {
        String classPageStr = (String) classPageContent.getProperty(CLASS_PAGE_PROP_NAME);
        classPage = new JSONObject(classPageStr);
      } else {
        LOGGER.warn("No classPage data found for classId: " + classId);
      }
    } catch (ClientPoolException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (StorageClientException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (JSONException e) {
      LOGGER.warn(e.getMessage(), e);
    } finally {
      if (adminSession != null) {
        try {
          adminSession.logout();
        } catch (ClientPoolException e) {
          LOGGER.warn(e.getMessage(), e);
        }
      }
    }
    return classPage;
  }

  @Override
  public ClassPageProvisionResult provisionClassPage(JSONObject classPageJson) {
    Session adminSession = null;
    SynchronizationState synchronizationState = SynchronizationState.error;
    Content classPageContent = null;
    String classId = null;
    String courseTitle = null;
    Map<String, Object> classPageContentMap = null;
    try {
      adminSession = repository.loginAdministrative();
      ContentManager cm = adminSession.getContentManager();
      classId = classPageJson.getString("classid");
      if (classId == null) {
        throw new JSONException("classid key missing");
      }
      String path = STORE_NAME + "/" + classId;
      JSONObject courseInfo = classPageJson.getJSONObject(Part.courseinfo.name());
      courseTitle = courseInfo != null ? courseInfo.getString("title") : "NO TITLE";
      String stringifiedClassPage = classPageJson.toString();
      classPageContent = cm.get(path);
      if (classPageContent == null) {
        classPageContentMap = new HashMap<String, Object>(2);
        classPageContentMap.put("sling:resourceType", STORE_RESOURCETYPE);
        classPageContentMap.put(CLASS_PAGE_PROP_NAME, stringifiedClassPage);
        classPageContent = new Content(path, classPageContentMap);
        cm.update(classPageContent);
        synchronizationState = SynchronizationState.created;
      }
      else {
        classPageContent.setProperty(CLASS_PAGE_PROP_NAME, stringifiedClassPage);
        cm.update(classPageContent);
        synchronizationState = SynchronizationState.refreshed;
      }
    } catch (ClientPoolException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (StorageClientException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (JSONException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    finally {
      if (adminSession != null) {
        try {
          adminSession.logout();
        } catch (ClientPoolException e) {
          LOGGER.warn(e.getMessage(), e);
        }
      }
    }
    return new ClassPageProvisionResult(classId, courseTitle, synchronizationState);
  }

  @Override
  public ClassPageProvisionResult provisionClassPage(String classId) {
    ClassPageProvisionResult result = null;
    Content classPageContent = null;
    Map<String, Object> classPageContentMap = null;
    String stringifiedClassPage = null;
    String courseTitle = null;
    JSONObject courseInfo = null;
    Session adminSession = null;
    SynchronizationState synchronizationState = SynchronizationState.error;
    LOGGER.info("Provisioning class: " + classId);
    try {
      adminSession = repository.loginAdministrative();
      ContentManager cm = adminSession.getContentManager();
      String path = STORE_NAME + "/" + classId;
      LOGGER.info("Storing classPage at: " + path);
      classPageContent = cm.get(path);
//      if (this.connection.isClosed()) {
////        jdbcConnectionService..
//      }
      JSONObject classPageJson = buildClassPage(classId);
      LOGGER.debug("rendered classPageJson is:\n" + classPageJson);
      try {
        courseInfo = classPageJson.getJSONObject(Part.courseinfo.name());
        courseTitle = courseInfo != null ? courseInfo.getString("title") : "NO TITLE";
      } catch (JSONException e) {
        courseTitle = "NO TITLE";
        LOGGER.warn(e.getMessage(), e);
      }
      stringifiedClassPage = classPageJson.toString();
      if (classPageContent == null) {
        classPageContentMap = new HashMap<String, Object>(2);
        classPageContentMap.put("sling:resourceType", STORE_RESOURCETYPE);
        classPageContentMap.put(CLASS_PAGE_PROP_NAME, stringifiedClassPage);
        classPageContent = new Content(path, classPageContentMap);
        cm.update(classPageContent);
        synchronizationState = SynchronizationState.created;
      }
      else {
        classPageContent.setProperty(CLASS_PAGE_PROP_NAME, stringifiedClassPage);
        cm.update(classPageContent);
        synchronizationState = SynchronizationState.refreshed;
      }
    } catch (ClientPoolException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (StorageClientException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOGGER.warn(e.getMessage(), e);
    } finally {
      if (adminSession != null) {
        try {
          adminSession.logout();
        } catch (ClientPoolException e) {
          LOGGER.warn(e.getMessage(), e);
        }
      }      
    }
    
    return new ClassPageProvisionResult(classId, courseTitle, synchronizationState);
  }

  private JSONObject buildClassPage(String classId) {
    JSONObject classPage = null;
    CalClassPageBuilder builder = new CalClassPageBuilder();
    classPage = builder.begin(classId)
                .insert(Part.courseinfo)
                .end();
    return classPage;
  }

  /**
   * inner class to build the class page JSON
   */
  private class CalClassPageBuilder {
    
    private String classId;
    
    private JSONObject classPage;
    
    private CalClassPageBuilder begin(String classId) {
      this.classId = classId;
      ClassAttributeProvider attributeProvider = attributeProviders.get(Part.container);
      ClassPageRenderer renderer = renderers.get(Part.container);
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

    private CalClassPageBuilder insert(Part part) {
      JSONObject renderedSection = null;
      ClassAttributeProvider attributeProvider = attributeProviders.get(part);
      ClassPageRenderer renderer = renderers.get(part);
      List<Map<String, Object>> attributesList = attributeProvider.getAttributes(this.classId);
      if (attributesList.size() == 1) {
        try {
          renderedSection = renderer.render(attributesList.get(0));
          this.classPage.put(part.name(), renderedSection);
        } catch (JSONException e) {
          LOGGER.warn(e.getMessage(), e);
        }
      } else {
//        handle the tuples
      }
  ;    return this;
    }

    private JSONObject end() {
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
  }
   
  @Activate @Modified
  protected void activate(ComponentContext componentContext) {
    // wire up later to allow pipeline configurataion and template passing??

    Dictionary<?, ?> props = componentContext.getProperties();
    
    try {
      this.connection = this.jdbcConnectionService.getConnection();
    } catch (SQLException e) {
      LOGGER.warn(e.getMessage(), e);
      deactivate(componentContext);
    }
    // cab't get an ImmutableMap to handle types so using plain HashMaqp    
    this.attributeProviders = new HashMap<Part, ClassAttributeProvider>();
    this.attributeProviders.put(Part.container, new OracleClassPageContainerAttributeProvider(this.connection));
    this.attributeProviders.put(Part.courseinfo, new OracleClassPageCourseInfoAttributeProvider(this.connection));

    this.renderers = new HashMap<Part, ClassPageRenderer>();
    this.renderers.put(Part.container, new ClassPageContainerRenderer(repository, null));
    this.renderers.put(Part.courseinfo, new ClassPageCourseInfoRenderer(repository, null));
  }
  
  @SuppressWarnings("deprecation")
  @Deactivate
  protected void deactivate(ComponentContext componentContext) {
    if (this.connection != null) {
      try {
        this.connection.close();
      } catch (SQLException e) {
        LOGGER.info("Exception while closing dbConnection", e);
        throw new ComponentException("Could not close connection");
      } finally {
        this.connection = null;
        this.jdbcConnectionService = null;
      }
    }
  }
}
