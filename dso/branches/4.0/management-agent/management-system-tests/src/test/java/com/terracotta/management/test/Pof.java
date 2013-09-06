package com.terracotta.management.test;

import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.junit.matchers.JUnitMatchers.either;

/**
 * @author: Anthony Dahanne
 */
public class Pof {


  @Test
  public void plof() {
    assertThat("127.0.0.", either(containsString("localhost")).or(containsString("127.0.0.1")));
  }

}
