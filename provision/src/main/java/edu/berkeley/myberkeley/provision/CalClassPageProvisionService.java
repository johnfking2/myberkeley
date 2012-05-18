package edu.berkeley.myberkeley.provision;

import edu.berkeley.myberkeley.api.provision.ClassPageProvisionResult;
import edu.berkeley.myberkeley.api.provision.ClassPageProvisionService;
import edu.berkeley.myberkeley.api.provision.SynchronizationState;
import edu.berkeley.myberkeley.provision.provide.ClassAttributeProvider;
import edu.berkeley.myberkeley.provision.provide.OracleClassPageHeaderAttributeProvider;
import edu.berkeley.myberkeley.provision.render.ClassPageHeaderRenderer;
import edu.berkeley.myberkeley.provision.render.ClassPageRenderer;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  
  private static final Logger LOGGER = LoggerFactory.getLogger(CalClassPageProvisionService.class);
  
  private Map<Component, ClassAttributeProvider> attributeProviders;
  
  private Map<Component, ClassPageRenderer> renderers;
  
  
  private enum Component {
    classPageHeader
  }
  
  @Reference
  Repository repository;
  
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
        classPageContent.setProperty("courseInfo", stringifiedClassPage);
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
    Session adminSession = null;
    SynchronizationState synchronizationState = SynchronizationState.error;
    try {
      adminSession = repository.loginAdministrative();
      ContentManager cm = adminSession.getContentManager();
      String path = STORE_NAME + "/" + classId;
      classPageContent = cm.get(path);
      if (classPageContent == null) {
        classPageContentMap = new HashMap<String, Object>(2);
        classPageContentMap.put("sling:resourceType", STORE_RESOURCETYPE);
        JSONObject classPageJSON = buildJSON(classId);
        classPageContentMap.put(CLASS_PAGE_PROP_NAME, classPageJSON.toString());
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
  
  private JSONObject buildJSON(String classId) {
    JSONObject classPageJSON = null;
    ClassAttributeProvider headerProvider = this.attributeProviders.get(Component.classPageHeader);
    List<Map<String, Object>> headerAttriubesList = headerProvider.getAttributes(classId);
    ClassPageRenderer headerRenderer = this.renderers.get(Component.classPageHeader);
    try {
      JSONObject headerJSON = headerRenderer.render(headerAttriubesList.get(0));
      LOGGER.debug("courseInfoHeader:" + headerJSON.toString());
    } catch (JSONException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return classPageJSON;
  }

  @Activate @Modified
  protected void activate(ComponentContext componentContext) {
    // wire up later to allow pipeline configurataion and template passing??
    Dictionary<?, ?> props = componentContext.getProperties();
    
    // cab't get an ImmutableMap to handle types so using plain HashMaqp    
    this.attributeProviders = new HashMap<CalClassPageProvisionService.Component, ClassAttributeProvider>();
    this.attributeProviders.put(Component.classPageHeader, new OracleClassPageHeaderAttributeProvider());

    this.renderers = new HashMap<Component, ClassPageRenderer>();
    this.renderers.put(Component.classPageHeader, new ClassPageHeaderRenderer(repository, null));
      
  }

}
