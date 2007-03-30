/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.core.resources.IProject;
import org.terracotta.dso.TcPlugin;
import org.terracotta.ui.util.SWTComponentModel;

import com.tc.util.event.EventMulticaster;
import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.Client;
import com.terracottatech.config.DsoClientData;
import com.terracottatech.config.DsoClientDebugging;
import com.terracottatech.config.DsoServerData;
import com.terracottatech.config.Server;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class XmlConfigContext {

  public static final String                           DEFAULT_NAME = "dev";
  public static final String                           DEFAULT_HOST = "localhost";

  private final EventMulticaster                       m_xmlStructureChangedObserver;
  private UpdateEventListener                          m_xmlStructureChangedListener;
  private final EventMulticaster                       m_serverNameObserver;
  private UpdateEventListener                          m_serverNameListener;
  private final EventMulticaster                       m_serverHostObserver;
  private UpdateEventListener                          m_serverHostListener;
  private final EventMulticaster                       m_serverDSOPortObserver;
  private UpdateEventListener                          m_serverDsoPortListener;
  private final EventMulticaster                       m_serverJMXPortObserver;
  private UpdateEventListener                          m_serverJmxPortListener;
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

  private final EventMulticaster                       m_clientLogsObserver;
  private UpdateEventListener                          m_clientLogsListener;
  private final EventMulticaster                       m_clientDSOReflectionEnabledObserver;
  private UpdateEventListener                          m_clientDSOReflectionEnabledListener;
  private final EventMulticaster                       m_clientClassObserver;
  private UpdateEventListener                          m_clientClassListener;
  private final EventMulticaster                       m_clientHierarchyObserver;
  private UpdateEventListener                          m_clientHierarchyListener;
  private final EventMulticaster                       m_clientLocksObserver;
  private UpdateEventListener                          m_clientLocksListener;
  private final EventMulticaster                       m_clientTransientRootObserver;
  private UpdateEventListener                          m_clientTransientRootListener;
  private final EventMulticaster                       m_clientDistributedMethodsObserver;
  private UpdateEventListener                          m_clientDistributedMethodsListener;
  private final EventMulticaster                       m_clientRootsObserver;
  private UpdateEventListener                          m_clientRootsListener;
  private final EventMulticaster                       m_clientLockDebugObserver;
  private UpdateEventListener                          m_clientLockDebugListener;
  private final EventMulticaster                       m_clientDistributedMethodDebugObserver;
  private UpdateEventListener                          m_clientDistributedMethodDebugListener;
  private final EventMulticaster                       m_clientFieldChangeDebugObserver;
  private UpdateEventListener                          m_clientFieldChangeDebugListener;
  private final EventMulticaster                       m_clientNonPortableWarningObserver;
  private UpdateEventListener                          m_clientNonPortableWarningListener;
  private final EventMulticaster                       m_clientPartialInstrumentationObserver;
  private UpdateEventListener                          m_clientPartialInstrumentationListener;
  private final EventMulticaster                       m_clientWaitNotifyDebugObserver;
  private UpdateEventListener                          m_clientWaitNofifyDebugListener;
  private final EventMulticaster                       m_clientNewObjectDebugObserver;
  private UpdateEventListener                          m_clientNewObjectDebugListener;
  private final EventMulticaster                       m_clientAutolockDetailsObserver;
  private UpdateEventListener                          m_clientAutolockDetialsListener;
  private final EventMulticaster                       m_clientCallerObserver;
  private UpdateEventListener                          m_clientCallerListener;
  private final EventMulticaster                       m_clientFullStackObserver;
  private UpdateEventListener                          m_clientFullStackListener;
  private final EventMulticaster                       m_clientFindNeededIncludesObserver;
  private UpdateEventListener                          m_clientFindNeededIncludesListener;
  private final EventMulticaster                       m_clientFaultCountObserver;
  private UpdateEventListener                          m_clientFaultCountListener;

  // context new/remove element observers
  private final EventMulticaster                       m_newServerObserver;
  private final EventMulticaster                       m_removeServerObserver;
  // context create/delete listeners
  private UpdateEventListener                          m_createServerListener;
  private UpdateEventListener                          m_deleteServerListener;

  private static final Map<IProject, XmlConfigContext> m_contexts   = new HashMap<IProject, XmlConfigContext>();
  private final TcConfig                               m_config;
  private final Map<SWTComponentModel, List>           m_componentModels;

  private XmlConfigContext(IProject project) {
    this.m_config = TcPlugin.getDefault().getConfiguration(project);
    this.m_componentModels = new HashMap<SWTComponentModel, List>();
    m_contexts.put(project, this);
    // standard observers
    this.m_xmlStructureChangedObserver = new EventMulticaster();
    this.m_serverNameObserver = new EventMulticaster();
    this.m_serverHostObserver = new EventMulticaster();
    this.m_serverDSOPortObserver = new EventMulticaster();
    this.m_serverJMXPortObserver = new EventMulticaster();
    this.m_serverDataObserver = new EventMulticaster();
    this.m_serverLogsObserver = new EventMulticaster();
    this.m_serverPersistObserver = new EventMulticaster();
    this.m_serverGCObserver = new EventMulticaster();
    this.m_serverVerboseObserver = new EventMulticaster();
    this.m_serverGCIntervalObserver = new EventMulticaster();
    this.m_clientLogsObserver = new EventMulticaster();
    this.m_clientDSOReflectionEnabledObserver = new EventMulticaster();
    this.m_clientClassObserver = new EventMulticaster();
    this.m_clientHierarchyObserver = new EventMulticaster();
    this.m_clientLocksObserver = new EventMulticaster();
    this.m_clientTransientRootObserver = new EventMulticaster();
    this.m_clientDistributedMethodsObserver = new EventMulticaster();
    this.m_clientRootsObserver = new EventMulticaster();
    this.m_clientLockDebugObserver = new EventMulticaster();
    this.m_clientDistributedMethodDebugObserver = new EventMulticaster();
    this.m_clientFieldChangeDebugObserver = new EventMulticaster();
    this.m_clientNonPortableWarningObserver = new EventMulticaster();
    this.m_clientPartialInstrumentationObserver = new EventMulticaster();
    this.m_clientWaitNotifyDebugObserver = new EventMulticaster();
    this.m_clientNewObjectDebugObserver = new EventMulticaster();
    this.m_clientAutolockDetailsObserver = new EventMulticaster();
    this.m_clientCallerObserver = new EventMulticaster();
    this.m_clientFullStackObserver = new EventMulticaster();
    this.m_clientFindNeededIncludesObserver = new EventMulticaster();
    this.m_clientFaultCountObserver = new EventMulticaster();
    // "new" and "remove" element observers
    this.m_newServerObserver = new EventMulticaster();
    this.m_removeServerObserver = new EventMulticaster();
    init();
  }

  public static synchronized XmlConfigContext getInstance(IProject project) {
    if (m_contexts.containsKey(project)) return m_contexts.get(project);
    return new XmlConfigContext(project);
  }

  /**
   * Update listeners with current XmlContext state. This should be used to initialize object state.
   */
  public void updateListeners(final XmlConfigEvent event) {
    doAction(new XmlAction() {
      public void exec(EventMulticaster multicaster, UpdateEventListener source) {
        event.data = XmlConfigPersistenceManager.readElement(event.element, XmlConfigEvent.m_elementNames[event.type]);
        event.source = source;
        multicaster.fireUpdateEvent(event);
      }

      public XmlConfigEvent getEvent() {
        return event;
      }
    }, event.type);
  }

  /**
   * Notify <tt>XmlContext</tt> that a change has occured
   */
  public void notifyListeners(final XmlConfigEvent event) {
    if (event.type < 0) {
      creationEvent(event);
      return;
    } else if (event.type > XmlConfigEvent.ALT_RANGE_CONSTANT) return;
    doAction(new XmlAction() {
      public void exec(EventMulticaster multicaster, UpdateEventListener source) {
        multicaster.fireUpdateEvent(event);
      }

      public XmlConfigEvent getEvent() {
        return event;
      }
    }, event.type);
  }

  public void addListener(final UpdateEventListener listener, int type) {
    addListener(listener, type, null);
  }

  public void addListener(final UpdateEventListener listener, int type, final SWTComponentModel model) {
    doAction(new XmlAction() {
      public void exec(EventMulticaster multicaster, UpdateEventListener source) {
        multicaster.addListener(listener);
        MulticastListenerPair mLPair = new MulticastListenerPair();
        mLPair.multicaster = multicaster;
        mLPair.listener = listener;
        if (!m_componentModels.containsKey(model)) {
          List<MulticastListenerPair> list = new LinkedList<MulticastListenerPair>();
          m_componentModels.put(model, list);
          list.add(mLPair);
        } else m_componentModels.get(model).add(mLPair);
      }

      public XmlConfigEvent getEvent() {
        return null;
      }
    }, type);
  }

  public void detachComponentModel(SWTComponentModel model) {
    List<MulticastListenerPair> pairs = m_componentModels.get(model);
    for (Iterator<MulticastListenerPair> iter = pairs.iterator(); iter.hasNext();) {
      MulticastListenerPair pair = iter.next();
      pair.multicaster.removeListener(pair.listener);
    }
  }

  public void removeListener(final UpdateEventListener listener, int type) {
    doAction(new XmlAction() {
      public void exec(EventMulticaster multicaster, UpdateEventListener source) {
        multicaster.removeListener(listener);
      }

      public XmlConfigEvent getEvent() {
        return null;
      }
    }, type);
  }

  // HELPERS
  public static String[] getListDefaults(Class parentType, int type) {
    return XmlConfigPersistenceManager.getListDefaults(parentType, XmlConfigEvent.m_elementNames[type]);
  }

  // register context listeners - to persist state to xml beans
  private void init() {
    registerEventListeners();
    registerContextEventListeners();
  }
  
  private void registerEventListeners() {
    addListener(m_xmlStructureChangedListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent data) {
        // GENERAL EVENT TO PROVOKE ALL EVENT LISTENERS TO UPDATE THEIR VALUES
        System.out.println(data.data);// XXX
      }
    }, XmlConfigEvent.XML_STRUCTURE_CHANGED);
    // server
    addListener(m_serverNameListener = newWriter(), XmlConfigEvent.SERVER_NAME);
    addListener(m_serverHostListener = newWriter(), XmlConfigEvent.SERVER_HOST);
    addListener(m_serverDsoPortListener = newWriter(), XmlConfigEvent.SERVER_DSO_PORT);
    addListener(m_serverJmxPortListener = newWriter(), XmlConfigEvent.SERVER_JMX_PORT);
    addListener(m_serverDataListener = newWriter(), XmlConfigEvent.SERVER_DATA);
    addListener(m_serverDataListener = newWriter(), XmlConfigEvent.SERVER_LOGS);
    addListener(m_serverDataListener = newWriter(), XmlConfigEvent.SERVER_LOGS);
    addListener(m_serverPersistListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        XmlConfigEvent event = (XmlConfigEvent) e;
        final String element = XmlConfigEvent.m_elementNames[event.type];
        XmlObject xml = ensureServerDsoPersistElement((XmlObject) event.variable);
        XmlConfigPersistenceManager.writeElement(xml, element, (String) event.data);
      }
    }, XmlConfigEvent.SERVER_PERSIST);
    // server gc
    addListener(m_serverGCIntervalListener = newGCWriter(), XmlConfigEvent.SERVER_GC_INTERVAL);
    addListener(m_serverGCListener = newGCWriter(), XmlConfigEvent.SERVER_GC);
    addListener(m_serverVerboseListener = newGCWriter(), XmlConfigEvent.SERVER_GC_VERBOSE);
    // client
    addListener(m_clientLogsListener = newWriter(), XmlConfigEvent.CLIENT_LOGS);
    addListener(m_clientDSOReflectionEnabledListener = newWriter(), XmlConfigEvent.CLIENT_DSO_REFLECTION_ENABLED);
    // client instrumentation logging
    addListener(m_clientClassListener = newInstLoggingWriter(), XmlConfigEvent.CLIENT_CLASS);
    addListener(m_clientHierarchyListener = newInstLoggingWriter(), XmlConfigEvent.CLIENT_HIERARCHY);
    addListener(m_clientLocksListener = newInstLoggingWriter(), XmlConfigEvent.CLIENT_LOCKS);
    addListener(m_clientTransientRootListener = newInstLoggingWriter(), XmlConfigEvent.CLIENT_TRANSIENT_ROOT);
    addListener(m_clientDistributedMethodsListener = newInstLoggingWriter(), XmlConfigEvent.CLIENT_DISTRIBUTED_METHODS);
    addListener(m_clientRootsListener = newInstLoggingWriter(), XmlConfigEvent.CLIENT_ROOTS);
    // client runtime logging
    addListener(m_clientLockDebugListener = newRuntimeLoggingWriter(), XmlConfigEvent.CLIENT_LOCK_DEBUG);
    addListener(m_clientDistributedMethodDebugListener = newRuntimeLoggingWriter(),
        XmlConfigEvent.CLIENT_DISTRIBUTED_METHOD_DEBUG);
    addListener(m_clientFieldChangeDebugListener = newRuntimeLoggingWriter(), XmlConfigEvent.CLIENT_FIELD_CHANGE_DEBUG);
    addListener(m_clientNonPortableWarningListener = newRuntimeLoggingWriter(),
        XmlConfigEvent.CLIENT_NON_PORTABLE_WARNING);
    addListener(m_clientPartialInstrumentationListener = newRuntimeLoggingWriter(),
        XmlConfigEvent.CLIENT_PARTIAL_INSTRUMENTATION);
    addListener(m_clientWaitNofifyDebugListener = newRuntimeLoggingWriter(), XmlConfigEvent.CLIENT_WAIT_NOTIFY_DEBUG);
    addListener(m_clientNewObjectDebugListener = newRuntimeLoggingWriter(), XmlConfigEvent.CLIENT_NEW_OBJECT_DEBUG);
    // client runtime output
    addListener(m_clientAutolockDetialsListener = newRuntimeOutputWriter(), XmlConfigEvent.CLIENT_AUTOLOCK_DETAILS);
    addListener(m_clientCallerListener = newRuntimeOutputWriter(), XmlConfigEvent.CLIENT_CALLER);
    addListener(m_clientFullStackListener = newRuntimeOutputWriter(), XmlConfigEvent.CLIENT_FULL_STACK);
    addListener(m_clientFindNeededIncludesListener = newRuntimeOutputWriter(),
        XmlConfigEvent.CLIENT_FIND_NEEDED_INCLUDES);
    addListener(m_clientFaultCountListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        XmlConfigEvent event = (XmlConfigEvent) e;
        final String element = XmlConfigEvent.m_elementNames[event.type];
        XmlObject xml = ensureClientDsoElement((XmlObject) event.variable);
        XmlConfigPersistenceManager.writeElement(xml, element, (String) event.data);
      }
    }, XmlConfigEvent.CLIENT_FAULT_COUNT);
  }

  private UpdateEventListener newInstLoggingWriter() {
    return new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        XmlConfigEvent event = (XmlConfigEvent) e;
        final String element = XmlConfigEvent.m_elementNames[event.type];
        XmlObject xml = ensureClientInstrumentationLoggingElement((XmlObject) event.variable);
        XmlConfigPersistenceManager.writeElement(xml, element, (String) event.data);
      }
    };
  }

  private UpdateEventListener newRuntimeOutputWriter() {
    return new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        XmlConfigEvent event = (XmlConfigEvent) e;
        final String element = XmlConfigEvent.m_elementNames[event.type];
        XmlObject xml = ensureClientRuntimeOutputOptionsElement((XmlObject) event.variable);
        XmlConfigPersistenceManager.writeElement(xml, element, (String) event.data);
      }
    };
  }

  private UpdateEventListener newRuntimeLoggingWriter() {
    return new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        XmlConfigEvent event = (XmlConfigEvent) e;
        final String element = XmlConfigEvent.m_elementNames[event.type];
        XmlObject xml = ensureClientRuntimeLoggingElement((XmlObject) event.variable);
        XmlConfigPersistenceManager.writeElement(xml, element, (String) event.data);
      }
    };
  }

  private UpdateEventListener newGCWriter() {
    return new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        XmlConfigEvent event = (XmlConfigEvent) e;
        final String element = XmlConfigEvent.m_elementNames[event.type];
        XmlObject xml = ensureServerDsoGCElement((XmlObject) event.variable);
        XmlConfigPersistenceManager.writeElement(xml, element, (String) event.data);
      }
    };
  }

  private UpdateEventListener newWriter() {
    return new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        XmlConfigEvent event = (XmlConfigEvent) e;
        final String element = XmlConfigEvent.m_elementNames[event.type];
        XmlObject xml = event.element;
        XmlConfigPersistenceManager.writeElement(xml, element, (String) event.data);
      }
    };
  }

  private void registerContextEventListeners() {
    m_createServerListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent data) {
        if (m_config.getServers() == null) m_config.addNewServers();
        Server server = m_config.getServers().addNewServer();
        m_newServerObserver.fireUpdateEvent(new XmlConfigEvent(server, XmlConfigEvent.NEW_SERVER));
      }
    };
    m_deleteServerListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent data) {
        XmlObject server = ((XmlConfigEvent) data).element;
        Server[] servers = m_config.getServers().getServerArray();
        for (int i = 0; i < servers.length; i++) {
          if (servers[i] == server) {
            m_config.getServers().removeServer(i);
            break;
          }
        }
        m_removeServerObserver.fireUpdateEvent(new XmlConfigEvent(server, XmlConfigEvent.REMOVE_SERVER));
      }
    };
  }

  private void doAction(XmlAction action, int type) {
    XmlConfigEvent event = action.getEvent();
    switch (type) {
      case XmlConfigEvent.XML_STRUCTURE_CHANGED:
        action.exec(m_xmlStructureChangedObserver, m_xmlStructureChangedListener);
        break;
      case XmlConfigEvent.SERVER_NAME:
        action.exec(m_serverNameObserver, m_serverNameListener);
        break;
      case XmlConfigEvent.SERVER_HOST:
        action.exec(m_serverHostObserver, m_serverHostListener);
        break;
      case XmlConfigEvent.SERVER_DSO_PORT:
        action.exec(m_serverDSOPortObserver, m_serverDsoPortListener);
        break;
      case XmlConfigEvent.SERVER_JMX_PORT:
        action.exec(m_serverJMXPortObserver, m_serverJmxPortListener);
        break;
      case XmlConfigEvent.SERVER_DATA:
        action.exec(m_serverDataObserver, m_serverDataListener);
        break;
      case XmlConfigEvent.SERVER_LOGS:
        action.exec(m_serverLogsObserver, m_serverLogsListener);
        break;
      case XmlConfigEvent.SERVER_PERSIST:
        swapServerPersistEvent(event);
        action.exec(m_serverPersistObserver, m_serverPersistListener);
        break;
      case XmlConfigEvent.SERVER_GC:
        swapServerGCEvent(event);
        action.exec(m_serverGCObserver, m_serverGCListener);
        break;
      case XmlConfigEvent.SERVER_GC_VERBOSE:
        swapServerGCEvent(event);
        action.exec(m_serverVerboseObserver, m_serverVerboseListener);
        break;
      case XmlConfigEvent.SERVER_GC_INTERVAL:
        swapServerGCEvent(event);
        action.exec(m_serverGCIntervalObserver, m_serverGCIntervalListener);
        break;
      case XmlConfigEvent.CLIENT_LOGS:
        action.exec(m_clientLogsObserver, m_clientLogsListener);
        break;
      case XmlConfigEvent.CLIENT_DSO_REFLECTION_ENABLED:
        action.exec(m_clientDSOReflectionEnabledObserver, m_clientDSOReflectionEnabledListener);
        break;
      case XmlConfigEvent.CLIENT_CLASS:
        swapClientInstLoggingEvent(event);
        action.exec(m_clientClassObserver, m_clientClassListener);
        break;
      case XmlConfigEvent.CLIENT_HIERARCHY:
        swapClientInstLoggingEvent(event);
        action.exec(m_clientHierarchyObserver, m_clientHierarchyListener);
        break;
      case XmlConfigEvent.CLIENT_LOCKS:
        swapClientInstLoggingEvent(event);
        action.exec(m_clientLocksObserver, m_clientLocksListener);
        break;
      case XmlConfigEvent.CLIENT_TRANSIENT_ROOT:
        swapClientInstLoggingEvent(event);
        action.exec(m_clientTransientRootObserver, m_clientTransientRootListener);
        break;
      case XmlConfigEvent.CLIENT_DISTRIBUTED_METHODS:
        swapClientInstLoggingEvent(event);
        action.exec(m_clientDistributedMethodsObserver, m_clientDistributedMethodsListener);
        break;
      case XmlConfigEvent.CLIENT_ROOTS:
        swapClientInstLoggingEvent(event);
        action.exec(m_clientRootsObserver, m_clientRootsListener);
        break;
      case XmlConfigEvent.CLIENT_LOCK_DEBUG:
        swapClientRuntimeLoggingEvent(event);
        action.exec(m_clientLockDebugObserver, m_clientLockDebugListener);
        break;
      case XmlConfigEvent.CLIENT_DISTRIBUTED_METHOD_DEBUG:
        swapClientRuntimeLoggingEvent(event);
        action.exec(m_clientDistributedMethodDebugObserver, m_clientDistributedMethodDebugListener);
        break;
      case XmlConfigEvent.CLIENT_FIELD_CHANGE_DEBUG:
        swapClientRuntimeLoggingEvent(event);
        action.exec(m_clientFieldChangeDebugObserver, m_clientFieldChangeDebugListener);
        break;
      case XmlConfigEvent.CLIENT_NON_PORTABLE_WARNING:
        swapClientRuntimeLoggingEvent(event);
        action.exec(m_clientNonPortableWarningObserver, m_clientNonPortableWarningListener);
        break;
      case XmlConfigEvent.CLIENT_PARTIAL_INSTRUMENTATION:
        swapClientRuntimeLoggingEvent(event);
        action.exec(m_clientPartialInstrumentationObserver, m_clientPartialInstrumentationListener);
        break;
      case XmlConfigEvent.CLIENT_WAIT_NOTIFY_DEBUG:
        swapClientRuntimeLoggingEvent(event);
        action.exec(m_clientWaitNotifyDebugObserver, m_clientWaitNofifyDebugListener);
        break;
      case XmlConfigEvent.CLIENT_NEW_OBJECT_DEBUG:
        swapClientRuntimeLoggingEvent(event);
        action.exec(m_clientNewObjectDebugObserver, m_clientNewObjectDebugListener);
        break;
      case XmlConfigEvent.CLIENT_AUTOLOCK_DETAILS:
        swapClientRuntimeOutputEvent(event);
        action.exec(m_clientAutolockDetailsObserver, m_clientAutolockDetialsListener);
        break;
      case XmlConfigEvent.CLIENT_CALLER:
        swapClientRuntimeOutputEvent(event);
        action.exec(m_clientCallerObserver, m_clientCallerListener);
        break;
      case XmlConfigEvent.CLIENT_FULL_STACK:
        swapClientRuntimeOutputEvent(event);
        action.exec(m_clientFullStackObserver, m_clientFullStackListener);
        break;
      case XmlConfigEvent.CLIENT_FIND_NEEDED_INCLUDES:
        swapClientRuntimeOutputEvent(event);
        action.exec(m_clientFindNeededIncludesObserver, m_clientFindNeededIncludesListener);
        break;
      case XmlConfigEvent.CLIENT_FAULT_COUNT:
        swapClientDsoEvent(event);
        action.exec(m_clientFaultCountObserver, m_clientFaultCountListener);
        break;
      // NEW and REMOVE EVENTS - Notified after corresponding creation or deletion
      case XmlConfigEvent.NEW_SERVER:
        action.exec(m_newServerObserver, null);
        break;
      case XmlConfigEvent.REMOVE_SERVER:
        action.exec(m_removeServerObserver, null);
        break;

      default:
        break;
    }
  }

  private void swapServerGCEvent(XmlConfigEvent event) {
    if (event != null) {
      event.variable = event.element; // <-- NOTE: Server element moved to variable field
      event.element = ensureServerDsoGCElement(event.element);
    }
  }

  private void swapServerPersistEvent(XmlConfigEvent event) {
    if (event != null) {
      event.variable = event.element; // <-- NOTE: Server element moved to variable field
      event.element = ensureServerDsoPersistElement(event.element);
    }
  }

  private void swapClientInstLoggingEvent(XmlConfigEvent event) {
    if (event != null) {
      event.variable = event.element; // <-- NOTE: Client element moved to variable field
      event.element = ensureClientInstrumentationLoggingElement(event.element);
    }
  }

  private void swapClientRuntimeLoggingEvent(XmlConfigEvent event) {
    if (event != null) {
      event.variable = event.element; // <-- NOTE: Client element moved to variable field
      event.element = ensureClientRuntimeLoggingElement(event.element);
    }
  }

  private void swapClientRuntimeOutputEvent(XmlConfigEvent event) {
    if (event != null) {
      event.variable = event.element; // <-- NOTE: Client element moved to variable field
      event.element = ensureClientRuntimeOutputOptionsElement(event.element);
    }
  }

  private void swapClientDsoEvent(XmlConfigEvent event) {
    if (event != null) {
      event.variable = event.element; // <-- NOTE: Client element moved to variable field
      event.element = ensureClientDsoElement(event.element);
    }
  }

  private void creationEvent(XmlConfigEvent event) {
    switch (event.type) {
      case XmlConfigEvent.CREATE_SERVER:
        m_createServerListener.handleUpdate(event);
        break;
      case XmlConfigEvent.DELETE_SERVER:
        m_deleteServerListener.handleUpdate(event);
        break;

      default:
        break;
    }
  }

  private XmlObject ensureServerDsoElement(XmlObject server) {
    return XmlConfigPersistenceManager.ensureXml(server, Server.class, XmlConfigEvent.PARENT_ELEM_DSO);
  }

  private XmlObject ensureServerDsoGCElement(XmlObject server) {
    XmlObject dso = ensureServerDsoElement(server);
    return XmlConfigPersistenceManager.ensureXml(dso, DsoServerData.class, XmlConfigEvent.PARENT_ELEM_GC);
  }

  private XmlObject ensureServerDsoPersistElement(XmlObject server) {
    XmlObject dso = ensureServerDsoElement(server);
    return XmlConfigPersistenceManager.ensureXml(dso, DsoServerData.class, XmlConfigEvent.PARENT_ELEM_PERSIST);
  }

  private XmlObject ensureClientDsoElement(XmlObject client) {
    return XmlConfigPersistenceManager.ensureXml(client, Client.class, XmlConfigEvent.PARENT_ELEM_DSO);
  }

  private XmlObject ensureClientDsoDebuggingElement(XmlObject client) {
    XmlObject dso = ensureClientDsoElement(client);
    return XmlConfigPersistenceManager.ensureXml(dso, DsoClientData.class, XmlConfigEvent.PARENT_ELEM_DEBUGGING);
  }

  private XmlObject ensureClientInstrumentationLoggingElement(XmlObject client) {
    XmlObject debugging = ensureClientDsoDebuggingElement(client);
    return XmlConfigPersistenceManager.ensureXml(debugging, DsoClientDebugging.class,
        XmlConfigEvent.PARENT_ELEM_INSTRUMENTATION_LOGGING);
  }

  private XmlObject ensureClientRuntimeOutputOptionsElement(XmlObject client) {
    XmlObject debugging = ensureClientDsoDebuggingElement(client);
    return XmlConfigPersistenceManager.ensureXml(debugging, DsoClientDebugging.class,
        XmlConfigEvent.PARENT_ELEM_RUNTIME_OUTPUT_OPTIONS);
  }

  private XmlObject ensureClientRuntimeLoggingElement(XmlObject client) {
    XmlObject debugging = ensureClientDsoDebuggingElement(client);
    return XmlConfigPersistenceManager.ensureXml(debugging, DsoClientDebugging.class,
        XmlConfigEvent.PARENT_ELEM_RUNTIME_LOGGING);
  }

  // --------------------------------------------------------------------------------

  private class MulticastListenerPair {
    EventMulticaster    multicaster;
    UpdateEventListener listener;
  }

  // --------------------------------------------------------------------------------

  private interface XmlAction {
    void exec(EventMulticaster multicaster, UpdateEventListener source);

    XmlConfigEvent getEvent();
  }
}
