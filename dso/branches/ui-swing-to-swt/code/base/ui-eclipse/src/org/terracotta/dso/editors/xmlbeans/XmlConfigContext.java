/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.core.resources.IProject;

import com.tc.util.event.EventMulticaster;
import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;

import java.util.HashMap;
import java.util.Map;

public final class XmlConfigContext {

  // if the config xml structure changes rename (producing errors) the effected event type to locate it's listeners
  public static final int                              XML_STRUCTURE_CHANGED_EVENT = 0;
  public static final int                              SERVER_ADDED_EVENT          = 5;
  public static final int                              SERVER_NAME_EVENT           = 10;
  public static final int                              SERVER_HOST_EVENT           = 15;
  public static final int                              SERVER_DSO_PORT_EVENT       = 20;
  public static final int                              SERVER_JMX_PORT_EVENT       = 25;
  public static final int                              SERVER_DATA_EVENT           = 30;
  public static final int                              SERVER_LOGS_EVENT           = 35;
  public static final int                              SERVER_PERSIST_EVENT        = 40;
  public static final int                              SERVER_GC_EVENT             = 45;
  public static final int                              SERVER_VERBOSE_EVENT        = 50;
  public static final int                              SERVER_GC_INTERVAL_EVENT    = 55;

  public static final String                           ELEM_DATA                   = "data";

  private final EventMulticaster                       m_xmlStructureChangedObserver;
  private UpdateEventListener                          m_xmlStructureChangedListener;
  private final EventMulticaster                       m_serverAddedObserver;
  private UpdateEventListener                          m_serverAddedListener;
  private final EventMulticaster                       m_serverNameObserver;
  private UpdateEventListener                          m_serverNameListener;
  private final EventMulticaster                       m_serverHostObserver;
  private UpdateEventListener                          m_serverHostListener;
  private final EventMulticaster                       m_serverDSOPortObserver;
  private UpdateEventListener                          m_serverDSOPortListener;
  private final EventMulticaster                       m_serverJMXPortObserver;
  private UpdateEventListener                          m_serverJMXPortListener;
  private final EventMulticaster                       m_serverDataObserver;
  private UpdateEventListener                          m_serverDataListener;
  private final EventMulticaster                       m_serverLogsObserver;
  private UpdateEventListener                          m_serverLogsListener;
  private final EventMulticaster                       m_serverPersistObserver;
  private UpdateEventListener                          m_serverPersistListener;
  private final EventMulticaster                       m_serverGCObserver;
  private UpdateEventListener                          m_serverGCListener;
  private final EventMulticaster                       m_serverVerboseObserver;
  private UpdateEventListener                          m_serverVerboseListener;
  private final EventMulticaster                       m_serverGCIntervalObserver;
  private UpdateEventListener                          m_serverGCIntervalListener;

  private static final Map<IProject, XmlConfigContext> m_contexts                  = new HashMap<IProject, XmlConfigContext>();
  private final XmlConfigPersistenceManager            m_persistenceManager;

  private XmlConfigContext(IProject project) {
    m_persistenceManager = new XmlConfigPersistenceManager();
    m_contexts.put(project, this);
    m_xmlStructureChangedObserver = new EventMulticaster();
    m_serverAddedObserver = new EventMulticaster();
    m_serverNameObserver = new EventMulticaster();
    m_serverHostObserver = new EventMulticaster();
    m_serverDSOPortObserver = new EventMulticaster();
    m_serverJMXPortObserver = new EventMulticaster();
    m_serverDataObserver = new EventMulticaster();
    m_serverLogsObserver = new EventMulticaster();
    m_serverPersistObserver = new EventMulticaster();
    m_serverGCObserver = new EventMulticaster();
    m_serverVerboseObserver = new EventMulticaster();
    m_serverGCIntervalObserver = new EventMulticaster();
    init();
  }

  public static synchronized XmlConfigContext getInstance(IProject project) {
    if (m_contexts.containsKey(project)) return m_contexts.get(project);
    return new XmlConfigContext(project);
  }

  // register context listeners - to persist state to xml beans
  private void init() {
    addListener(m_xmlStructureChangedListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent data) {
        System.out.println(data.data);// XXX
      }
    }, XML_STRUCTURE_CHANGED_EVENT);

    addListener(m_serverDataListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent data) {
        XmlObject server = ((XmlChangeEvent) data).element;
        m_persistenceManager.writeElement(server, server.schemaType().getJavaClass(), ELEM_DATA, (String) data.data);
      }
    }, SERVER_DATA_EVENT);
  }

  private void doAction(XmlAction action, int type) {
    switch (type) {
      case XML_STRUCTURE_CHANGED_EVENT:
        action.exec(m_xmlStructureChangedObserver, m_xmlStructureChangedListener);
        break;
      case SERVER_ADDED_EVENT:
        action.exec(m_serverAddedObserver, m_serverAddedListener);
        break;
      case SERVER_NAME_EVENT:
        action.exec(m_serverNameObserver, m_serverNameListener);
        break;
      case SERVER_HOST_EVENT:
        action.exec(m_serverHostObserver, m_serverHostListener);
        break;
      case SERVER_DSO_PORT_EVENT:
        action.exec(m_serverDSOPortObserver, m_serverDSOPortListener);
        break;
      case SERVER_JMX_PORT_EVENT:
        action.exec(m_serverJMXPortObserver, m_serverJMXPortListener);
        break;
      case SERVER_DATA_EVENT:
        action.exec(m_serverDataObserver, m_serverDataListener);
        break;
      case SERVER_LOGS_EVENT:
        action.exec(m_serverLogsObserver, m_serverLogsListener);
        break;
      case SERVER_PERSIST_EVENT:
        action.exec(m_serverPersistObserver, m_serverPersistListener);
        break;
      case SERVER_GC_EVENT:
        action.exec(m_serverGCObserver, m_serverGCListener);
        break;
      case SERVER_VERBOSE_EVENT:
        action.exec(m_serverVerboseObserver, m_serverVerboseListener);
        break;
      case SERVER_GC_INTERVAL_EVENT:
        action.exec(m_serverGCIntervalObserver, m_serverGCIntervalListener);
        break;

      default:
        break;
    }
  }

  /**
   * Notify <tt>XmlContext</tt> that a change has occured
   */
  public void notify(XmlChangeEvent event) {
    notify(event, false);
  }

  /**
   * Update listeners with current XmlContext state. This should be used to initialize object state.
   */
  public void updateListeners(XmlChangeEvent event) {
    event.data = m_persistenceManager.readElement(event.element, event.element.schemaType().getJavaClass(),
        event.elementName);
    notify(event, true);
  }

  private void notify(final XmlChangeEvent event, final boolean ignoreContext) {
    doAction(new XmlAction() {
      public void exec(EventMulticaster multicaster, UpdateEventListener source) {
        if (ignoreContext) event.source = source;
        multicaster.fireUpdateEvent(event);
      }
    }, event.type);
  }

  public void addListener(final UpdateEventListener listener, int type) {
    doAction(new XmlAction() {
      public void exec(EventMulticaster multicaster, UpdateEventListener source) {
        multicaster.addListener(listener);
      }
    }, type);
  }

  public void removeListener(final UpdateEventListener listener, int type) {
    doAction(new XmlAction() {
      public void exec(EventMulticaster multicaster, UpdateEventListener source) {
        multicaster.removeListener(listener);
      }
    }, type);
  }

  // --------------------------------------------------------------------------------

  private interface XmlAction {
    void exec(EventMulticaster multicaster, UpdateEventListener source);
  }

  // --------------------------------------------------------------------------------

  public static final class XmlChangeEvent extends UpdateEvent {
    public int       type;
    public XmlObject element;
    public String    elementName; // element name described by ELEM_X member variables of this class

    public XmlChangeEvent(Object data, UpdateEventListener source, XmlObject element, String elementName, int type) {
      super(data);
      this.source = source; // may be null
      this.element = element;
      this.type = type;
      this.elementName = elementName;
    }
  }
}
