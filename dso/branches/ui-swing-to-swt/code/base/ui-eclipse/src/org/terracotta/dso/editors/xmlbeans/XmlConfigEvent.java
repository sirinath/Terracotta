/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.XmlObject;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;

/**
 * This class should be treated as immutable. Don't change it's field values just because you can.
 */
public class XmlConfigEvent extends UpdateEvent {

  public static final int      ALT_RANGE_CONSTANT    = 999999;
  // if the config xml structure changes rename (producing errors) the effected event type to locate it's listeners
  public static final int      XML_STRUCTURE_CHANGED = 0;
  public static final int      SERVER_NAME           = 10;
  public static final int      SERVER_HOST           = 15;
  public static final int      SERVER_DSO_PORT       = 20;
  public static final int      SERVER_JMX_PORT       = 25;
  public static final int      SERVER_DATA           = 30;
  public static final int      SERVER_LOGS           = 35;
  public static final int      SERVER_PERSIST        = 40;
  public static final int      SERVER_GC             = 45;
  public static final int      SERVER_VERBOSE        = 50;
  public static final int      SERVER_GC_INTERVAL    = 55;
  // only this context may listen to "create" and "delete" events - corresponding "new" and "remove" events are
  // broadcast
  public static final int      CREATE_SERVER         = -5;
  public static final int      DELETE_SERVER         = -10;
  // only this context may notify "new" and "remove" events after receiving a corresponding "create" or "delete" event
  public static final int      NEW_SERVER            = ALT_RANGE_CONSTANT + 5;
  public static final int      REMOVE_SERVER         = ALT_RANGE_CONSTANT + 10;
  
  private static final String  ELEM_NAME             = "name";
  private static final String  ELEM_HOST             = "host";
  private static final String  ELEM_DSO_PORT         = "dso-port";
  private static final String  ELEM_JMX_PORT         = "jmx-port";
  private static final String  ELEM_DATA             = "data";
  private static final String  ELEM_LOGS             = "logs";

  public static final String[] elementNames          = new String[55];
  static {
    elementNames[SERVER_NAME] = ELEM_NAME;
    elementNames[SERVER_HOST] = ELEM_HOST;
    elementNames[SERVER_DSO_PORT] = ELEM_DSO_PORT;
    elementNames[SERVER_JMX_PORT] = ELEM_JMX_PORT;
    elementNames[SERVER_DATA] = ELEM_DATA;
    elementNames[SERVER_LOGS] = ELEM_LOGS;
  }

  public final int             type;
  public XmlObject             element;

  public XmlConfigEvent(int type) {
    this(null, null, null, type);
  }

  public XmlConfigEvent(XmlObject element, int type) {
    this(null, null, element, type);
  }

  public XmlConfigEvent(Object data, UpdateEventListener source, XmlObject element, int type) {
    super(data);
    this.source = source; // may be null
    this.element = element;
    this.type = type;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("data=" + data);
    sb.append("source=" + source);
    sb.append("type=" + type);
    sb.append("element=" + element);
    return sb.toString();
  }
}
