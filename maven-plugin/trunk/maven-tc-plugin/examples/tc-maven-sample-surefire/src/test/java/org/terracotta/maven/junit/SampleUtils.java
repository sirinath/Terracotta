/*
 * 
 */
package org.terracotta.maven.junit;

public class SampleUtils {

  public static int getTotalNodes() {
    try {
      return Integer.parseInt(System.getProperty("tc.numberOfNodes", "0"));
    } catch(Exception e) {
      return 0;
    }
  }

  
}
