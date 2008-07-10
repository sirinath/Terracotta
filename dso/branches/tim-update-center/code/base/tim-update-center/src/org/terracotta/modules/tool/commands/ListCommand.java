/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.terracotta.modules.tool.Tim;
import org.terracotta.modules.tool.TimRepository;

import java.util.List;

/**
 * Command class implementing the <code>list</code> command.
 */
class ListCommand extends AbstractCommand {
  
  private final TimRepository repository;

  public ListCommand() {
    this.repository = null;
  }
  
  public ListCommand(TimRepository repository) {
    this.repository = repository;
  }

  public String help() {
    return loadHelp(ListCommand.class.getSimpleName());
  }

  public void execute(CommandLine cli) {
    List<Tim> tims = repository.listAllCompatibleTims(getTerracottaVersion());
    out().println("*** TIM packages for Terracotta " + getTerracottaVersion() + " ***");
    for (Tim tim : tims) {
      out().println(tim.getTimId());
    }
  }
}
