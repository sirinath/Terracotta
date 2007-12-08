/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.schema.test.InstrumentedClassConfigBuilderImpl;
import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.LockConfigBuilderImpl;
import com.tc.config.schema.test.RootConfigBuilderImpl;
import com.tc.config.schema.test.TerracottaConfigBuilder;

public class LinkedBlockingQueueDynamicClientsTest extends ServerCrashingTestBase {
    private final static int NODE_COUNT = 2;

    public LinkedBlockingQueueDynamicClientsTest() {
      super(NODE_COUNT, new String[]{"-Dtc.classloader.writeToDisk=true"});
    }

    public void setUp() throws Exception {
      super.setUp();

      getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
      initializeTestRunner();
    }

    protected void createConfig(TerracottaConfigBuilder cb) {
      cb.getServers().getL2s()[0].setPersistenceMode(L2ConfigBuilder.PERSISTENCE_MODE_TEMPORARY_SWAP_ONLY);

      String testClassName = LinkedBlockingQueueDynamicClientsTestApp.class.getName();

      LockConfigBuilder lock1 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
      lock1.setMethodExpression("* " + testClassName + "*.*(..)");
      setLockLevel(lock1);

      cb.getApplication().getDSO().setLocks(new LockConfigBuilder[] { lock1 });

      RootConfigBuilder root = new RootConfigBuilderImpl();
      root.setFieldName(testClassName + ".barrier");
      root.setRootName("barrier");
      RootConfigBuilder root2 = new RootConfigBuilderImpl();
      root2.setFieldName(testClassName + ".queue");
      root2.setRootName("queue");
      cb.getApplication().getDSO().setRoots(new RootConfigBuilder[] { root, root2 });

      InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
      instrumented1.setClassExpression(testClassName + "*");

      cb.getApplication().getDSO().setInstrumentedClasses(
                                                          new InstrumentedClassConfigBuilder[] { instrumented1 });

    }
    
    protected void setLockLevel(LockConfigBuilder lock) {
      lock.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
    }

    protected void setReadLockLevel(LockConfigBuilder lock) {
      lock.setLockLevel(LockConfigBuilder.LEVEL_READ);
    }

    protected Class getApplicationClass() {
      return LinkedBlockingQueueDynamicClientsTestApp.class;
    }

}
