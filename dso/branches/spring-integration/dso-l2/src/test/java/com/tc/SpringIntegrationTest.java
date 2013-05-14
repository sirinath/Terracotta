package com.tc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.tc.objectserver.l1.api.ClientStateManager;

import static com.tc.util.Assert.assertNotNull;

/**
 * @author Eugene Shelestovich
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:context-common.xml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringIntegrationTest {

  private ClientStateManager clientStateManager;

  @Autowired
  public void setClientStateManager(final ClientStateManager clientStateManager) {
    this.clientStateManager = clientStateManager;
  }

  @Before
  public void setUp() {

  }

  @Test
  public void testFoo() {
    assertNotNull(clientStateManager);
  }

}
