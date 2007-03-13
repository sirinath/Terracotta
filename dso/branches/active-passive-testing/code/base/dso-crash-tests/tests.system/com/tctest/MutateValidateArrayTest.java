/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tctest.runner.TransparentAppConfig;

public class MutateValidateArrayTest extends TransparentTestBase {

  public static final int MUTATOR_NODE_COUNT   = 2;
  public static final int VALIDATOR_NODE_COUNT = 1;

  public void doSetUp(TransparentTestIface t) throws Exception {
    TransparentAppConfig tac = t.getTransparentAppConfig();
    tac.setClientCount(MUTATOR_NODE_COUNT).setIntensity(1).setValidatorCount(VALIDATOR_NODE_COUNT);
    t.initializeTestRunner(true);
  }

  protected Class getApplicationClass() {
    return MutateValidateArrayTestApp.class;
  }

  // protected void createConfig(TerracottaConfigBuilder cb) {
  // String testClassName = MutateValidateArrayTestApp.class.getName();
  // String testClassSuperName = AbstractMutateValidateTransparentApp.class.getName();
  //
  // LockConfigBuilder lock1 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
  // lock1.setMethodExpression("* " + testClassName + "*.*(..)");
  // lock1.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
  //
  // LockConfigBuilder lock2 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
  // lock2.setMethodExpression("* " + testClassSuperName + "*.*(..)");
  // lock2.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
  //
  // LockConfigBuilder lock3 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
  // lock3.setMethodExpression("* " + CyclicBarrier.class.getName() + "*.*(..)");
  // lock3.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
  //
  // cb.getApplication().getDSO().setLocks(new LockConfigBuilder[] { lock1, lock2, lock3 });
  //
  // RootConfigBuilder root1 = new RootConfigBuilderImpl();
  // root1.setFieldName(testClassName + ".myArrayTestRoot");
  //
  // RootConfigBuilder root2 = new RootConfigBuilderImpl();
  // root2.setFieldName(testClassName + ".validationArray");
  //
  // RootConfigBuilder root3 = new RootConfigBuilderImpl();
  // root3.setFieldName(testClassSuperName + ".mutatorBarrier");
  //
  // RootConfigBuilder root4 = new RootConfigBuilderImpl();
  // root4.setFieldName(testClassSuperName + ".allBarrier");
  //
  // cb.getApplication().getDSO().setRoots(new RootConfigBuilder[] { root1, root2, root3, root4 });
  //
  // InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
  // instrumented1.setClassExpression(testClassName + "*");
  //
  // InstrumentedClassConfigBuilder instrumented2 = new InstrumentedClassConfigBuilderImpl();
  // instrumented2.setClassExpression(testClassSuperName + "*");
  //
  // InstrumentedClassConfigBuilder instrumented3 = new InstrumentedClassConfigBuilderImpl();
  // instrumented3.setClassExpression(CyclicBarrier.class.getName() + "*");
  //
  // cb.getApplication().getDSO().setInstrumentedClasses(
  // new InstrumentedClassConfigBuilder[] { instrumented1,
  // instrumented2, instrumented3 });
  // }

}
