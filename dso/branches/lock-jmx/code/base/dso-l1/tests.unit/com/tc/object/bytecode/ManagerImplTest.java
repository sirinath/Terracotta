/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCNonPortableObjectError;
import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.appevent.ApplicationEvent;
import com.tc.object.appevent.ApplicationEventContext;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.loaders.StandardClassProvider;
import com.tc.object.session.SessionID;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.object.tx.WaitInvocation;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;
import com.tc.object.util.ToggleableStrongReference;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ManagerImplTest extends BaseDSOTestCase {

  public void testClassAutolocksIgnored() throws Exception {
    ClientObjectManager objMgr = new ObjMgr();
    ClientTransactionManager txnMgr = new TxnMgr();

    Manager manager = new ManagerImpl(false, objMgr, txnMgr, this.configHelper(), new StandardClassProvider(), null);

    manager.monitorEnter(getClass(), Manager.LOCK_TYPE_WRITE);

    manager.monitorExit(getClass());
  }

  private static class TxnMgr implements ClientTransactionManager {

    public boolean begin(String lock, int lockLevel, String lockType, String contextInfo) {
      throw new AssertionError("should not be called");
    }

    public void apply(TxnType txType, List lockIDs, Collection objectChanges, Set lookupObjectIDs, Map newRoots) {
      throw new ImplementMe();
    }

    public void createObject(TCObject source) {
      throw new ImplementMe();
    }

    public void createRoot(String name, ObjectID id) {
      throw new ImplementMe();
    }

    public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
      throw new ImplementMe();
    }

    public void logicalInvoke(TCObject source, int method, String methodName, Object[] parameters) {
      throw new ImplementMe();
    }

    public void wait(WaitInvocation call, Object object) throws UnlockedSharedObjectException {
      throw new ImplementMe();
    }

    public void notify(String lockName, boolean all, Object object) throws UnlockedSharedObjectException {
      throw new ImplementMe();
    }

    public void receivedAcknowledgement(SessionID sessionID, TransactionID requestID) {
      throw new ImplementMe();
    }

    public void receivedBatchAcknowledgement(TxnBatchID batchID) {
      throw new ImplementMe();
    }

    public void checkReadAccess(Object context) {
      throw new ImplementMe();
    }

    public void checkWriteAccess(Object context) {
      throw new ImplementMe();
    }

    public void addReference(TCObject tco) {
      throw new ImplementMe();
    }

    public ChannelIDProvider getChannelIDProvider() {
      throw new ImplementMe();
    }

    public boolean isLocked(String lockName, int lockLevel) {
      throw new ImplementMe();
    }

    public void commit(String lockName) throws UnlockedSharedObjectException {
      throw new AssertionError("should not be called");
    }

    public void wait(String lockName, WaitInvocation call, Object object) throws UnlockedSharedObjectException {
      throw new ImplementMe();

    }

//    public void lock(String lockName, int lockLevel) {
//      throw new ImplementMe();
//    }
//
    public void unlock(String lockName) {
      throw new ImplementMe();
    }

    public int queueLength(String lockName) {
      throw new ImplementMe();
    }

    public int waitLength(String lockName) {
      throw new ImplementMe();
    }

    public ClientTransaction getTransaction() {
      throw new ImplementMe();
    }

    public void disableTransactionLogging() {
      throw new ImplementMe();
    }

    public void enableTransactionLogging() {
      throw new ImplementMe();
    }

    public boolean isTransactionLoggingDisabled() {
      throw new ImplementMe();
    }

    public boolean isHeldByCurrentThread(String lockName, int lockLevel) {
      throw new ImplementMe();
    }

    public boolean tryBegin(String lock, WaitInvocation timeout, int lockLevel, String lockType) {
      throw new ImplementMe();
    }

    public void arrayChanged(TCObject src, int startPos, Object array, int length) {
      throw new ImplementMe();
    }

    public void literalValueChanged(TCObject source, Object newValue, Object oldValue) {
      throw new ImplementMe();
    }

    public void addDmiDescriptor(DmiDescriptor d) {
      throw new ImplementMe();
    }

    public int localHeldCount(String lockName, int lockLevel) {
      throw new ImplementMe();
    }

    public boolean isLockOnTopStack(String lockName) {
      return false;
    }
  }

  private static class ObjMgr implements ClientObjectManager {

    public Class getClassFor(String className, String loaderDesc) {
      throw new ImplementMe();
    }

    public boolean isManaged(Object pojo) {
      return false;
    }

    public void markReferenced(TCObject tcobj) {
      throw new ImplementMe();
    }

    public boolean isPortableInstance(Object pojo) {
      throw new ImplementMe();
    }

    public boolean isPortableClass(Class clazz) {
      throw new ImplementMe();
    }

    public void checkPortabilityOfField(Object value, String fieldName, Object pojo) throws TCNonPortableObjectError {
      throw new ImplementMe();
    }

    public void checkPortabilityOfLogicalAction(Object[] params, int index, String methodName, Object pojo)
        throws TCNonPortableObjectError {
      throw new ImplementMe();
    }

    public Object lookupObject(ObjectID id) {
      throw new ImplementMe();
    }

    public Object lookupObject(ObjectID id, ObjectID parentContext) {
      throw new ImplementMe();
    }

    public TCObject lookupOrCreate(Object obj) {
      throw new ImplementMe();
    }

    public ObjectID lookupExistingObjectID(Object obj) {
      throw new ImplementMe();
    }

    public Object lookupRoot(String name) {
      throw new ImplementMe();
    }

    public Object lookupOrCreateRoot(String name, Object obj) {
      throw new ImplementMe();
    }

    public TCObject lookupIfLocal(ObjectID id) {
      throw new ImplementMe();
    }

    public TCObject lookup(ObjectID id) {
      throw new ImplementMe();
    }

    public TCObject lookupExistingOrNull(Object pojo) {
      return null;
    }

    public Collection getAllObjectIDsAndClear(Collection c) {
      throw new ImplementMe();
    }

    public WeakReference createNewPeer(TCClass clazz, DNA dna) {
      throw new ImplementMe();
    }

    public WeakReference createNewPeer(TCClass clazz, int size, ObjectID id, ObjectID parentID) {
      throw new ImplementMe();
    }

    public TCClass getOrCreateClass(Class clazz) {
      throw new ImplementMe();
    }

    public void setTransactionManager(ClientTransactionManager txManager) {
      throw new ImplementMe();
    }

    public ClientTransactionManager getTransactionManager() {
      throw new ImplementMe();
    }

    public ReferenceQueue getReferenceQueue() {
      throw new ImplementMe();
    }

    public void shutdown() {
      throw new ImplementMe();
    }

    public void unpause() {
      throw new ImplementMe();
    }

    public void pause() {
      throw new ImplementMe();
    }

    public void starting() {
      throw new ImplementMe();
    }

    public Object deepCopy(Object source, OptimisticTransactionManager optimisticTxManager) {
      throw new ImplementMe();
    }

    public Object createNewCopyInstance(Object source, Object parent) {
      throw new ImplementMe();
    }

    public Object createParentCopyInstanceIfNecessary(Map visited, Map cloned, Object v) {
      throw new ImplementMe();
    }

    public void replaceRootIDIfNecessary(String rootName, ObjectID newRootID) {
      throw new ImplementMe();

    }

    public Object lookupOrCreateRoot(String name, Object obj, boolean dsoFinal) {
      throw new ImplementMe();
    }

    public TCObject lookupOrShare(Object pojo) {
      throw new ImplementMe();
    }

    public boolean isCreationInProgress() {
      throw new ImplementMe();
    }

    public void addPendingCreateObjectsToTransaction() {
      throw new ImplementMe();

    }

    public boolean hasPendingCreateObjects() {
      throw new ImplementMe();
    }

    public Object lookupObjectNoDepth(ObjectID id) {
      throw new ImplementMe();
    }

    public Object lookupOrCreateRootNoDepth(String rootName, Object object) {
      throw new ImplementMe();
    }

    public Object createOrReplaceRoot(String rootName, Object root) {
      throw new ImplementMe();
    }

    public void storeObjectHierarchy(Object pojo, ApplicationEventContext context) {
      throw new ImplementMe();
    }

    public void sendApplicationEvent(Object pojo, ApplicationEvent event) {
      throw new ImplementMe();
    }

    public Object cloneAndInvokeLogicalOperation(Object pojo, String methodName, Object[] parameters) {
      throw new ImplementMe();
    }

    public ToggleableStrongReference getOrCreateToggleRef(ObjectID id, Object peer) {
      throw new ImplementMe();
    }

  }

}
