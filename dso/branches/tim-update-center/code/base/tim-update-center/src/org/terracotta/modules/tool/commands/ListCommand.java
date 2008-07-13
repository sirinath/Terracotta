/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.terracotta.modules.tool.Module;
import org.terracotta.modules.tool.Modules;

import com.google.inject.Inject;

/**
 * Command class implementing the <code>list</code> command.
 */
public class ListCommand extends AbstractCommand {

  private final Modules modules;

  @Inject
  public ListCommand(Modules modules) {
    this.modules = modules;
    assert modules != null;
    options.addOption("v", "details", false, "Display detailed information");
  }

  public void execute(CommandLine cli) {
    for (Module module : this.modules.list()) {
      System.out.println(module + " " + (module.isLatest() ? "*" : ""));
    }

    System.out.println("---");

    for (Module module : this.modules.listLatest()) {
      System.out.println(module);
      for (String version : module.getVersions()) {
        System.out.println("- " + version);
      }
    }
    
    System.out.println("---");
    
    for (Module module : this.modules.listLatest()) {
      System.out.println(module);
      module.install();
    }
  }
}
