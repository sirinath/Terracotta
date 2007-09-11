/**
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.util.runtime.Vm;

public class StandardConfiguration
      extends TerracottaConfiguratorModule {

   protected void addInstrumentation(final BundleContext context,
         final StandardDSOClientConfigHelper configHelper) {
      super.addInstrumentation(context, configHelper);
      configAutoLockExcludes();
      configPermanentExcludes();
      configFileTypes();
      configEventTypes();
      configExceptionTypes();
      configArrayTypes();
      configAWTModels();
      configSwingModels();
   }
   
   private void configArrayTypes() {
      final TransparencyClassSpec spec = getOrCreateSpec("java.util.Arrays");
      spec.addDoNotInstrument("copyOfRange");
      spec.addDoNotInstrument("copyOf");
      getOrCreateSpec("java.util.Arrays$ArrayList");
   }
   
   private void configAutoLockExcludes() {
      configHelper.addAutoLockExcludePattern("* java.lang.Throwable.*(..)");      
   }
   
   private void configPermanentExcludes() {
      configHelper.addPermanentExcludePattern("java.awt.Component");
      configHelper.addPermanentExcludePattern("java.lang.Thread");
      configHelper.addPermanentExcludePattern("java.lang.ThreadLocal");
      configHelper.addPermanentExcludePattern("java.lang.ThreadGroup");
      configHelper.addPermanentExcludePattern("java.lang.Process");
      configHelper.addPermanentExcludePattern("java.lang.ClassLoader");
      configHelper.addPermanentExcludePattern("java.lang.Runtime");
      configHelper.addPermanentExcludePattern("java.io.FileReader");
      configHelper.addPermanentExcludePattern("java.io.FileWriter");
      configHelper.addPermanentExcludePattern("java.io.FileDescriptor");
      configHelper.addPermanentExcludePattern("java.io.FileInputStream");
      configHelper.addPermanentExcludePattern("java.io.FileOutputStream");
      configHelper.addPermanentExcludePattern("java.net.DatagramSocket");
      configHelper.addPermanentExcludePattern("java.net.DatagramSocketImpl");
      configHelper.addPermanentExcludePattern("java.net.MulticastSocket");
      configHelper.addPermanentExcludePattern("java.net.ServerSocket");
      configHelper.addPermanentExcludePattern("java.net.Socket");
      configHelper.addPermanentExcludePattern("java.net.SocketImpl");
      configHelper.addPermanentExcludePattern("java.nio.channels.DatagramChannel");
      configHelper.addPermanentExcludePattern("java.nio.channels.FileChannel");
      configHelper.addPermanentExcludePattern("java.nio.channels.FileLock");
      configHelper.addPermanentExcludePattern("java.nio.channels.ServerSocketChannel");
      configHelper.addPermanentExcludePattern("java.nio.channels.SocketChannel");
      configHelper.addPermanentExcludePattern("java.util.logging.FileHandler");
      configHelper.addPermanentExcludePattern("java.util.logging.SocketHandler");
      configHelper.addPermanentExcludePattern("com.sun.crypto.provider..*");

      // 
      configHelper.addPermanentExcludePattern("java.util.WeakHashMap+");
      configHelper.addPermanentExcludePattern("java.lang.ref.*");

      // unsupported java.util.concurrent types
      configHelper.addPermanentExcludePattern("java.util.concurrent.AbstractExecutorService");
      configHelper.addPermanentExcludePattern("java.util.concurrent.ArrayBlockingQueue*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.ConcurrentLinkedQueue*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.ConcurrentSkipListMap*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.ConcurrentSkipListSet*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.CopyOnWriteArrayList*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.CopyOnWriteArraySet*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.CountDownLatch*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.DelayQueue*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.Exchanger*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.ExecutorCompletionService*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.LinkedBlockingDeque*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.PriorityBlockingQueue*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.ScheduledThreadPoolExecutor*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.Semaphore*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.SynchronousQueue*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.ThreadPoolExecutor*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicBoolean*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicIntegerArray*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicIntegerFieldUpdater*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicLongArray*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicLongFieldUpdater*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicMarkableReference*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicReference*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicReferenceArray*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicReferenceFieldUpdater*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicStampedReference*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.locks.AbstractQueuedLongSynchronizer*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.locks.AbstractQueuedSynchronizer*");
      configHelper.addPermanentExcludePattern("java.util.concurrent.locks.LockSupport*");
   }

   private void configFileTypes() {
      final TransparencyClassSpec spec = getOrCreateSpec("java.io.File");
      spec.setHonorTransient(true);
   }
   
   private void configEventTypes() {
      final TransparencyClassSpec spec = getOrCreateSpec("java.util.EventObject");
      spec.setHonorTransient(true);
   }

   private void configExceptionTypes() {
      getOrCreateSpec("java.lang.Exception");
      getOrCreateSpec("java.lang.RuntimeException");
      getOrCreateSpec("java.lang.InterruptedException");
      getOrCreateSpec("java.awt.AWTException");
      getOrCreateSpec("java.io.IOException");
      getOrCreateSpec("java.io.FileNotFoundException");
      getOrCreateSpec("java.lang.Error");
      getOrCreateSpec("java.util.ConcurrentModificationException");
      getOrCreateSpec("java.util.NoSuchElementException");
   }

   private void configAWTModels() {
      // Color
      configHelper.addIncludePattern("java.awt.Color", true);
      TransparencyClassSpec spec = getOrCreateSpec("java.awt.Color");
      spec.addTransient("cs");

      // MouseMotionAdapter, MouseAdapter
      getOrCreateSpec("java.awt.event.MouseMotionAdapter");
      getOrCreateSpec("java.awt.event.MouseAdapter");

      // Point
      getOrCreateSpec("java.awt.Point");
      getOrCreateSpec("java.awt.geom.Point2D");
      getOrCreateSpec("java.awt.geom.Point2D$Double");
      getOrCreateSpec("java.awt.geom.Point2D$Float");

      // Line
      getOrCreateSpec("java.awt.geom.Line2D");
      getOrCreateSpec("java.awt.geom.Line2D$Double");
      getOrCreateSpec("java.awt.geom.Line2D$Float");

      // Rectangle
      getOrCreateSpec("java.awt.Rectangle");
      getOrCreateSpec("java.awt.geom.Rectangle2D");
      getOrCreateSpec("java.awt.geom.RectangularShape");
      getOrCreateSpec("java.awt.geom.Rectangle2D$Double");
      getOrCreateSpec("java.awt.geom.Rectangle2D$Float");
      getOrCreateSpec("java.awt.geom.RoundRectangle2D");
      getOrCreateSpec("java.awt.geom.RoundRectangle2D$Double");
      getOrCreateSpec("java.awt.geom.RoundRectangle2D$Float");

      // Ellipse2D
      getOrCreateSpec("java.awt.geom.Ellipse2D");
      getOrCreateSpec("java.awt.geom.Ellipse2D$Double");
      getOrCreateSpec("java.awt.geom.Ellipse2D$Float");

      // java.awt.geom.Path2D
      if (Vm.isJDK16Compliant()) {
         getOrCreateSpec("java.awt.geom.Path2D");
         getOrCreateSpec("java.awt.geom.Path2D$Double");
         getOrCreateSpec("java.awt.geom.Path2D$Float");
      }

      // GeneralPath
      getOrCreateSpec("java.awt.geom.GeneralPath");
      // 
      // BasicStroke
      getOrCreateSpec("java.awt.BasicStroke");

      // Dimension
      getOrCreateSpec("java.awt.Dimension");
      getOrCreateSpec("java.awt.geom.Dimension2D");
   }

   private void configSwingModels() {
      // TableModelEvent
      configHelper.addIncludePattern("javax.swing.event.TableModelEvent", true);
      getOrCreateSpec("javax.swing.event.TableModelEvent");

      // AbstractTableModel
      configHelper.addIncludePattern("javax.swing.table.AbstractTableModel",
            true);
      TransparencyClassSpec spec = getOrCreateSpec("javax.swing.table.AbstractTableModel");
      spec.addDistributedMethodCall("fireTableChanged",
            "(Ljavax/swing/event/TableModelEvent;)V", false);
      spec.addTransient("listenerList");

      // DefaultTableModel
      spec = getOrCreateSpec("javax.swing.table.DefaultTableModel");
      spec.setCallConstructorOnLoad(true);
      LockDefinition ld = configHelper.createLockDefinition(
            "tcdefaultTableLock", ConfigLockLevel.WRITE);
      ld.commit();
      addLock("* javax.swing.table.DefaultTableModel.set*(..)", ld);
      addLock("* javax.swing.table.DefaultTableModel.insert*(..)", ld);
      addLock("* javax.swing.table.DefaultTableModel.move*(..)", ld);
      addLock("* javax.swing.table.DefaultTableModel.remove*(..)", ld);
      ld = configHelper.createLockDefinition("tcdefaultTableLock",
            ConfigLockLevel.READ);
      ld.commit();
      addLock("* javax.swing.table.DefaultTableModel.get*(..)", ld);

      // DefaultListModel
      spec = getOrCreateSpec("javax.swing.DefaultListModel");
      spec.setCallConstructorOnLoad(true);
      ld = configHelper.createLockDefinition("tcdefaultListLock",
            ConfigLockLevel.WRITE);
      ld.commit();
      addLock("* javax.swing.DefaultListModel.*(..)", ld);

      // TreePath
      configHelper.addIncludePattern("javax.swing.tree.TreePath", false);
      getOrCreateSpec("javax.swing.tree.TreePath");

      // DefaultMutableTreeNode
      configHelper.addIncludePattern("javax.swing.tree.DefaultMutableTreeNode",
            false);
      getOrCreateSpec("javax.swing.tree.DefaultMutableTreeNode");

      // DefaultTreeModel
      spec = getOrCreateSpec("javax.swing.tree.DefaultTreeModel");
      ld = configHelper.createLockDefinition("tcdefaultTreeLock",
            ConfigLockLevel.WRITE);
      ld.commit();
      addLock("* javax.swing.tree.DefaultTreeModel.get*(..)", ld);
      addLock("* javax.swing.tree.DefaultTreeModel.set*(..)", ld);
      addLock("* javax.swing.tree.DefaultTreeModel.insert*(..)", ld);
      spec.addTransient("listenerList");
      spec.addDistributedMethodCall("fireTreeNodesChanged",
            "(Ljava/lang/Object;[Ljava/lang/Object;[I[Ljava/lang/Object;)V",
            false);
      spec.addDistributedMethodCall("fireTreeNodesInserted",
            "(Ljava/lang/Object;[Ljava/lang/Object;[I[Ljava/lang/Object;)V",
            false);
      spec.addDistributedMethodCall("fireTreeNodesRemoved",
            "(Ljava/lang/Object;[Ljava/lang/Object;[I[Ljava/lang/Object;)V",
            false);
      spec.addDistributedMethodCall("fireTreeStructureChanged",
            "(Ljava/lang/Object;[Ljava/lang/Object;[I[Ljava/lang/Object;)V",
            false);
      spec.addDistributedMethodCall("fireTreeStructureChanged",
            "(Ljava/lang/Object;Ljavax/swing/tree/TreePath;)V", false);

      // AbstractListModel
      spec = getOrCreateSpec("javax.swing.AbstractListModel");
      spec.addTransient("listenerList");
      spec.addDistributedMethodCall("fireContentsChanged",
            "(Ljava/lang/Object;II)V", false);
      spec.addDistributedMethodCall("fireIntervalAdded",
            "(Ljava/lang/Object;II)V", false);
      spec.addDistributedMethodCall("fireIntervalRemoved",
            "(Ljava/lang/Object;II)V", false);
   }

}
