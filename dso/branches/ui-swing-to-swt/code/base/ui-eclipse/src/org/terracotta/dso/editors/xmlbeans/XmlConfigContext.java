/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.XmlAnyURI;
import org.apache.xmlbeans.XmlObject;
import org.eclipse.core.resources.IProject;
import org.terracotta.dso.TcPlugin;
import org.terracotta.ui.util.SWTComponentModel;

import com.tc.util.event.EventMulticaster;
import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.Autolock;
import com.terracottatech.config.Client;
import com.terracottatech.config.DistributedMethods;
import com.terracottatech.config.DsoClientData;
import com.terracottatech.config.DsoClientDebugging;
import com.terracottatech.config.DsoServerData;
import com.terracottatech.config.LockLevel;
import com.terracottatech.config.Module;
import com.terracottatech.config.NamedLock;
import com.terracottatech.config.Root;
import com.terracottatech.config.Roots;
import com.terracottatech.config.Server;
import com.terracottatech.config.TransientFields;
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
  private final EventMulticaster                       m_clientNonPortableDumpObserver;
  private UpdateEventListener                          m_clientNonPortableDumpListener;
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
  private final EventMulticaster                       m_clientFaultCountObserver;
  private UpdateEventListener                          m_clientFaultCountListener;
  private final EventMulticaster                       m_rootsFieldObserver;
  private UpdateEventListener                          m_rootsFieldListener;
  private final EventMulticaster                       m_rootsNameObserver;
  private UpdateEventListener                          m_rootsNameListener;
  private final EventMulticaster                       m_transientFieldsObserver;
  private UpdateEventListener                          m_transientFieldsListener;
  private final EventMulticaster                       m_distributedMethodsObserver;
  private UpdateEventListener                          m_distributedMethodsListener;
  private final EventMulticaster                       m_bootClassesObserver;
  private UpdateEventListener                          m_bootClassesListener;
  private final EventMulticaster                       m_locksAutoMethodObserver;
  private UpdateEventListener                          m_locksAutoMethodListener;
  private final EventMulticaster                       m_locksAutoLevelObserver;
  private UpdateEventListener                          m_locksAutoLevelListener;
  private final EventMulticaster                       m_locksNamedNameObserver;
  private UpdateEventListener                          m_locksNamedNameListener;
  private final EventMulticaster                       m_locksNamedMethodObserver;
  private UpdateEventListener                          m_locksNamedMethodListener;
  private final EventMulticaster                       m_locksNamedLevelObserver;
  private UpdateEventListener                          m_locksNamedLevelListener;
  // context new/remove element observers
  private final EventMulticaster                       m_newServerObserver;
  private final EventMulticaster                       m_removeServerObserver;
  private final EventMulticaster                       m_newClientModuleRepoObserver;
  private final EventMulticaster                       m_removeClientModuleRepoObserver;
  private final EventMulticaster                       m_newClientModuleObserver;
  private final EventMulticaster                       m_removeClientModuleObserver;
  private final EventMulticaster                       m_newRootObserver;
  private final EventMulticaster                       m_removeRootObserver;
  private final EventMulticaster                       m_newTransientFieldObserver;
  private final EventMulticaster                       m_removeTransientFieldObserver;
  private final EventMulticaster                       m_newDistributedMethodObserver;
  private final EventMulticaster                       m_removeDistributedMethodObserver;
  private final EventMulticaster                       m_newBootClassObserver;
  private final EventMulticaster                       m_removeBootClassObserver;
  private final EventMulticaster                       m_newLockAutoObserver;
  private final EventMulticaster                       m_removeLockAutoObserver;
  private final EventMulticaster                       m_newLockNamedObserver;
  private final EventMulticaster                       m_removeLockNamedObserver;
  // context create/delete listeners
  private UpdateEventListener                          m_createServerListener;
  private UpdateEventListener                          m_deleteServerListener;
  private UpdateEventListener                          m_createClientModuleRepoListener;
  private UpdateEventListener                          m_deleteClientModuleRepoListener;
  private UpdateEventListener                          m_createClientModuleListener;
  private UpdateEventListener                          m_deleteClientModuleListener;
  private UpdateEventListener                          m_createRootListener;
  private UpdateEventListener                          m_deleteRootListener;
  private UpdateEventListener                          m_createTransientFieldListener;
  private UpdateEventListener                          m_deleteTransientFieldListener;
  private UpdateEventListener                          m_createDistributedMethodListener;
  private UpdateEventListener                          m_deleteDistributedMethodListener;
  private UpdateEventListener                          m_createBootClassListener;
  private UpdateEventListener                          m_deleteBootClassListener;
  private UpdateEventListener                          m_createLockAutoListener;
  private UpdateEventListener                          m_deleteLockAutoListener;
  private UpdateEventListener                          m_createLockNamedListener;
  private UpdateEventListener                          m_deleteLockNamedListener;

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
    this.m_clientClassObserver = new EventMulticaster();
    this.m_clientHierarchyObserver = new EventMulticaster();
    this.m_clientLocksObserver = new EventMulticaster();
    this.m_clientTransientRootObserver = new EventMulticaster();
    this.m_clientDistributedMethodsObserver = new EventMulticaster();
    this.m_clientRootsObserver = new EventMulticaster();
    this.m_clientLockDebugObserver = new EventMulticaster();
    this.m_clientDistributedMethodDebugObserver = new EventMulticaster();
    this.m_clientFieldChangeDebugObserver = new EventMulticaster();
    this.m_clientNonPortableDumpObserver = new EventMulticaster();
    this.m_clientWaitNotifyDebugObserver = new EventMulticaster();
    this.m_clientNewObjectDebugObserver = new EventMulticaster();
    this.m_clientAutolockDetailsObserver = new EventMulticaster();
    this.m_clientCallerObserver = new EventMulticaster();
    this.m_clientFullStackObserver = new EventMulticaster();
    this.m_clientFaultCountObserver = new EventMulticaster();
    this.m_rootsFieldObserver = new EventMulticaster();
    this.m_rootsNameObserver = new EventMulticaster();
    this.m_transientFieldsObserver = new EventMulticaster();
    this.m_distributedMethodsObserver = new EventMulticaster();
    this.m_bootClassesObserver = new EventMulticaster();
    this.m_locksAutoMethodObserver = new EventMulticaster();
    this.m_locksAutoLevelObserver = new EventMulticaster();
    this.m_locksNamedNameObserver = new EventMulticaster();
    this.m_locksNamedMethodObserver = new EventMulticaster();
    this.m_locksNamedLevelObserver = new EventMulticaster();
    // "new" and "remove" element observers
    this.m_newServerObserver = new EventMulticaster();
    this.m_removeServerObserver = new EventMulticaster();
    this.m_newClientModuleRepoObserver = new EventMulticaster();
    this.m_removeClientModuleRepoObserver = new EventMulticaster();
    this.m_newClientModuleObserver = new EventMulticaster();
    this.m_removeClientModuleObserver = new EventMulticaster();
    this.m_newRootObserver = new EventMulticaster();
    this.m_removeRootObserver = new EventMulticaster();
    this.m_newTransientFieldObserver = new EventMulticaster();
    this.m_removeTransientFieldObserver = new EventMulticaster();
    this.m_newDistributedMethodObserver = new EventMulticaster();
    this.m_removeDistributedMethodObserver = new EventMulticaster();
    this.m_newBootClassObserver = new EventMulticaster();
    this.m_removeBootClassObserver = new EventMulticaster();
    this.m_newLockAutoObserver = new EventMulticaster();
    this.m_removeLockAutoObserver = new EventMulticaster();
    this.m_newLockNamedObserver = new EventMulticaster();
    this.m_removeLockNamedObserver = new EventMulticaster();
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
    addListener(m_clientNonPortableDumpListener = newRuntimeLoggingWriter(), XmlConfigEvent.CLIENT_NON_PORTABLE_DUMP);
    addListener(m_clientWaitNofifyDebugListener = newRuntimeLoggingWriter(), XmlConfigEvent.CLIENT_WAIT_NOTIFY_DEBUG);
    addListener(m_clientNewObjectDebugListener = newRuntimeLoggingWriter(), XmlConfigEvent.CLIENT_NEW_OBJECT_DEBUG);
    // client runtime output
    addListener(m_clientAutolockDetialsListener = newRuntimeOutputWriter(), XmlConfigEvent.CLIENT_AUTOLOCK_DETAILS);
    addListener(m_clientCallerListener = newRuntimeOutputWriter(), XmlConfigEvent.CLIENT_CALLER);
    addListener(m_clientFullStackListener = newRuntimeOutputWriter(), XmlConfigEvent.CLIENT_FULL_STACK);
    addListener(m_clientFaultCountListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        XmlConfigEvent event = (XmlConfigEvent) e;
        final String element = XmlConfigEvent.m_elementNames[event.type];
        XmlObject xml = ensureClientDsoElement((XmlObject) event.variable);
        XmlConfigPersistenceManager.writeElement(xml, element, (String) event.data);
      }
    }, XmlConfigEvent.CLIENT_FAULT_COUNT);
    // dso applications
    addListener(m_rootsFieldListener = newWriter(), XmlConfigEvent.ROOTS_FIELD);
    addListener(m_rootsNameListener = newWriter(), XmlConfigEvent.ROOTS_NAME);
    addListener(m_transientFieldsListener = newTransientFieldsWriter(), XmlConfigEvent.TRANSIENT_FIELD);
    addListener(m_distributedMethodsListener = newDistributedMethodsWriter(), XmlConfigEvent.DISTRIBUTED_METHOD);
    addListener(m_bootClassesListener = newBootClassesWriter(), XmlConfigEvent.BOOT_CLASS);
    addListener(m_locksAutoMethodListener = newLocksAutoWriter(), XmlConfigEvent.LOCKS_AUTO_METHOD);
    addListener(m_locksAutoLevelListener = newLocksAutoWriter(), XmlConfigEvent.LOCKS_AUTO_LEVEL);
    addListener(m_locksNamedNameListener = newLocksNamedWriter(), XmlConfigEvent.LOCKS_NAMED_NAME);
    addListener(m_locksNamedMethodListener = newLocksNamedWriter(), XmlConfigEvent.LOCKS_NAMED_METHOD);
    addListener(m_locksNamedLevelListener = newLocksNamedWriter(), XmlConfigEvent.LOCKS_NAMED_LEVEL);
  }

  private UpdateEventListener newLocksAutoWriter() {
    return new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        XmlConfigEvent event = (XmlConfigEvent) e;
        final String element = XmlConfigEvent.m_elementNames[event.type];
        XmlConfigPersistenceManager.writeElement(event.element, element, (String) event.data);
      }
    };
  }

  private UpdateEventListener newLocksNamedWriter() {
    return new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        XmlConfigEvent event = (XmlConfigEvent) e;
        final String element = XmlConfigEvent.m_elementNames[event.type];
        XmlConfigPersistenceManager.writeElement(event.element, element, (String) event.data);
      }
    };
  }

  private UpdateEventListener newBootClassesWriter() {
    return new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        XmlConfigEvent event = (XmlConfigEvent) e;
        AdditionalBootJarClasses classes = (AdditionalBootJarClasses) event.element;
        classes.setIncludeArray(event.index, (String) event.data);
      }
    };
  }

  private UpdateEventListener newDistributedMethodsWriter() {
    return new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        XmlConfigEvent event = (XmlConfigEvent) e;
        DistributedMethods.MethodExpression method = (DistributedMethods.MethodExpression) event.element;
        method.setStringValue((String) event.data);
      }
    };
  }

  private UpdateEventListener newTransientFieldsWriter() {
    return new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        XmlConfigEvent event = (XmlConfigEvent) e;
        TransientFields fields = (TransientFields) event.element;
        fields.setFieldNameArray(event.index, (String) event.data);
      }
    };
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
    m_createClientModuleListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        String[] values = (String[]) e.data;
        Module module = m_config.getClients().getModules().addNewModule();
        module.setName(values[0]);
        module.setVersion(values[1]);
        XmlConfigEvent event = new XmlConfigEvent(values, null, module, XmlConfigEvent.NEW_CLIENT_MODULE);
        m_newClientModuleObserver.fireUpdateEvent(event);
      }
    };
    m_deleteClientModuleListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        int index = ((XmlConfigEvent) e).index;
        m_config.getClients().getModules().removeModule(index);
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.REMOVE_CLIENT_MODULE);
        event.index = index;
        m_removeClientModuleObserver.fireUpdateEvent(event);
      }
    };
    m_createClientModuleRepoListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        XmlAnyURI repo = m_config.getClients().getModules().addNewRepository();
        repo.setStringValue((String) e.data);
        XmlConfigEvent event = new XmlConfigEvent(e.data, null, repo, XmlConfigEvent.NEW_CLIENT_MODULE_REPO);
        m_newClientModuleRepoObserver.fireUpdateEvent(event);
      }
    };
    m_deleteClientModuleRepoListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        int index = ((XmlConfigEvent) e).index;
        m_config.getClients().getModules().removeRepository(index);
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.REMOVE_CLIENT_MODULE_REPO);
        event.index = index;
        m_removeClientModuleRepoObserver.fireUpdateEvent(event);
      }
    };
    m_createRootListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        Root root = m_config.getApplication().getDso().getRoots().addNewRoot();
        root.setFieldName(((String[]) e.data)[0]);
        root.setRootName(((String[]) e.data)[1]);
        m_newRootObserver.fireUpdateEvent(new XmlConfigEvent(root, XmlConfigEvent.NEW_ROOT));
      }
    };
    m_deleteRootListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        int index = ((XmlConfigEvent) e).index;
        Roots roots = m_config.getApplication().getDso().getRoots();
        roots.removeRoot(index);
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.REMOVE_ROOT);
        event.index = index;
        m_removeRootObserver.fireUpdateEvent(event);
      }
    };
    m_createTransientFieldListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        m_config.getApplication().getDso().getTransientFields().addFieldName((String) e.data);
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.NEW_TRANSIENT_FIELD);
        event.data = e.data;
        m_newTransientFieldObserver.fireUpdateEvent(event);
      }
    };
    m_deleteTransientFieldListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        int index = ((XmlConfigEvent) e).index;
        TransientFields fields = m_config.getApplication().getDso().getTransientFields();
        fields.removeFieldName(index);
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.REMOVE_TRANSIENT_FIELD);
        event.index = index;
        m_removeTransientFieldObserver.fireUpdateEvent(event);
      }
    };
    m_createDistributedMethodListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        DistributedMethods.MethodExpression expr = m_config.getApplication().getDso().getDistributedMethods()
            .addNewMethodExpression();
        expr.setStringValue((String) e.data);
        m_newDistributedMethodObserver.fireUpdateEvent(new XmlConfigEvent(expr, XmlConfigEvent.NEW_DISTRIBUTED_METHOD));
      }
    };
    m_deleteDistributedMethodListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        int index = ((XmlConfigEvent) e).index;
        m_config.getApplication().getDso().getDistributedMethods().removeMethodExpression(index);
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.REMOVE_DISTRIBUTED_METHOD);
        event.index = index;
        m_removeDistributedMethodObserver.fireUpdateEvent(event);
      }
    };
    m_createBootClassListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        m_config.getApplication().getDso().getAdditionalBootJarClasses().addInclude((String) e.data);
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.NEW_BOOT_CLASS);
        event.data = e.data;
        m_newBootClassObserver.fireUpdateEvent(event);
      }
    };
    m_deleteBootClassListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        int index = ((XmlConfigEvent) e).index;
        m_config.getApplication().getDso().getAdditionalBootJarClasses().removeInclude(index);
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.REMOVE_BOOT_CLASS);
        event.index = index;
        m_removeBootClassObserver.fireUpdateEvent(event);
      }
    };
    m_createLockAutoListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        String[] values = (String[]) e.data;
        Autolock lock = m_config.getApplication().getDso().getLocks().addNewAutolock();
        lock.setMethodExpression(values[0]);
        lock.setLockLevel(LockLevel.Enum.forString(values[1]));
        m_newLockAutoObserver.fireUpdateEvent(new XmlConfigEvent(lock, XmlConfigEvent.NEW_LOCK_AUTO));
      }
    };
    m_deleteLockAutoListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        int index = ((XmlConfigEvent) e).index;
        m_config.getApplication().getDso().getLocks().removeAutolock(index);
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.REMOVE_LOCK_AUTO);
        event.index = index;
        m_removeLockAutoObserver.fireUpdateEvent(event);
      }
    };
    m_createLockNamedListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        String[] values = (String[]) e.data;
        NamedLock lock = m_config.getApplication().getDso().getLocks().addNewNamedLock();
        lock.setLockName(values[0]);
        lock.setMethodExpression(values[1]);
        lock.setLockLevel(LockLevel.Enum.forString(values[2]));
        m_newLockNamedObserver.fireUpdateEvent(new XmlConfigEvent(lock, XmlConfigEvent.NEW_LOCK_NAMED));
      }
    };
    m_deleteLockNamedListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        int index = ((XmlConfigEvent) e).index;
        m_config.getApplication().getDso().getLocks().removeNamedLock(index);
        XmlConfigEvent event = new XmlConfigEvent(XmlConfigEvent.REMOVE_LOCK_NAMED);
        event.index = index;
        m_removeLockNamedObserver.fireUpdateEvent(event);
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
      case XmlConfigEvent.CLIENT_NON_PORTABLE_DUMP:
        swapClientRuntimeLoggingEvent(event);
        action.exec(m_clientNonPortableDumpObserver, m_clientNonPortableDumpListener);
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
      case XmlConfigEvent.CLIENT_FAULT_COUNT:
        swapClientDsoEvent(event);
        action.exec(m_clientFaultCountObserver, m_clientFaultCountListener);
        break;
      case XmlConfigEvent.ROOTS_FIELD:
        action.exec(m_rootsFieldObserver, m_rootsFieldListener);
        break;
      case XmlConfigEvent.ROOTS_NAME:
        action.exec(m_rootsNameObserver, m_rootsNameListener);
        break;
      case XmlConfigEvent.TRANSIENT_FIELD:
        action.exec(m_transientFieldsObserver, m_transientFieldsListener);
        break;
      case XmlConfigEvent.DISTRIBUTED_METHOD:
        action.exec(m_distributedMethodsObserver, m_distributedMethodsListener);
        break;
      case XmlConfigEvent.BOOT_CLASS:
        action.exec(m_bootClassesObserver, m_bootClassesListener);
        break;
      case XmlConfigEvent.LOCKS_AUTO_METHOD:
        action.exec(m_locksAutoMethodObserver, m_locksAutoMethodListener);
        break;
      case XmlConfigEvent.LOCKS_AUTO_LEVEL:
        action.exec(m_locksAutoLevelObserver, m_locksAutoLevelListener);
        break;
      case XmlConfigEvent.LOCKS_NAMED_NAME:
        action.exec(m_locksNamedNameObserver, m_locksNamedNameListener);
        break;
      case XmlConfigEvent.LOCKS_NAMED_METHOD:
        action.exec(m_locksNamedMethodObserver, m_locksNamedMethodListener);
        break;
      case XmlConfigEvent.LOCKS_NAMED_LEVEL:
        action.exec(m_locksNamedLevelObserver, m_locksNamedLevelListener);
        break;
      // NEW and REMOVE EVENTS - Notified after corresponding creation or deletion
      case XmlConfigEvent.NEW_SERVER:
        action.exec(m_newServerObserver, null);
        break;
      case XmlConfigEvent.REMOVE_SERVER:
        action.exec(m_removeServerObserver, null);
        break;
      case XmlConfigEvent.NEW_CLIENT_MODULE:
        action.exec(m_newClientModuleObserver, null);
        break;
      case XmlConfigEvent.REMOVE_CLIENT_MODULE:
        action.exec(m_removeClientModuleObserver, null);
        break;
      case XmlConfigEvent.NEW_CLIENT_MODULE_REPO:
        swapClientDsoEvent(event);
        action.exec(m_newClientModuleRepoObserver, null);
        break;
      case XmlConfigEvent.REMOVE_CLIENT_MODULE_REPO:
        swapClientDsoEvent(event);
        action.exec(m_removeClientModuleRepoObserver, null);
        break;
      case XmlConfigEvent.NEW_ROOT:
        action.exec(m_newRootObserver, null);
        break;
      case XmlConfigEvent.REMOVE_ROOT:
        action.exec(m_removeRootObserver, null);
        break;
      case XmlConfigEvent.NEW_TRANSIENT_FIELD:
        action.exec(m_newTransientFieldObserver, null);
        break;
      case XmlConfigEvent.REMOVE_TRANSIENT_FIELD:
        action.exec(m_removeTransientFieldObserver, null);
        break;
      case XmlConfigEvent.NEW_DISTRIBUTED_METHOD:
        action.exec(m_newDistributedMethodObserver, null);
        break;
      case XmlConfigEvent.REMOVE_DISTRIBUTED_METHOD:
        action.exec(m_removeDistributedMethodObserver, null);
        break;
      case XmlConfigEvent.NEW_BOOT_CLASS:
        action.exec(m_newBootClassObserver, null);
        break;
      case XmlConfigEvent.REMOVE_BOOT_CLASS:
        action.exec(m_removeBootClassObserver, null);
        break;
      case XmlConfigEvent.NEW_LOCK_AUTO:
        action.exec(m_newLockAutoObserver, null);
        break;
      case XmlConfigEvent.REMOVE_LOCK_AUTO:
        action.exec(m_removeLockAutoObserver, null);
        break;
      case XmlConfigEvent.NEW_LOCK_NAMED:
        action.exec(m_newLockNamedObserver, null);
        break;
      case XmlConfigEvent.REMOVE_LOCK_NAMED:
        action.exec(m_removeLockNamedObserver, null);
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
      case XmlConfigEvent.CREATE_CLIENT_MODULE:
        m_createClientModuleListener.handleUpdate(event);
        break;
      case XmlConfigEvent.DELETE_CLIENT_MODULE:
        m_deleteClientModuleListener.handleUpdate(event);
        break;
      case XmlConfigEvent.CREATE_CLIENT_MODULE_REPO:
        m_createClientModuleRepoListener.handleUpdate(event);
        break;
      case XmlConfigEvent.DELETE_CLIENT_MODULE_REPO:
        m_deleteClientModuleRepoListener.handleUpdate(event);
        break;
      case XmlConfigEvent.CREATE_ROOT:
        m_createRootListener.handleUpdate(event);
        break;
      case XmlConfigEvent.DELETE_ROOT:
        m_deleteRootListener.handleUpdate(event);
        break;
      case XmlConfigEvent.CREATE_TRANSIENT_FIELD:
        m_createTransientFieldListener.handleUpdate(event);
        break;
      case XmlConfigEvent.DELETE_TRANSIENT_FIELD:
        m_deleteTransientFieldListener.handleUpdate(event);
        break;
      case XmlConfigEvent.CREATE_DISTRIBUTED_METHOD:
        m_createDistributedMethodListener.handleUpdate(event);
        break;
      case XmlConfigEvent.DELETE_DISTRIBUTED_METHOD:
        m_deleteDistributedMethodListener.handleUpdate(event);
        break;
      case XmlConfigEvent.CREATE_BOOT_CLASS:
        m_createBootClassListener.handleUpdate(event);
        break;
      case XmlConfigEvent.DELETE_BOOT_CLASS:
        m_deleteBootClassListener.handleUpdate(event);
        break;
      case XmlConfigEvent.CREATE_LOCK_AUTO:
        m_createLockAutoListener.handleUpdate(event);
        break;
      case XmlConfigEvent.DELETE_LOCK_AUTO:
        m_deleteLockAutoListener.handleUpdate(event);
        break;
      case XmlConfigEvent.CREATE_LOCK_NAMED:
        m_createLockNamedListener.handleUpdate(event);
        break;
      case XmlConfigEvent.DELETE_LOCK_NAMED:
        m_deleteLockNamedListener.handleUpdate(event);
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
