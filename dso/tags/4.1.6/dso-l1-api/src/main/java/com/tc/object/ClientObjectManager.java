/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.abortable.AbortedOperationException;
import com.tc.exception.TCClassNotFoundException;
import com.tc.exception.TCNonPortableObjectError;
import com.tc.net.GroupID;
import com.tc.object.dna.api.DNA;
import com.tc.object.tx.ClientTransactionManager;

import java.lang.ref.WeakReference;

/**
 * Manages client-side (L1) object state in a VM.
 */
public interface ClientObjectManager extends TCObjectSelfCallback {

  /**
   * Find a class based on the class name and the classloader name
   * 
   * @param className Class name
   * @return Class, never null
   * @throws ClassNotFoundException If class not found
   */
  public Class getClassFor(String className) throws ClassNotFoundException;

  /**
   * Checks whether the state of an ObjectID is present on the current node.
   * 
   * @param objectID the object ID to check
   * @return {@code true} when the state of the object ID is present on the current node; or {@code false} otherwise
   */
  public boolean isLocal(ObjectID objectID);

  /**
   * Determine whether this instance is managed.
   * 
   * @param pojo The instance
   * @return True if managed
   */
  public boolean isManaged(Object pojo);

  /**
   * Determine whether this class is portable
   * 
   * @param clazz The class to check
   * @return True if portable
   */
  public boolean isPortableClass(Class clazz);

  /**
   * Determine whether this instance is portable
   * 
   * @param instance The instance to check
   * @return True if portable
   */
  public boolean isPortableInstance(Object instance);

  /**
   * Check whether field of an instance is portable
   * 
   * @param value Field value
   * @param fieldName Field name
   * @param pojo Instance to check
   * @throws TCNonPortableObjectError If field is not portable
   */
  public void checkPortabilityOfField(Object value, String fieldName, Object pojo) throws TCNonPortableObjectError;

  /**
   * Check whether logical action is portable
   * 
   * @param params Method call parameters
   * @param paramIndex Parameter index
   * @param methodName Method name
   * @param pojo Instance
   * @throws TCNonPortableObjectError If logical action is not portable
   */
  public void checkPortabilityOfLogicalAction(Object[] params, int paramIndex, String methodName, Object pojo)
      throws TCNonPortableObjectError;

  /**
   * Replace root ID. Primitive roots are replaceable. Object reference roots generally are not but this can be
   * controlled by the configuration.
   * 
   * @param rootName Root object name
   * @param newRootID New root object identifier
   */
  public void replaceRootIDIfNecessary(String rootName, ObjectID newRootID);

  /**
   * Prefetch object by ID, faulting into the JVM if necessary, Async lookup and will not cause ObjectNotFoundException
   * like lookupObject. Non-existent objects are ignored by the server.
   * 
   * @param id Object identifier
   * @throws AbortedOperationException
   */
  public void preFetchObject(ObjectID id) throws AbortedOperationException;

  /**
   * Find object by ID. If necessary, the object will be faulted into the JVM. This method will not log any exceptions
   * encountered.
   * 
   * @param id Identifier
   * @return Instance for the id
   * @throws ClassNotFoundException If class can't be found in this VM
   * @throws AbortedOperationException
   */
  public Object lookupObjectQuiet(ObjectID id) throws ClassNotFoundException, AbortedOperationException;

  /**
   * Find object by ID. If necessary, the object will be faulted into the JVM. The default fault-count will be used to
   * limit the number of dependent objects that are also faulted in.
   * 
   * @param id Identifier
   * @return Instance for the id
   * @throws ClassNotFoundException If class can't be found in this VM
   * @throws AbortedOperationException
   */
  public Object lookupObject(ObjectID id) throws ClassNotFoundException, AbortedOperationException;

  /**
   * Look up object by ID, faulting into the JVM if necessary, This method also passes the parent Object context so that
   * more intelligent prefetching is possible at the L2. The default fault-count will be used to limit the number of
   * dependent objects that are also faulted in.
   * 
   * @param id Object identifier of the object we are looking up
   * @param parentContext Object identifier of the parent object
   * @return The actual object
   * @throws AbortedOperationException
   * @throws TCClassNotFoundException If a class is not found during faulting
   */
  public Object lookupObject(ObjectID id, ObjectID parentContext) throws ClassNotFoundException,
      AbortedOperationException;

  /**
   * Find object by ID. If necessary, the object will be faulted into the JVM. No fault-count depth will be used and all
   * dependent objects will be faulted into memory.
   * 
   * @param id Identifier
   * @return Instance for the id
   * @throws ClassNotFoundException If class can't be found in this VM
   * @throws AbortedOperationException
   */
  public Object lookupObjectNoDepth(ObjectID id) throws ClassNotFoundException, AbortedOperationException;

  /**
   * Find the managed object for this instance or create a new one if it does not yet exist.
   * 
   * @param obj Instance
   * @return Managed object, may be new. Should never be null, but might be object representing null TCObject.
   */
  public TCObject lookupOrCreate(Object obj);

  /**
   * Find the managed object for this instance or create a new one if it does not yet exist.
   * 
   * @param obj Instance
   * @param gid group to which this object should reside
   * @return Managed object, may be new. Should never be null, but might be object representing null TCObject.
   */
  public TCObject lookupOrCreate(Object obj, GroupID gid);

  /**
   * Find identifier for existing instance
   * 
   * @param obj Object instance
   * @return Identifier
   */
  public ObjectID lookupExistingObjectID(Object obj);

  /**
   * Find named root object
   * 
   * @param name Root name
   * @return Root object
   */
  public Object lookupRoot(String name);

  /**
   * Find named root object in a particular group
   * 
   * @param name Root name
   * @return Root object
   */
  public Object lookupRoot(String name, GroupID groupID);

  /**
   * Find and create if necessary a root object for the specified named root. All dependent objects needed will be
   * faulted in to arbitrary depth.
   * 
   * @param rootName Root name
   * @param object Instance to use if new
   * @return New or existing object to use as root
   */
  public Object lookupOrCreateRootNoDepth(String rootName, Object object);

  /**
   * Find and create if necessary a root object for the specified named root. All dependent objects needed will be
   * faulted in, limited to the fault-count specified in the configuration.
   * 
   * @param name Root name
   * @param obj Instance to use if new
   * @return New or existing object to use as root
   */
  public Object lookupOrCreateRoot(String name, Object obj);

  /**
   * Find and create if necessary a root object for the specified named root.
   * 
   * @param name Root name
   * @param obj Instance to use if new
   * @param gid GroupID where the object needs to be created
   * @return New or existing object to use as root
   */
  public Object lookupOrCreateRoot(String name, Object obj, GroupID gid);

  /**
   * Find and create if necessary a root object for the specified named root. All dependent objects needed will be
   * faulted in, limited to the fault-count specified in the configuration.
   * 
   * @param name Root name
   * @param obj Instance to use if new
   * @param dsoFinal Specify whether this is root is considered final and whether an existing root can be replaced
   * @return New or existing object to use as root
   */
  public Object lookupOrCreateRoot(String name, Object obj, boolean dsoFinal);

  /**
   * Find managed object locally (don't fault in an object from the server).
   * 
   * @param id Identifier
   * @return Managed object or null if not in client
   */
  public TCObject lookupIfLocal(ObjectID id);

  /**
   * Find managed object by identifier
   * 
   * @param id Identifier
   * @return Managed object
   * @throws ClassNotFoundException If a class needed to hydrate cannot be found
   * @throws AbortedOperationException
   */
  public TCObject lookup(ObjectID id) throws ClassNotFoundException, AbortedOperationException;

  /**
   * Find a managed object quietly by id.
   *
   * @param id Identifier
   * @return Managed object
   * @throws ClassNotFoundException If a class needed to hydrate cannot be found
   * @throws AbortedOperationException
   */
  public TCObject lookupQuiet(ObjectID id) throws ClassNotFoundException, AbortedOperationException;

  /**
   * Find managed object by instance, which may be null
   * 
   * @param pojo Instance
   * @return Managed object if it exists, or null otherwise
   */
  public TCObject lookupExistingOrNull(Object pojo);

    /**
   * Create new peer object instance for the clazz, referred to through a WeakReference.
   * 
   * @param clazz The kind of class
   * @param dna The dna defining the object instance
   * @return Weak reference referring to the peer
   */
  public WeakReference createNewPeer(TCClass clazz, DNA dna);

  // public WeakObjectReference createNewPeer(TCClass clazz, DNA dna);

  /**
   * Create new peer object instance for the clazz, referred to through a WeakReference.
   * 
   * @param clazz The kind of class
   * @param size The size if this is an array
   * @param id The object identifier
   * @param parentID The parent object, if this is an inner object
   * @return Weak reference referring to the peer
   */
  public WeakReference createNewPeer(TCClass clazz, int size, ObjectID id, ObjectID parentID);

  // public WeakObjectReference createNewPeer(TCClass clazz, int size, ObjectID id, ObjectID parentID);

  /**
   * Get or create a reference to the managed class for this clazz
   * 
   * @param clazz The Java class
   * @return The Terracotta class
   */
  public TCClass getOrCreateClass(Class clazz);

  /**
   * Set the client transaction manager
   * 
   * @param txManager Transaction manager
   */
  public void setTransactionManager(ClientTransactionManager txManager);

  /**
   * Get the client transaction manager
   * 
   * @return Transaction manager
   */
  public ClientTransactionManager getTransactionManager();

  /**
   * Shutdown the client object manager
   */
  public void shutdown(boolean fromShutdownHook);

  /**
   * @return True if creation in progress
   */
  public boolean isCreationInProgress();

  /**
   * Create or replace a root value, typically used for replacable roots.
   * 
   * @param rootName Root name
   * @param root New root value
   */
  public Object createOrReplaceRoot(String rootName, Object root);

  /**
   * Create new WeakReference wrapper for the given id and peer object.
   * 
   * @param objectID The TCObjet
   * @param peer The peer object
   * @return the weak reference
   */
  WeakReference newWeakObjectReference(ObjectID objectID, Object peer);
  
  TCObject addLocalPrefetch(DNA object) throws ClassNotFoundException, AbortedOperationException;

}
