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
  // context new/remove element observers
  private final EventMulticaster                       m_newServerObserver;
  private final EventMulticaster                       m_removeServerObserver;
  // context create/delete listeners
  private UpdateEventListener                          m_createServerListener;
  private UpdateEventListener                          m_deleteServerListener;

  private static final Map<IProject, XmlConfigContext> m_contexts   = new HashMap<IProject, XmlConfigContext>();
  private final XmlConfigPersistenceManager            m_persistenceManager;
  private final TcConfig                               m_config;
  private final Map<SWTComponentModel, List>           m_componentModels;

  private XmlConfigContext(IProject project) {
    this.m_persistenceManager = new XmlConfigPersistenceManager();
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
    // "new" and "remove" element observers
    this.m_newServerObserver = new EventMulticaster();
    this.m_removeServerObserver = new EventMulticaster();
    init();
  }

  public static synchronized XmlConfigContext getInstance(IProject project) {
    if (m_contexts.containsKey(project)) return m_contexts.get(project);
    return new XmlConfigContext(project);
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

    addListener(m_serverNameListener = newWriter(), XmlConfigEvent.SERVER_NAME);
    addListener(m_serverHostListener = newWriter(), XmlConfigEvent.SERVER_HOST);
    addListener(m_serverDsoPortListener = newWriter(), XmlConfigEvent.SERVER_DSO_PORT);
    addListener(m_serverJmxPortListener = newWriter(), XmlConfigEvent.SERVER_JMX_PORT);
    addListener(m_serverDataListener = newWriter(), XmlConfigEvent.SERVER_DATA);
    addListener(m_serverDataListener = newWriter(), XmlConfigEvent.SERVER_LOGS);
  }

  private UpdateEventListener newWriter() {
    return new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        XmlConfigEvent event = (XmlConfigEvent) e;
        final String element = XmlConfigEvent.elementNames[event.type];
        XmlObject server = event.element;
        m_persistenceManager.writeElement(server, element, (String) event.data);
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
        action.exec(m_serverPersistObserver, m_serverPersistListener);
        break;
      case XmlConfigEvent.SERVER_GC:
        action.exec(m_serverGCObserver, m_serverGCListener);
        break;
      case XmlConfigEvent.SERVER_VERBOSE:
        action.exec(m_serverVerboseObserver, m_serverVerboseListener);
        break;
      case XmlConfigEvent.SERVER_GC_INTERVAL:
        action.exec(m_serverGCIntervalObserver, m_serverGCIntervalListener);
        break;
      // NEW/REMOVE EVENTS - notified after corresponding creation/deletion
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

  /**
   * Notify <tt>XmlContext</tt> that a change has occured
   */
  public void notifyListeners(XmlConfigEvent event) {
    if (event.type < 0) {
      creationEvent(event);
      return;
    } else if (event.type > XmlConfigEvent.ALT_RANGE_CONSTANT) return;
    notifyListeners(event, false);
  }

  /**
   * Update listeners with current XmlContext state. This should be used to initialize object state.
   */
  public void updateListeners(XmlConfigEvent event) {
    event.data = m_persistenceManager.readElement(event.element, XmlConfigEvent.elementNames[event.type]);
    notifyListeners(event, true);
  }

  private void notifyListeners(final XmlConfigEvent event, final boolean ignoreContext) {
    doAction(new XmlAction() {
      public void exec(EventMulticaster multicaster, UpdateEventListener source) {
        if (ignoreContext) event.source = source;
        multicaster.fireUpdateEvent(event);
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
    }, type);
  }

  // HELPERS
  // public String[] getListDefaults() {
  // return null;
  // }
  //
  // public int[] getRangeDefaults() {
  // return null;
  // }
  //
  // public boolean isRequired() {
  // return false;
  // }

  // --------------------------------------------------------------------------------

  private class MulticastListenerPair {
    EventMulticaster    multicaster;
    UpdateEventListener listener;
  }

  // --------------------------------------------------------------------------------

  private interface XmlAction {
    void exec(EventMulticaster multicaster, UpdateEventListener source);
  }
}
