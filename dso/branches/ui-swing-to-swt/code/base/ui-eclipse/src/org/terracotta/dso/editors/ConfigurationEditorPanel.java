/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.xmlbeans.XmlConfigEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import org.terracotta.ui.util.AbstractSWTPanel;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.UpdateEvent;

import java.util.ArrayList;

public abstract class ConfigurationEditorPanel extends AbstractSWTPanel {

  private transient ArrayList<XmlObjectStructureListener> m_listenerList;
  private transient XmlObjectStructureChangeEvent         m_changeEvent;

  public ConfigurationEditorPanel(Composite parent, int style) {
    super(parent, style);
    setLayout(new FillLayout());
  }
  
  protected final XmlConfigEvent castEvent(UpdateEvent e) {
    return (XmlConfigEvent) e;
  }
  
  // TODO: REMOVE ALL OF THIS
  public static void ensureXmlObject(Composite comp) {
    ConfigurationEditorPanel parent = (ConfigurationEditorPanel) SWTUtil.getAncestorOfClass(
        ConfigurationEditorPanel.class, comp);

    if (parent != null) {
      parent.ensureXmlObject();
    }
  }

  public void ensureXmlObject() {
    ConfigurationEditorPanel parent = (ConfigurationEditorPanel) SWTUtil.getAncestorOfClass(
        ConfigurationEditorPanel.class, this);

    if (parent != null) {
      parent.ensureXmlObject();
    }
  }

  public void setDirty() {
    ConfigurationEditorRoot editorRoot = getConfigurationEditorRoot();
    final IProject project = editorRoot.getProject();

    PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
      public void run() {
        TcPlugin plugin = TcPlugin.getDefault();
        ConfigurationEditor editor = plugin.getConfigurationEditor(project);

        if (editor != null) {
          editor._setDirty();
        }
      }
    });
  }

  /**
   * Retrieve our top-level SWT parent.
   */
  private ConfigurationEditorRoot getConfigurationEditorRoot() {
    if (this instanceof ConfigurationEditorRoot) {
      return (ConfigurationEditorRoot) this;
    } else {
      return (ConfigurationEditorRoot) SWTUtil.getAncestorOfClass(ConfigurationEditorRoot.class, this);
    }
  }

  public synchronized void addXmlObjectStructureListener(XmlObjectStructureListener listener) {
    if(listener != null) {
      if(m_listenerList == null) {
        m_listenerList = new ArrayList<XmlObjectStructureListener>();
      }
      m_listenerList.add(listener);
    }
  }

  public synchronized void removeXmlObjectStructureListener(XmlObjectStructureListener listener) {
    if (listener != null) {
      if (m_listenerList != null) {
        m_listenerList.remove(listener);
      }
    }
  }

  private XmlObjectStructureChangeEvent getChangeEvent(XmlObject source) {
    if (m_changeEvent == null) {
      m_changeEvent = new XmlObjectStructureChangeEvent(source);
    } else {
      m_changeEvent.setXmlObject(source);
    }

    return m_changeEvent;
  }

  private XmlObjectStructureListener[] getListenerArray() {
    return m_listenerList.toArray(new XmlObjectStructureListener[0]);
  }

  protected void fireXmlObjectStructureChanged(XmlObjectStructureChangeEvent e) {
    if (m_listenerList != null) {
      XmlObjectStructureListener[] listeners = getListenerArray();

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].structureChanged(e);
      }
    }
  }

  protected void fireXmlObjectStructureChanged(XmlObject source) {
    fireXmlObjectStructureChanged(getChangeEvent(source));
  }

  protected int parseInt(String s) {
    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
      return 0;
    }
  }
}
