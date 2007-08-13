/*
 * 
 */
package org.terracotta.maven.junit;

import java.util.ArrayList;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * @author Eugene Kuleshov
 */
public class SampleTest extends TestCase {

//  public CyclicBarrier barrier = new CyclicBarrier(SampleUtils.getTotalNodes());
  
  public ArrayList values = new ArrayList();
  
  public void testSample1() throws Exception {
    System.err.println("SampleTest.testSample1() starting");
    
    synchronized(values) {
      values.add("" + System.identityHashCode(this));
    }
    
//    System.out.println("SampleTest.testSample1() waiting on barrier");
//    barrier.barrier();
//    
//    synchronized(values) {
//      assertEquals(2, values.size());
//    }

    System.out.println("SampleTest.testSample1() completed");
  }

  public void testSample2() throws Exception {
    System.err.println("SampleTest.testSample2() starting");
    
    synchronized(values) {
      assertEquals(SampleUtils.getTotalNodes(), values.size());
    }
    
    System.out.println("SampleTest.testSample2() completed");
  }
  
  public static void main(String[] args) {
    TestRunner runner = new TestRunner();
    runner.doRun(new TestSuite(SampleTest.class));
  }
  
}
