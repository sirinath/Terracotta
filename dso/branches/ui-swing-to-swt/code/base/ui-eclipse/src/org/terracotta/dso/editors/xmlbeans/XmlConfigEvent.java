/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.XmlObject;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;


public class XmlConfigEvent extends UpdateEvent {

  public static final int    NEW_RANGE_CONSTANT          = 999999;
  // if the config xml structure changes rename (producing errors) the effected event type to locate it's listeners
  public static final int    XML_STRUCTURE_CHANGED = 0;
  public static final int    SERVER_NAME           = 10;
  public static final int    SERVER_HOST           = 15;
  public static final int    SERVER_DSO_PORT       = 20;
  public static final int    SERVER_JMX_PORT       = 25;
  public static final int    SERVER_DATA           = 30;
  public static final int    SERVER_LOGS           = 35;
  public static final int    SERVER_PERSIST        = 40;
  public static final int    SERVER_GC             = 45;
  public static final int    SERVER_VERBOSE        = 50;
  public static final int    SERVER_GC_INTERVAL    = 55;
  // only this context may listen to "create" events - a corresponding "new" event will be published
  public static final int    CREATE_SERVER         = -5;
  // only this context may notify "new" events after receiving a corresponding "create" event
  public static final int    NEW_SERVER            = NEW_RANGE_CONSTANT + 5;

  public static final String ELEM_NAME                   = "name";
  public static final String ELEM_HOST                   = "host";
  public static final String ELEM_DSO_PORT               = "dso-port";
  public static final String ELEM_JMX_PORT               = "jmx-port";
  public static final String ELEM_DATA                   = "data";

  public final int           type;
  public XmlObject           element;
  public String              elementName;

  public XmlConfigEvent(int type) {
    this(null, null, null, null, type);
  }

  public XmlConfigEvent(XmlObject element, String elementName, int type) {
    this(null, null, element, elementName, type);
  }

  public XmlConfigEvent(Object data, UpdateEventListener source, XmlObject element, String elementName, int type) {
    super(data);
    this.source = source; // may be null
    this.element = element;
    this.type = type;
    this.elementName = elementName;
  }
}
