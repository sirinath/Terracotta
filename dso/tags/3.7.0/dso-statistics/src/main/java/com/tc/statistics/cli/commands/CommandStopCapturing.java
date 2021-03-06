/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.cli.commands;

import com.tc.statistics.cli.GathererConnection;

public class CommandStopCapturing extends AbstractCliCommand {
  public String[] getArgumentNames() {
    return NO_ARGUMENTS;
  }

  public void execute(final GathererConnection connection, final String[] arguments) {
    connection.getGatherer().stopCapturing();
    System.out.println("> Capturing stopped.");
  }
}