/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.container;

public interface ContainerStateFactory {
  public ContainerState newContainerState(String containerId);
}