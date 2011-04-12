package edu.berkeley.myberkeley.caldav;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;

@Component(label = "MyBerkeley :: CalDavConnectorProvider",
        description = "Provider for CalDav server authentication information",
        immediate = true, metatype = true)
@Service(value = CalDavConnectorProvider.class)
public class CalDavConnectorProvider {

  @org.apache.felix.scr.annotations.Property(value = "admin", label = "CalDav Admin Username")
  protected static final String PROP_ADMIN_USERNAME = "caldavconnectorprovider.adminusername";

  @org.apache.felix.scr.annotations.Property(value = "bedework", label = "CalDav Admin Password")
  protected static final String PROP_ADMIN_PASSWORD = "caldavconnectorprovider.adminpassword";

  @org.apache.felix.scr.annotations.Property(value = "http://test.media.berkeley.edu:8080", label = "CalDav Server Root")
  protected static final String PROP_SERVER_ROOT = "caldavconnectorprovider.serverroot";

  private String adminUsername;

  private String adminPassword;

  private String calDavServerRoot;

  protected void activate(ComponentContext componentContext) throws Exception {
    Dictionary<?, ?> props = componentContext.getProperties();
    this.adminUsername = (String) props.get(PROP_ADMIN_USERNAME);
    this.adminPassword = (String) props.get(PROP_ADMIN_PASSWORD);
    this.calDavServerRoot = (String) props.get(PROP_SERVER_ROOT);

  }

  public CalDavConnector getCalDavConnector() throws URIException {
    // TODO get connector appropriate for recipient user
    // TODO make admin password configurable

    return new CalDavConnector(this.adminUsername, this.adminPassword,
            new URI(this.calDavServerRoot, false),
            new URI(this.calDavServerRoot + "/ucaldav/user/vbede/calendar/", false));
  }

}
