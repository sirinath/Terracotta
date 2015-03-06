package com.tc.object;

import com.tc.exception.TCObjectNotFoundException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.msg.RequestManagedObjectMessageFactory;
import com.tc.object.msg.RequestRootMessageFactory;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.TestCase;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RemoteObjectManagerImplTest extends TestCase {
  private static final TCLogger logger = TCLogging.getLogger(RemoteObjectManagerImplTest.class);
  private RemoteObjectManager remoteObjectManager;

  @Override
  public void setUp() throws Exception {
    RequestManagedObjectMessageFactory requestObjectMessageFactory = mock(RequestManagedObjectMessageFactory.class, RETURNS_MOCKS);
    SessionManager sessionManager = mock(SessionManager.class);
    when(sessionManager.isCurrentSession(any(NodeID.class), any(SessionID.class))).thenReturn(true);
    remoteObjectManager = new RemoteObjectManagerImpl(new GroupID(0), logger, mock(RequestRootMessageFactory.class),
        requestObjectMessageFactory, 1, sessionManager);
  }

  public void testLookupMultipleMissingResponses() throws Exception {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<?> f = executorService.submit(new Runnable() {
      @Override
      public void run() {
        remoteObjectManager.retrieve(new ObjectID(0));
      }
    });
    try {
      // Give lookup a second to get stuck on the wait.
      f.get(1, TimeUnit.SECONDS);
      fail("Lookup completed early");
    } catch (TimeoutException e) {
      // expected
    }
    // Simulate sending back multiple missing object responses.
    remoteObjectManager.objectsNotFoundFor(new SessionID(0), 1, Collections.singleton(new ObjectID(0)), new ClientID(1));
    remoteObjectManager.objectsNotFoundFor(new SessionID(0), 1, Collections.singleton(new ObjectID(0)), new ClientID(1));
    try {
      f.get();
      fail("Lookup should have failed.");
    } catch (ExecutionException e) {
      // expecting a TCObjectNotFound, throw otherwise.
      if (!(e.getCause() instanceof TCObjectNotFoundException)) {
        throw e;
      }
    }
  }
}