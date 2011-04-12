package edu.berkeley.myberkeley.caldav.report;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.Namespace;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Based on code found in "Introducing CalDAV (Part I and II)" at
 * http://blogs.nologin.es/rickyepoderi/index.php?/archives/15-Introducing-CalDAV-Part-II.html
 *
 * @author ricky
 */
public class CalendarQueryReportInfo extends ReportInfo {

  private RequestCalendarData calendarData = null;
  private Filter filter = null;
  private String timezone = null;

  public CalendarQueryReportInfo(RequestCalendarData calendarData, Filter filter) {
    super(CalendarQueryReport.CALENDAR_QUERY, DavConstants.DEPTH_1, CalDavConstants.ETAG);
    this.calendarData = calendarData;
    this.filter = filter;
    this.timezone = null;
  }

  @Override
  public Element toXml(Document document) {
    // create calendar-multiget element
    Element calendarQuery = DomUtil.createElement(document,
            CalendarQueryReport.CALENDAR_QUERY.getLocalName(),
            CalendarQueryReport.CALENDAR_QUERY.getNamespace());
    calendarQuery.setAttributeNS(Namespace.XMLNS_NAMESPACE.getURI(),
            Namespace.XMLNS_NAMESPACE.getPrefix() + ":" + DavConstants.NAMESPACE.getPrefix(),
            DavConstants.NAMESPACE.getURI());
    // append props
    Element prop = getPropertyNameSet().toXml(document);
    // append calendar-data request info
    prop.appendChild(calendarData.toXml(document));
    // append prop to calendarMultiGet
    calendarQuery.appendChild(prop);
    // append filter
    calendarQuery.appendChild(filter.toXml(document));
    // timezone
    if (timezone != null) {
      Element timezoneElement = DomUtil.createElement(document,
              CalDavConstants.CALDAV_XML_TIMEZONE, CalDavConstants.CALDAV_NAMESPACE);
      calendarQuery.appendChild(timezoneElement);
    }
    return calendarQuery;
  }

}

