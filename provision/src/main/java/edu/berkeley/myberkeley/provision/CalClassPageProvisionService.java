package edu.berkeley.myberkeley.provision;

import edu.berkeley.myberkeley.api.provision.ClassPageProvisionResult;
import edu.berkeley.myberkeley.api.provision.ClassPageProvisionService;
import edu.berkeley.myberkeley.api.provision.JdbcConnectionService;
import edu.berkeley.myberkeley.api.provision.SynchronizationState;
import edu.berkeley.myberkeley.provision.CalClassPageBuilderOuter.Section;
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
  
  public enum Section {
    container,
    courseinfo,
    instructors,
    schedule,
    sections
  }
  
  private static final Logger LOGGER = LoggerFactory.getLogger(CalClassPageProvisionService.class);
  
  Map<Section, ClassAttributeProvider> attributeProviders;
  
  Map<Section, ClassPageRenderer> renderers;
  
  @Reference
  Repository repository;
  
  @Reference
  JdbcConnectionService jdbcConnectionService;
  
  Connection dbConnection;
  
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
    Map<String, Object> classPageContentMap = null;
    try {
      adminSession = repository.loginAdministrative();
      ContentManager cm = adminSession.getContentManager();
      classId = classPageJson.getString("classid");
      if (classId == null) {
        throw new JSONException("classid key missing");
      }
      String path = STORE_NAME + "/" + classId;
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
    return new ClassPageProvisionResult(classId, synchronizationState);
  }

  @Override
  public ClassPageProvisionResult provisionClassPage(String classId) {
    ClassPageProvisionResult result = null;
    Content classPageContent = null;
    Map<String, Object> classPageContentMap = null;
    String stringifiedClassPage = null;
    Session adminSession = null;
    SynchronizationState synchronizationState = SynchronizationState.error;
    try {
      adminSession = repository.loginAdministrative();
      ContentManager cm = adminSession.getContentManager();
      String path = STORE_NAME + "/" + classId;
      classPageContent = cm.get(path);
      JSONObject classPageJSON = buildClassPage(classId);
      stringifiedClassPage = classPageJSON.toString();  
      if (classPageContent == null) {
        classPageContentMap = new HashMap<String, Object>(2);
        classPageContentMap.put("sling:resourceType", STORE_RESOURCETYPE);
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
    
    return result;
  }

  private JSONObject buildClassPage(String classId) {
    JSONObject classPage = null;
    CalClassPageBuilder builder = new CalClassPageBuilder();
    JSONObject classPag = builder.begin(classId)
            .insert(Section.courseinfo)
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
      ClassAttributeProvider attributeProvider = attributeProviders.get(Section.container);
      ClassPageRenderer renderer = renderers.get(Section.container);
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

    private CalClassPageBuilder insert(Section section) {
      JSONObject renderedSection = null;
      ClassAttributeProvider attributeProvider = attributeProviders.get(section);
      ClassPageRenderer renderer = renderers.get(section);
      List<Map<String, Object>> attributesList = attributeProvider.getAttributes(this.classId);
      if (attributesList.size() == 1) {
        try {
          renderedSection = renderer.render(attributesList.get(0));
          this.classPage.put(section.name(), renderedSection);
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
