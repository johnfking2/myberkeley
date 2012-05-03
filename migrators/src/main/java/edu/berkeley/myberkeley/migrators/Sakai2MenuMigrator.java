/*
  * Licensed to the Sakai Foundation (SF) under one
  * or more contributor license agreements. See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership. The SF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License. You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied. See the License for the
  * specific language governing permissions and limitations under the License.
 */
package edu.berkeley.myberkeley.migrators;

import static org.sakaiproject.nakamura.lite.content.InternalContent.PATH_FIELD;
import static org.sakaiproject.nakamura.lite.content.InternalContent.STRUCTURE_UUID_FIELD;

import com.google.common.collect.ImmutableMap;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.Servlet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.PropertyMigrator;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service({PropertyMigrator.class})
@Component
public class Sakai2MenuMigrator implements PropertyMigrator {
  private static final Logger LOGGER = LoggerFactory.getLogger(Sakai2MenuMigrator.class);

  @Reference
  private Repository repository;

  @Override
  public boolean migrate(String rowId, Map<String, Object> properties) {
    if (properties.containsKey("sling:resourceType")) {
      String resourceType = properties.get("sling:resourceType").toString(); 
      if (resourceType.equals("sakai/private")) {
        migratePrivSpace(properties);
      } else if (resourceType.equals("sakai/public")) {
        migragePubSpace(properties);
      }
    }
    
    return false;
  }

  private void migratePrivSpace(Map<String, Object> properties) {
    Session session = null;
    try {
      if (properties.containsKey(PATH_FIELD)) {
        session = repository.loginAdministrative();
        String path = properties.get(PATH_FIELD).toString();
        Content privSpaceContent = session.getContentManager().get(path);
        Iterable<Content> children = privSpaceContent.listChildren();
        Iterator<Content> contentIter = children.iterator();
        while (contentIter.hasNext()) {
          Content privspace = contentIter.next();
          if (privspace.getPath().contains("privspace")) {
            migrateStruc0(privspace);
            migrateDashboard(privspace);
            migrateRows(privspace);
          }
        }
      }
    } catch (ClientPoolException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (StorageClientException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (AccessDeniedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      if (session != null) {
        try {
          session.logout();
        } catch (ClientPoolException e) {
          LOGGER.error("Unexpected exception logging out of session", e);
        }
      }
    }
    
  }
  
  private void migrateStruc0(Content privspace) throws JSONException {
    JSONObject struc0JSON = findStructure0(privspace);
    JSONObject menuObj = new JSONObject();
    menuObj.put("_ref", "${refid}2345");
    menuObj.put("_title", "My bSpace sites");
    menuObj.put("_order", 2);
    menuObj.put("_canEdit", Boolean.TRUE);
    menuObj.put("_reorderOnly", Boolean.TRUE);
    menuObj.put("_nonEditable", Boolean.TRUE);
    JSONObject mainObj = new JSONObject();
    mainObj.put("_ref", "${refid}2345");
    mainObj.put("_order", 0);
    mainObj.put("_title", "My Sakai 2 sites");
    menuObj.put("main", menuObj);
    struc0JSON.put("sakai2sites", menuObj.toString());
  //config.defaultprivstructure.structure0['sakai2sites'] =  {
//  '_ref': '${refid}2345',
//  '': 'My bSpace sites',
//  '_order': 2,
//  '_canEdit': true,
//  '_reorderOnly': true,
//  '_nonEditable': true,
//  'main': {
//      '_ref': '${refid}2345',
//      '_order': 0,
//      '_title': 'My Sakai 2 sites'
//  }
//};
  }

  private void migrateRows(Content privspace) {
    
//    config.defaultprivstructure['${refid}2345'] = {
//        'rows': [
//            {
//                'id': 'id8965114',
//                'columns': [
//                    {
//                        'width': 1,
//                        'elements': [
//                            {
//                                'id': '${refid}2346',
//                                'type': 'searchsakai2'
//                            }
//                        ]
//                    }
//                ]
//            }
//        ]
//    };
  }

  private void migrateDashboard(Content privspace) {
    
    
//    config.defaultprivstructure['${refid}0']['${refid}5'].dashboard.columns.column1.push({
//      'uid': '${refid}1234',
//      'visible': 'block',
//      'name': 'mysakai2'
//  });
  }

  
  private JSONObject findStructure0(Content foundContent) throws JSONException {
    JSONObject structure0 = null;
    Object structureObj = foundContent.getProperty("structure0");
    if (structureObj != null) {
      structure0 = new JSONObject(structureObj.toString());
    }
    return structure0;
  }                     

  private void migragePubSpace(Map<String, Object> properties) {

  }

  @Override
  public String[] getDependencies() {
    return new String[0];
  }

  @Override
  public String getName() {
    return Sakai2MenuMigrator.class.getName();
  }

  @Override
  public Map<String, String> getOptions() {
    return ImmutableMap.of(PropertyMigrator.OPTION_RUNONCE, "false");
  }
}
