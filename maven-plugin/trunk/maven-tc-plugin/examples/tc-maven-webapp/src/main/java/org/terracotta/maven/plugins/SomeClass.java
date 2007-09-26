package org.terracotta.maven.plugins;

public class SomeClass {

  private int count;
  
  public String saySomething() {
    synchronized(this) {
      return "Hello World! " + count++;
    }
  }
  
}
