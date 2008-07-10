/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry of {@link Command} objects.
 *
 * @author Jason Voegele (jvoegele@terracotta.org)
 */
public class CommandRegistry {
  private Map<String, Command> commands = new HashMap<String, Command>();

  public void addCommand(Command command) {
    commands.put(command.name(), command);
  }

  public Command getCommand(String commandName) {
    return commands.get(commandName);
  }

  public Set<String> commandNames() {
    return commands.keySet();
  }

  public void executeCommand(String commandName, String[] args) throws ParseException {
    Command cmd = getCommand(commandName);
    Options options = cmd.options();

    CommandLineParser parser = new GnuParser();
    CommandLine cli = parser.parse(options, args);

    if (cli.hasOption('h') || cli.hasOption("help")) {
      System.out.println(cmd.help());
      return;
    }

    cmd.execute(cli);
  }

  public void executeCommand(String commandName, List<String> args) throws ParseException {
    executeCommand(commandName, args.toArray(new String[args.size()]));
  }
}
