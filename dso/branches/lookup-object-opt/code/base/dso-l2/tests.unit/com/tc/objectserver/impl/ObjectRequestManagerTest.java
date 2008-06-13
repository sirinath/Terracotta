/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.Sink;
import com.tc.net.groups.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestID;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.impl.ObjectRequestManagerImpl.LookupContext;
import com.tc.objectserver.impl.ObjectRequestManagerImpl.ObjectRequestCache;
import com.tc.objectserver.impl.ObjectRequestManagerImpl.ResponseContext;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

public class ObjectRequestManagerTest extends TestCase {

  private ObjectRequestCache cache       = null;

  private Set                objectIDSet = null;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testContexts() {
    ObjectRequestManager orm = new TestObjectRequestManager();
    ClientID clientID = new ClientID(new ChannelID(1));
    ObjectRequestID objectRequestID = new ObjectRequestID(1);
    Set ids = createObjectIDSet();
    Set missingIds = new HashSet();
    Sink sink = new TestSink();
    Collection objs = null;

    LookupContext lookupContext = new LookupContext(orm, clientID, objectRequestID, ids, 0, "Thread-1", false, sink);
    assertEquals(lookupContext.getLookupIDs().size(), ids.size());
    assertEquals(0, lookupContext.getMaxRequestDepth());
    assertEquals(clientID, lookupContext.getRequestedNodeID());
    assertEquals(objectRequestID, lookupContext.getRequestID());
    assertEquals("Thread-1", lookupContext.getRequestingThreadName());
    assertEquals(false, lookupContext.isServerInitiated());
    
    ResponseContext responseContext = new ResponseContext(clientID, objs, ids, missingIds, false );
    assertEquals(clientID, responseContext.getRequestedNodeID());
  }

  public void testObjectRequestCache() {

    cache = new ObjectRequestCache();
    objectIDSet = createObjectIDSet();
    CacheThread cacheThread1 = new CacheThread(1, cache, objectIDSet);
    CacheThread cacheThread2 = new CacheThread(2, cache, objectIDSet);
    cacheThread1.start();
    cacheThread2.start();

    try {
      cacheThread1.join();

      cacheThread2.join();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }

    synchronized (cache) {
      for (Iterator i = objectIDSet.iterator(); i.hasNext();) {
        ObjectID id = (ObjectID) i.next();
        Set clients = cache.clients(id);
        assertEquals(0, clients.size());
      }
    }

    synchronized (cache) {
      assertEquals(0, cache.ids(new ClientID(new ChannelID(1))).size());
      assertEquals(0, cache.ids(new ClientID(new ChannelID(2))).size());

    }

  }

  private Set createObjectIDSet() {
    Set set = new HashSet();
    for (int i = 0; i < 100; i++) {
      set.add(new ObjectID(i));
    }
    return set;
  }

  private static class CacheThread extends Thread {

    private ObjectRequestCache cache = null;

    private Set                set   = null;

    private long               channnelID;

    public CacheThread(long channelID, ObjectRequestCache cache, Set set) {
      this.channnelID = channelID;
      this.cache = cache;
      this.set = set;
    }

    @Override
    public void run() {
      ClientID clientID = new ClientID(new ChannelID(channnelID));
      for (int j = 0; j < 10; j++) {
        synchronized (cache) {
          for (Iterator i = set.iterator(); i.hasNext();) {
            ObjectID id = (ObjectID) i.next();
            cache.add(clientID, id);
          }

          cache.remove(set);
        }
      }
    }
  }

  private static class TestObjectRequestManager implements ObjectRequestManager {

    protected ClientID        cID;

    protected ObjectRequestID oID;

    protected Set             lookupIds;

    protected int             maxDepth;

    protected boolean         serverInit;

    protected String          rThreadName;

    protected Collection      objColl;

    public void requestObjects(ClientID clientID, ObjectRequestID requestID, Set ids, int maxRequestDepth,
                               boolean serverInitiated, String requestingThreadName) {
      this.cID = clientID;
      this.oID = requestID;
      this.lookupIds = ids;
      this.maxDepth = maxRequestDepth;
      this.serverInit = serverInitiated;
      this.rThreadName = requestingThreadName;

    }

    public void sendObjects(ClientID requestedNodeID, Collection objs, Set requestedObjectIDs, Set missingObjectIDs,
                            boolean isServerInitiated) {

      //    
    }

  }

}
