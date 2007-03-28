/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.XmlObject;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;

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
  public static final int      SERVER_GC_VERBOSE     = 50;
  public static final int      SERVER_GC_INTERVAL    = 55;
  // only this context may listen to "create" and "delete" events - corresponding "new" and "remove" events are
  // broadcast
  public static final int      CREATE_SERVER         = -5;
  public static final int      DELETE_SERVER         = -10;
  // only this context may notify "new" and "remove" events after receiving a corresponding "create" or "delete" event
  public static final int      NEW_SERVER            = ALT_RANGE_CONSTANT + 5;
  public static final int      REMOVE_SERVER         = ALT_RANGE_CONSTANT + 10;
  // container elements with no associated events
  public static final String   PARENT_ELEM_DSO       = "dso";
  public static final String   PARENT_ELEM_PERSIST   = "persistence";
  public static final String   PARENT_ELEM_GC        = "garbage-collection";

  private static final String  ELEM_NAME             = "name";
  private static final String  ELEM_HOST             = "host";
  private static final String  ELEM_DSO_PORT         = "dso-port";
  private static final String  ELEM_JMX_PORT         = "jmx-port";
  private static final String  ELEM_DATA             = "data";
  private static final String  ELEM_LOGS             = "logs";
  private static final String  ELEM_PERSIST          = "mode";
  private static final String  ELEM_GC               = "enabled";
  private static final String  ELEM_GC_VERBOSE       = "verbose";
  private static final String  ELEM_GC_INTERVAL      = "interval";

  public static final String[] m_elementNames        = new String[56];
  static {
    m_elementNames[SERVER_NAME] = ELEM_NAME;
    m_elementNames[SERVER_HOST] = ELEM_HOST;
    m_elementNames[SERVER_DSO_PORT] = ELEM_DSO_PORT;
    m_elementNames[SERVER_JMX_PORT] = ELEM_JMX_PORT;
    m_elementNames[SERVER_DATA] = ELEM_DATA;
    m_elementNames[SERVER_LOGS] = ELEM_LOGS;
    m_elementNames[SERVER_PERSIST] = ELEM_PERSIST;
    m_elementNames[SERVER_GC] = ELEM_GC;
    m_elementNames[SERVER_GC_VERBOSE] = ELEM_GC_VERBOSE;
    m_elementNames[SERVER_GC_INTERVAL] = ELEM_GC_INTERVAL;
  }

  public final int             type;
  public XmlObject             element;
  public Object                variable;                                       // extra field may contain anything

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
    sb.append("data=" + data + "\n");
    sb.append("source=" + source + "\n");
    sb.append("type=" + type + "\n");
    sb.append("element=" + element + "\n");
    sb.append("variable=" + variable + "\n");
    return sb.toString();
  }
}
