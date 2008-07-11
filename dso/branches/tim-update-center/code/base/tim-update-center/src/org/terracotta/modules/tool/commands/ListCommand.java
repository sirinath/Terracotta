/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.terracotta.modules.tool.Module;
import org.terracotta.modules.tool.Modules;

import com.google.inject.Inject;

import java.util.List;

/**
 * Command class implementing the <code>list</code> command.
 */
public class ListCommand extends AbstractCommand {
  
  private final Modules repository;

  @Inject
  public ListCommand(Modules repository) {
    this.repository = repository;
    assert repository != null;
    options.addOption("v", "details", false, "Display detailed information");
  }

  public void execute(CommandLine cli) {
    List<Module> tims = repository.listAllCompatibleTims(getTerracottaVersion());
    out().println("*** TIM packages for Terracotta " + getTerracottaVersion() + " ***");
    for (Module tim : tims) {
      out().println(tim.getTimId());
    }
  }
}
