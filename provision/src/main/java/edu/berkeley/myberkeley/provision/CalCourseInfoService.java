package edu.berkeley.myberkeley.provision;

import edu.berkeley.myberkeley.api.provision.CourseInfoProvisionResult;
import edu.berkeley.myberkeley.api.provision.CourseInfoService;
import edu.berkeley.myberkeley.api.provision.SynchronizationState;
import edu.berkeley.myberkeley.provision.provide.CourseAttributeProvider;
import edu.berkeley.myberkeley.provision.provide.OracleCourseHeaderAttributeProvider;
import edu.berkeley.myberkeley.provision.render.CourseInfoHeaderRenderer;
import edu.berkeley.myberkeley.provision.render.CourseInfoRenderer;

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
    label = "CalCentral :: Course Info Provision Service", description = "Create & Store Course Info")
@Service
public class CalCourseInfoService implements CourseInfoService {

  public static final String STORE_NAME = "_myberkeley_classpage";
  public static final String STORE_RESOURCETYPE = "myberkeley/c";
  public static final String CLASS_PAGE_PROP_NAME = "classPzge";
  
  private static final Logger LOGGER = LoggerFactory.getLogger(CalCourseInfoService.class);
  
  private Map<Component, CourseInfoRenderer> renderers;
  
  private Map<Component, CourseAttributeProvider> attributeProviders;
  
  private enum Component {
    classPageHeader
  }
  
  @Reference
  Repository repository;
  
  @Override
  public JSONObject getCourseInfo(String classId) {
    JSONObject courseInfo = null;
    Session adminSession = null;
    try {
      adminSession = repository.login();
      ContentManager cm = adminSession.getContentManager();
      Content courseInfoContent = cm.get(STORE_NAME + "/" + classId);
      if (courseInfoContent != null) {
        String courseInfoStr = (String) courseInfoContent.getProperty(CLASS_PAGE_PROP_NAME);
        courseInfo = new JSONObject(courseInfoStr);
      } else {
        LOGGER.warn("No classPageInkfo found for classId: " + classId);
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
    return courseInfo;
  }

  @Override
  public CourseInfoProvisionResult provisionCourseInfo(JSONObject courseInfo) {
    Session adminSession = null;
    SynchronizationState synchronizationState = SynchronizationState.error;
    Content courseInfoContent = null;
    String classId = null;
    Map<String, Object> courseInfoMap = null;
    try {
      adminSession = repository.loginAdministrative();
      ContentManager cm = adminSession.getContentManager();
      classId = courseInfo.getString("classid");
      if (classId == null) {
        throw new JSONException("classid key missing");
      }
      String path = STORE_NAME + "/" + classId;
      String stringifiedClaseeInfo = courseInfo.toString();
      courseInfoContent = cm.get(path);
      if (courseInfoContent == null) {
        courseInfoMap = new HashMap<String, Object>(2);
        courseInfoMap.put("sling:resourceType", STORE_RESOURCETYPE);
        courseInfoMap.put("courseInfo", stringifiedClaseeInfo);
        courseInfoContent = new Content(path, courseInfoMap);
        cm.update(courseInfoContent);
        synchronizationState = SynchronizationState.created;
      }
      else {
        courseInfoContent.setProperty("courseInfo", stringifiedClaseeInfo);
        cm.update(courseInfoContent);
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
    return new CourseInfoProvisionResult(classId, synchronizationState);
  }

  @Override
  public CourseInfoProvisionResult provisionCourseInfo(String classId) {
    CourseInfoProvisionResult result = null;
    Content courseInfoContent = null;
    Map<String, Object> courseInfoMap = null;
    Session adminSession = null;
    SynchronizationState synchronizationState = SynchronizationState.error;
    try {
      adminSession = repository.loginAdministrative();
      ContentManager cm = adminSession.getContentManager();
      String path = STORE_NAME + "/" + classId;
      courseInfoContent = cm.get(path);
      if (courseInfoContent == null) {
        courseInfoMap = new HashMap<String, Object>(2);
        courseInfoMap.put("sling:resourceType", STORE_RESOURCETYPE);
//        courseInfoMap.put("courseInfo", stringifiedClaseeInfo);
        JSONObject courseInfoJSON = buildJSON(classId);
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
    JSONObject courseInfo = null;
    CourseAttributeProvider headerProvider = this.attributeProviders.get(Component.classPageHeader);
    List<Map<String, Object>> headerAttriubesList = headerProvider.getAttributes(classId);
    CourseInfoRenderer headerRenderer = this.renderers.get(Component.classPageHeader);
    try {
      JSONObject headerJSON = headerRenderer.render(headerAttriubesList.get(0));
      LOGGER.debug("courseInfoHeader:" + headerJSON.toString());
    } catch (JSONException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return courseInfo;
  }

  @Activate @Modified
  protected void activate(ComponentContext componentContext) {
    // wire up later to allow pipeline configurataion and template passing??
    Dictionary<?, ?> props = componentContext.getProperties();
    
    // cab't get an ImmutableMap to handle types so using plain HashMaqp    
    this.attributeProviders = new HashMap<CalCourseInfoService.Component, CourseAttributeProvider>();
    this.attributeProviders.put(Component.classPageHeader, new OracleCourseHeaderAttributeProvider());

    this.renderers = new HashMap<Component, CourseInfoRenderer>();
    this.renderers.put(Component.classPageHeader, new CourseInfoHeaderRenderer(repository, null));
      
  }

}
