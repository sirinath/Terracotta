/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;

import java.util.List;

class HelpCommand extends AbstractCommand {

  public void execute(CommandLine cli) {
    List<String> topics = cli.getArgList();
    
    if (topics.isEmpty()) {
      System.out.println(loadHelp("GenericHelp"));
      System.out.println();
      return;
    }

    for (String topic : topics) {
      try {
        Command cmd = create(topic);
        System.out.println(cmd.help());
      } catch (CommandException e) {
        System.out.println("Command not supported: " + topic);
      }
      System.out.println();
    }
  }

  public String help() {
    return loadHelp(HelpCommand.class.getSimpleName());
  }

}
