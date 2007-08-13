/*
 * 
 */
package org.terracotta.maven.plugins.tc.test;

import java.util.ArrayList;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
// import java.util.concurrent.CyclicBarrier;

public class SampleProcess {
  // root
  private static CyclicBarrier barrier;
  
  // root
  private static ArrayList nodes = new ArrayList();
  
  
  private final String nodeName;
  
  
  public SampleProcess(String nodeName) {
    this.nodeName = nodeName;
  }

  public static void main(String[] args) throws Exception {
    int totalNodes = SampleUtils.getTotalNodes();
    System.err.println("Number of nodes: " + totalNodes);
    barrier = new CyclicBarrier(totalNodes);
    
    SampleProcess node = new SampleProcess(System.getProperty("tc.nodeName"));
    node.process();
  }

  private void process() throws Exception {
    synchronized(nodes) {
      nodes.add(this);
    }

    synchronized(this) {
      // barrier.await();
      barrier.barrier();
    }
    
    synchronized(nodes) {
      System.err.println("Nodes: " + nodes);
    }
  }

  public String toString() {
    return nodeName;
  }
  
}
