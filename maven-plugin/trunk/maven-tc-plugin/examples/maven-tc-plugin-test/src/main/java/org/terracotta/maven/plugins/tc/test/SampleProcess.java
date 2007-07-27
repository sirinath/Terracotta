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
  private static ArrayList processes = new ArrayList();
  
  
  private final String nodeName;
  
  
  public SampleProcess(String nodeName) {
    this.nodeName = nodeName;
  }

  public static void main(String[] args) throws Exception {
    int nodeCount = Integer.parseInt(args[0]);
    barrier = new CyclicBarrier(nodeCount);
    
    SampleProcess process = new SampleProcess(System.getProperty("tc.nodeName"));
    process.process();
  }

  private void process() throws Exception {
    synchronized(processes) {
      processes.add(this);
    }

    synchronized(this) {
      // barrier.await();
      barrier.barrier();
    }
    
    synchronized(processes) {
      System.err.println(processes);
    }
  }

  public String toString() {
    return nodeName;
  }
  
}
