package com.terracotta.management;

import static org.hamcrest.Matchers.equalTo;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * @author: Anthony Dahanne
 */
public class ApplicationTsaV1Test  extends JerseyApplicationTestCommon {
  @Test
  public void testGetClasses() throws Exception {
    ApplicationTsaV1 applicationEhCache = new ApplicationTsaV1();
    Set<Class<?>> applicationClasses = applicationEhCache.getResourceClasses();
    Set<Class<?>> annotatedClasses = annotatedClassesFound();
    Assert.assertThat(annotatedClasses, equalTo(applicationClasses));
  }

}
