/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool;

import java.util.ArrayList;
import java.util.List;

/**
 * Stub implementation of the {@link Modules} interface used by tests.
 */
public class TimRepositoryStub implements Modules {

  public List<Module> list() {
    return new ArrayList<Module>();
  }
}
