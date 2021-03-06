/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.apache.xmlbeans.XmlOptions;
import org.dijon.ScrollPane;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.JavaSetupParticipant;
import org.terracotta.dso.Messages;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.decorator.AdaptedModuleDecorator;
import org.terracotta.dso.decorator.AdaptedPackageFragmentDecorator;
import org.terracotta.dso.decorator.AdaptedTypeDecorator;
import org.terracotta.dso.decorator.AutolockedDecorator;
import org.terracotta.dso.decorator.DistributedMethodDecorator;
import org.terracotta.dso.decorator.ExcludedModuleDecorator;
import org.terracotta.dso.decorator.ExcludedTypeDecorator;
import org.terracotta.dso.decorator.NameLockedDecorator;
import org.terracotta.dso.decorator.RootDecorator;
import org.terracotta.dso.decorator.TransientDecorator;
import org.terracotta.dso.editors.xml.XMLEditor;

import com.terracottatech.config.Application;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.MessageFormat;

import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

public class ConfigurationEditor extends MultiPageEditorPart
  implements IResourceChangeListener,
             IGotoMarker
{
  private IProject             m_project;
  private Application          m_application;
  private DsoApplicationPanel  m_dsoAppPanel;
  private ServersPanel         m_serversPanel;
  private ClientsPanel         m_clientsPanel;
  private XMLEditor            m_xmlEditor;
  private int                  m_xmlEditorPageIndex;
  private ResourceDeltaVisitor m_resourceDeltaVisitor;
  private boolean              m_haveActiveConfig;
  private DocumentListener     m_docListener;
  private ElementStateListener m_elementStateListener;
  private TextInputListener    m_textInputListener;
  private Timer                m_parseTimer;
  
  static {
    try {
      java.lang.System.setProperty("sun.awt.noerasebackground", "true");
      java.lang.System.setProperty("swing.volatileImageBufferEnabled", "false");
    } catch(Throwable t) {/**/}
  }
  
  public ConfigurationEditor() {
    super();
    
    m_docListener          = new DocumentListener();
    m_elementStateListener = new ElementStateListener();
    m_textInputListener    = new TextInputListener();
    m_parseTimer           = new Timer(2000, new ParseTimerAction());
    
    m_parseTimer.setRepeats(false);
  }
    
  private Composite createComposite() {
    return new Composite(getContainer(), SWT.EMBEDDED);
  }
  
  void createDsoApplicationPage(int pageIndex) {
    final Composite composite = createComposite();
    final Frame     frame     = SWT_AWT.new_Frame(composite);
    JRootPane       rootPane  = new JRootPane();

    rootPane.setBackground(UIManager.getColor("control"));
    rootPane.setOpaque(true);
    frame.add(rootPane);
    ScrollPane scroller = new ScrollPane(m_dsoAppPanel = new DsoApplicationPanel()); 
    rootPane.getContentPane().add(scroller);
    m_dsoAppPanel.setup(m_project);
    m_dsoAppPanel.setVisible(true);

    addPage(pageIndex, composite);
    setPageText(pageIndex, "DSO config");
  }

  public DsoApplicationPanel getDsoApplicationPanel() {
    return m_dsoAppPanel;
  }
  
  protected void pageChange(final int newPageIndex) {
    if(newPageIndex != 0) {
      if(m_project != null && m_project.isOpen()) {
        TcPlugin plugin = TcPlugin.getDefault(); 
        TcConfig config = plugin.getConfiguration(m_project);
       
        if(config == TcPlugin.BAD_CONFIG) {
          Display.getDefault().syncExec(new Runnable() {
            public void run() {
              getControl(newPageIndex).setVisible(false);

              Shell  shell = Display.getDefault().getActiveShell();
              String title = "Terracotta Config Editor";
              String msg   = "The source page has errors. The other pages cannot be\nused until these errors are resolved.";
              
              MessageDialog.openWarning(shell, title, msg);
            }
          });
          setActivePage(0);
        }
      }
    }
  }
  
  public void showDsoApplicationPanel() {
    setActivePage(0);
  }
  
  void createServersPage(int pageIndex) {
    final Composite composite = createComposite();
    final Frame     frame     = SWT_AWT.new_Frame(composite);
    JRootPane       rootPane  = new JRootPane();

    rootPane.setBackground(UIManager.getColor("control"));
    rootPane.setOpaque(true);
    frame.add(rootPane);
    ScrollPane scroller = new ScrollPane(m_serversPanel = new ServersPanel());
    rootPane.getContentPane().add(scroller);
    m_serversPanel.setup(m_project);
    m_serversPanel.setVisible(true);

    addPage(pageIndex, composite);
    setPageText(pageIndex, "Servers config");
  }
    
  void createClientPage(int pageIndex) {
    final Composite composite = createComposite();
    final Frame     frame     = SWT_AWT.new_Frame(composite);
    JRootPane       rootPane  = new JRootPane();

    frame.setBackground(UIManager.getColor("control"));
    rootPane.setOpaque(true);
    frame.add(rootPane);
    ScrollPane scroller = new ScrollPane(m_clientsPanel = new ClientsPanel());
    rootPane.getContentPane().add(scroller);
    m_clientsPanel.setup(m_project);
    m_clientsPanel.setVisible(true);

    addPage(pageIndex, composite);    
    setPageText(pageIndex, "Clients config");
  }
    
  void createXMLEditorPage(int pageIndex) {
    try {
      IEditorInput input = getEditorInput();

      addPage(m_xmlEditorPageIndex = pageIndex, m_xmlEditor = new XMLEditor(), input);
      setPageText(m_xmlEditorPageIndex, m_xmlEditor.getTitle());

      m_xmlEditor.addTextInputListener(m_textInputListener);
      m_xmlEditor.getDocument().addDocumentListener(m_docListener);
      m_xmlEditor.getDocumentProvider().addElementStateListener(m_elementStateListener);
    }
    catch(PartInitException e) {
      ErrorDialog.openError(
        getSite().getShell(),
        "Error creating nested text editor",
        null,
        e.getStatus());
    }
  }
  
  public boolean isActiveConfig() {
    TcPlugin plugin = TcPlugin.getDefault();
    IFile configFile = plugin.getConfigurationFile(m_project);
    IFileEditorInput fileEditorInput = (FileEditorInput)getEditorInput();
    IFile file = fileEditorInput.getFile();
    
    return file != null && file.equals(configFile);
  }
  
  boolean haveActiveConfig() {
    return m_haveActiveConfig;
  }
  
  protected void createPages() {
    createXMLEditorPage(0);

    if(haveActiveConfig()) {
      createDsoApplicationPage(1);
      createServersPage(2);
      createClientPage(3);

      ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }
  }

  public void dispose() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
    super.dispose();
  }
 
  public void doSave(IProgressMonitor monitor) {
    m_xmlEditor.doSave(monitor);
  }

  public void doSaveAs() {
    performSaveAs(null);
  }

  protected void performSaveAs(IProgressMonitor progressMonitor) {
    Shell        shell    = getSite().getShell();
    IEditorInput input    = getEditorInput();
    SaveAsDialog dialog   = new SaveAsDialog(shell);
    IFile        original = null;
    
    if(input instanceof IFileEditorInput) {
      if((original = ((IFileEditorInput)input).getFile()) != null) {
        dialog.setOriginalFile(original);
      }
    }

    dialog.create();

    IDocumentProvider provider = m_xmlEditor.getDocumentProvider();
    if(provider == null) {
      // editor has programmatically been closed while the dialog was open
      return;
    }

    if(provider.isDeleted(input) && original != null) {
      String message = MessageFormat.format(Messages.Editor_warning_save_delete,
                                            new Object[] {original.getName()});
      
      dialog.setErrorMessage(null);
      dialog.setMessage(message, IMessageProvider.WARNING);
    }
    
    if(dialog.open() == Window.CANCEL) {
      if(progressMonitor != null) {
        progressMonitor.setCanceled(true);
      }
      return;
    }

    IPath filePath = dialog.getResult();
    if(filePath == null) {
      if(progressMonitor != null) {
        progressMonitor.setCanceled(true);
      }
      return;
    }

    IWorkspace         workspace = ResourcesPlugin.getWorkspace();
    IFile              file      = workspace.getRoot().getFile(filePath);
    final IEditorInput newInput  = new FileEditorInput(file);
    boolean            success   = false;

    try {
      provider.aboutToChange(newInput);
      provider.saveDocument(progressMonitor,
                            newInput,
                            provider.getDocument(input),
                            true);
      success = true;
      clearDirty();
    } catch(CoreException ce) {
      IStatus status = ce.getStatus();
      
      if(status == null || status.getSeverity() != IStatus.CANCEL) {
        String title = Messages.Editor_error_save_title;
        String msg   = MessageFormat.format(Messages.Editor_error_save_message,
                                            new Object[] {ce.getMessage()});

        if(status != null) {
          switch(status.getSeverity()) {
            case IStatus.INFO:
              MessageDialog.openInformation(shell, title, msg);
              break;
            case IStatus.WARNING:
              MessageDialog.openWarning(shell, title, msg);
              break;
            default:
              MessageDialog.openError(shell, title, msg);
          }
        }
        else {
          MessageDialog.openError(shell, title, msg);
        }
      }
    } finally {
      provider.changed(newInput);
      if(success) {
        setInput(newInput);
      }
    }

    if(progressMonitor != null) {
      progressMonitor.setCanceled(!success);
    }
  }
  
  public void gotoMarker(IMarker marker) {
    setActivePage(0);
    IDE.gotoMarker(getEditor(0), marker);
  }

  public void newInputFile(final IFile file) {
    if(file != null && file.exists()) {
      final FileEditorInput input = new FileEditorInput(file);
      
      setInput(input);

      m_project = file.getProject();

      if(haveActiveConfig()) {
        if(getPageCount() == 1) {
          createDsoApplicationPage(1);
          createServersPage(2);
          createClientPage(3);
        }
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
        initPanels();
      } else {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        for(int i = getPageCount()-1; i > 0; i--) {
          removePage(i);
        }
      }
      
      syncExec(new Runnable() {
        public void run() {
          m_xmlEditor.setInput(input);
          String name = file.getName();
          setPartName(name);
          setPageText(m_xmlEditorPageIndex, name);
        }
      });
    }
  }
  
  private void syncExec(Runnable runner) {
    getSite().getShell().getDisplay().syncExec(runner);
  }
  
  private void asyncExec(Runnable runner) {
    getSite().getShell().getDisplay().asyncExec(runner);
  }

  public void init(IEditorSite site, IEditorInput editorInput)
    throws PartInitException
  {
    if(!(editorInput instanceof IFileEditorInput)) {
      throw new PartInitException("Invalid Input: Must be IFileEditorInput");
    }
   
    IFileEditorInput fileEditorInput = (IFileEditorInput)editorInput;
    IFile            file            = fileEditorInput.getFile();
    IProject         project         = file.getProject();
    
    if(!project.exists()) {
      String msg = "Project '"+project.getName()+"' does not exist";

      ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
      throw new PartInitException(msg);
    }
    
    if(!project.isOpen()) {
      String msg = "Project '"+project.getName()+"' is not open";

      ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
      throw new PartInitException(msg);
    }

    m_project = project;
    super.init(site, editorInput);

    setPartName(file.getName());
  }
  
  protected void setInput(IEditorInput input) {
    super.setInput(input);
    m_haveActiveConfig = isActiveConfig();
  }

  public boolean isSaveAsAllowed() {
    return true;
  }
  
  private ResourceDeltaVisitor getResourceDeltaVisitor() {
    if(m_resourceDeltaVisitor == null) {
      m_resourceDeltaVisitor = new ResourceDeltaVisitor();
    }
    return m_resourceDeltaVisitor;
  }
  
  class ResourceDeltaVisitor implements IResourceDeltaVisitor {
    public boolean visit(IResourceDelta delta) {
      if(PlatformUI.getWorkbench().isClosing()) {
        return false;
      }
      
      if(delta.getKind() == IResourceDelta.CHANGED) {
        TcPlugin  plugin = TcPlugin.getDefault();
        IResource res    = delta.getResource();
          
        if(res instanceof IFile) {
          IProject project = res.getProject();
          int flags = delta.getFlags();
          
          if(project != null && (flags & IResourceDelta.CONTENT) != 0) {
            if((flags & IResourceDelta.MARKERS) != 0) {
              return false;
            }
            IFile configFile = plugin.getConfigurationFile(project);
            
            if(configFile != null && configFile.equals(res)) {
              plugin.reloadConfiguration(m_project);
              initPanels();
              clearDirty();

              return false;
            }
          }
        }
      }
      
      return true;
    }
  }

  /**
   * Ensures that the config has an Application element.
   */
  private void ensureRequiredConfigElements() {
    if(m_project != null && m_project.isOpen()) {
      TcConfig config = TcPlugin.getDefault().getConfiguration(m_project);
    
      if(config != null) {
        m_application = config.getApplication();
      
        if(m_application == null) {
          m_application = config.addNewApplication();
          m_application.addNewDso().addNewInstrumentedClasses();
        }
      }
    }
  }

  public void initPanels() {
    try {
      SwingUtilities.invokeAndWait(new Runnable () {
        public void run() {
          if(m_project != null && m_project.isOpen()) {
            enablePanels();
            ensureRequiredConfigElements();
          
            m_dsoAppPanel.setup(m_project);
            m_serversPanel.setup(m_project);
            m_clientsPanel.setup(m_project);
          }
          else {
            disablePanels();
          }
        }
      });
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  public void updateInstrumentedClassesPanel() {
    syncXmlDocument();
    try {
      SwingUtilities.invokeAndWait(new Runnable () {
        public void run() {
          m_dsoAppPanel.updateInstrumentedClassesPanel();
        }
      });
    } catch(Exception e) {
      e.printStackTrace();
    }
    TcPlugin.getDefault().updateDecorators(
      new String[] {
        AdaptedModuleDecorator.DECORATOR_ID,
        AdaptedTypeDecorator.DECORATOR_ID,
        AdaptedPackageFragmentDecorator.DECORATOR_ID,
        ExcludedTypeDecorator.DECORATOR_ID,
        ExcludedModuleDecorator.DECORATOR_ID});
  }

  public void updateTransientsPanel() {
    syncXmlDocument();
    try {
      SwingUtilities.invokeAndWait(new Runnable () {
        public void run() {
          m_dsoAppPanel.updateTransientsPanel();
        }
      });
    } catch(Exception e) {
      e.printStackTrace();
    }
    TcPlugin.getDefault().updateDecorator(TransientDecorator.DECORATOR_ID);
  }
  
  public void updateRootsPanel() {
    syncXmlDocument();
    try {
      SwingUtilities.invokeAndWait(new Runnable () {
        public void run() {
          m_dsoAppPanel.updateRootsPanel();
        }
      });
    } catch(Exception e) {
      e.printStackTrace();
    }
    TcPlugin.getDefault().updateDecorator(RootDecorator.DECORATOR_ID);
  }

  public void updateDistributedMethodsPanel() {
    try {
      SwingUtilities.invokeAndWait(new Runnable () {
        public void run() {
          m_dsoAppPanel.updateDistributedMethodsPanel();
        }
      });
    } catch(Exception e) {
      e.printStackTrace();
    }
    TcPlugin.getDefault().updateDecorator(DistributedMethodDecorator.DECORATOR_ID);
  }

  public void updateLocksPanel() {
    try {
      SwingUtilities.invokeAndWait(new Runnable () {
        public void run() {
          m_dsoAppPanel.updateLocksPanel();
        }
      });
    } catch(Exception e) {
      e.printStackTrace();
    }
    TcPlugin.getDefault().updateDecorators(
      new String[] {
        NameLockedDecorator.DECORATOR_ID,
        AutolockedDecorator.DECORATOR_ID});
  }

  public void updateBootClassesPanel() {
    try {
      SwingUtilities.invokeAndWait(new Runnable () {
        public void run() {
          m_dsoAppPanel.updateBootClassesPanel();
        }
      });
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private void disablePanels() {
    m_dsoAppPanel.setEnabled(false);
    m_serversPanel.setEnabled(false);
    m_clientsPanel.setEnabled(false);
  }
  
  private void enablePanels() {
    m_dsoAppPanel.setEnabled(true);
    m_serversPanel.setEnabled(true);
    m_clientsPanel.setEnabled(true);
  }
  
  public void closeEditor() {
    getSite().getPage().closeEditor(this, true);    
  }
  
  public void resourceChanged(final IResourceChangeEvent event){
    switch(event.getType()) {
      case IResourceChangeEvent.PRE_DELETE:
      case IResourceChangeEvent.PRE_CLOSE: {
        asyncExec(new Runnable() {
          public void run(){
            if(m_project.equals(event.getResource())) {
              ConfigurationEditor.this.closeEditor();
            }
          }            
        });
        break;
      }
      case IResourceChangeEvent.POST_CHANGE: {
        asyncExec(new Runnable() {
          public void run(){
            try {
              event.getDelta().accept(getResourceDeltaVisitor());
            } catch(CoreException ce) {
              ce.printStackTrace();
            }
          }
        });
        break;
      }
    }
  }

  public void syncXmlDocument() {
    TcPlugin     plugin = TcPlugin.getDefault();
    IDocument    doc    = m_xmlEditor.getDocument();
    XmlOptions   opts   = plugin.getXmlOptions();
    
    TcConfig config = plugin.getConfiguration(m_project);

    if(config != null) {
      TcConfigDocument configDoc = TcConfigDocument.Factory.newInstance();
      
      configDoc.setTcConfig(config);
      doc.removeDocumentListener(m_docListener);
      doc.set(configDoc.xmlText(opts));
      doc.addDocumentListener(m_docListener);
    }
  }

  public synchronized void syncXmlModel() {
    TcPlugin  plugin  = TcPlugin.getDefault();
    IDocument doc     = m_xmlEditor.getDocument();
    String    xmlText = doc.get();
    
    try {
      plugin.setConfigurationFromString(m_project, xmlText);
      initPanels();
    } catch(IOException ioe) {
      disablePanels();
    }
  }

  /**
   * This gets invokes by our sub-components when they've modified the model.
   */
  public synchronized void setDirty() {
    syncXmlDocument();
    internalSetDirty(Boolean.TRUE);

    TcPlugin plugin = TcPlugin.getDefault(); 
    plugin.getConfigurationHelper(m_project).validateAll();
    JavaSetupParticipant.inspectAll();
    plugin.updateDecorators();
    plugin.fireConfigurationChange(m_project);
  }

  private void clearDirty() {
    internalSetDirty(Boolean.FALSE);
  }
  
  private void internalSetDirty(Boolean isDirty) {
    TcPlugin.getDefault().setConfigurationFileDirty(m_project, isDirty);
    firePropertyChange(PROP_DIRTY);
  }
  
  public boolean isDirty() {
    if(m_project != null && haveActiveConfig()) {
      return m_project.isOpen() && TcPlugin.getDefault().isConfigurationFileDirty(m_project);
    } else {
      return super.isDirty();
    }
  }
  
  public void modelChanged() {
    syncXmlDocument();
    internalSetDirty(Boolean.TRUE);
  }
  
  public IDocument getDocument() {
    return m_xmlEditor.getDocument();
  }
  
  class TextInputListener implements ITextInputListener {
    public void inputDocumentAboutToBeChanged(
      IDocument oldInput,
      IDocument newInput) {/**/}
    
    public void inputDocumentChanged(
      IDocument oldInput,
      IDocument newInput)
    {
      if(oldInput != null) {
        oldInput.removeDocumentListener(m_docListener);
      }
      if(newInput != null) {
        newInput.addDocumentListener(m_docListener);
      }
    }
  }
  
  class ElementStateListener implements IElementStateListener {
    public void elementContentAboutToBeReplaced(Object element) {
      m_xmlEditor.getDocument().removeDocumentListener(m_docListener);      
    }

    public void elementContentReplaced(Object element) {
      m_xmlEditor.getDocument().addDocumentListener(m_docListener);      
    }

    public void elementDeleted(Object element) {/**/}
    public void elementMoved(Object originalElement, Object movedElement) {/**/}
    public void elementDirtyStateChanged(Object element, boolean isDirty) {/**/}
  }
  
  class DocumentListener implements IDocumentListener {
    public void documentAboutToBeChanged(DocumentEvent event) {/**/}
    
    public void documentChanged(DocumentEvent event) {
      if(haveActiveConfig()) m_parseTimer.stop();
      internalSetDirty(Boolean.TRUE);
      if(haveActiveConfig()) m_parseTimer.start();
    }
  }
  
  class ParseTimerAction implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      asyncExec(new Runnable() {
        public void run(){
          syncXmlModel();
        }
      });
    }
  }
  
  public void applyProblemToText(String text, String msg, String markerType) {
    TcPlugin            plugin       = TcPlugin.getDefault();
    IDocument           doc          = m_xmlEditor.getDocument();
    ConfigurationHelper configHelper = plugin.getConfigurationHelper(m_project);
    
    configHelper.applyProblemToText(doc, text, msg, markerType);
  }
}
