/*
 * 
 */
package org.terracotta.maven.plugins.tc.test;

import java.util.ArrayList;

import junit.framework.TestCase;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

public class SampleTest extends TestCase {

  public CyclicBarrier barrier = new CyclicBarrier(2);
  
  public ArrayList values = new ArrayList();
  
  public void testSample() throws Exception {
    System.err.println("Starting test");
    
    synchronized(values) {
      values.add("" + System.identityHashCode(this));
    }
    
    System.err.println("Waiting on barrier");
    barrier.barrier();
    
    synchronized(values) {
      assertEquals(2, values.size());
    }

    System.err.println("Completed");
  }
  
}
