package edu.berkeley.myberkeley.provision;

import edu.berkeley.myberkeley.api.provision.CourseInfoProvisionResult;
import edu.berkeley.myberkeley.api.provision.CourseInfoService;
import edu.berkeley.myberkeley.api.provision.ProvisionResult;
import edu.berkeley.myberkeley.api.provision.SynchronizationState;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(metatype = true,
    label = "CalCentral :: Course Info Provision Service", description = "Create & Store Course Info")
@Service
public class CalCourseInfoService implements CourseInfoService {

  public static final String STORE_NAME = "_myberkeley_courseInfo";
  public static final String STORE_RESOURCETYPE = "myberkeley/courseinfo";
  
  private static final Logger LOGGER = LoggerFactory.getLogger(CalCourseInfoService.class);
  
  @Reference
  Repository repository;
  
  @Override
  public JSONObject getCourseInfo(String courseId) {
    return null;
  }

  @Override
  public CourseInfoProvisionResult saveCourseInfo(JSONObject courseInfo) {
    Session adminSession = null;
    SynchronizationState synchronizationState = SynchronizationState.error;
    Content courseInfoContent = null;
    String courseId = null;
    Map<String, Object> courseInfoMap = null;
    try {
      adminSession = repository.loginAdministrative();
      ContentManager cm = adminSession.getContentManager();
      courseId = courseInfo.getString("courseid");
      if (courseId == null) {
        throw new JSONException("courseId key missing");
      }
      String path = STORE_NAME + "/" + courseId;
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
    return new CourseInfoProvisionResult(courseId, synchronizationState);
  }

}
