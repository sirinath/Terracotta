/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.cli.commands;

import com.tc.statistics.cli.GathererConnection;
import com.tc.util.Assert;

public class CommandCreateSession extends AbstractCliCommand {
  public final static String[] ARGUMENT_NAMES = new String[] {"sessionId"};

  public String[] getArgumentNames() {
    return ARGUMENT_NAMES;
  }

  public void execute(final GathererConnection connection, final String[] arguments) {
    Assert.assertEquals(ARGUMENT_NAMES.length, arguments.length);
    connection.getGatherer().createSession(arguments[0]);
    System.out.println("> Session '" + arguments[0] + "' created.");
  }
}