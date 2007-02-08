/* $Id$
 * Created on Feb 8, 2007 by Jason Voegele
 * 
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

/**
 * Simple test case for ensuring that OSGi plugins are loaded properly.
 *
 * @author  Jason Voegele
 * @version $Revision$
 */
public class SimplePluginTest extends TransparentTestBase {

    private static final int NODE_COUNT = 3;

    public void doSetUp(TransparentTestIface t) throws Exception {
      t.getTransparentAppConfig().setClientCount(NODE_COUNT);
      t.initializeTestRunner();
    }

    protected Class getApplicationClass() {
      return SimplePluginTestApp.class;
    }

    public void testPluginsLoaded() {
        assertTrue(true);
    }

}
