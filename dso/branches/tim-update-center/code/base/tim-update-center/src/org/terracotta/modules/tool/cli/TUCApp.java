/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.terracotta.modules.tool.GuiceModule;
import org.terracotta.modules.tool.commands.CommandRegistry;
import org.terracotta.modules.tool.commands.HelpCommand;
import org.terracotta.modules.tool.commands.InfoCommand;
import org.terracotta.modules.tool.commands.InstallCommand;
import org.terracotta.modules.tool.commands.ListCommand;
import org.terracotta.modules.tool.commands.UpdateCommand;

import com.google.inject.Guice;
import com.google.inject.Injector;

import java.util.Arrays;
import java.util.List;

public class TUCApp {

  public static void main(String args[]) {
    Injector injector = Guice.createInjector(new GuiceModule());

    CommandRegistry commandRegistry = injector.getInstance(CommandRegistry.class);
    commandRegistry.addCommand(injector.getInstance(HelpCommand.class));
    commandRegistry.addCommand(injector.getInstance(InfoCommand.class));
    commandRegistry.addCommand(injector.getInstance(InstallCommand.class));
    commandRegistry.addCommand(injector.getInstance(ListCommand.class));
    commandRegistry.addCommand(injector.getInstance(UpdateCommand.class));

    Options options = new Options();
    options.addOption("h", "help", false, "Display help information.");
    try {
      CommandLineParser parser = new GnuParser();
      CommandLine cli = parser.parse(options, args);

      List<String> argList = cli.getArgList();
      String cmdname = argList.isEmpty() ? "help" : (String) argList.remove(0);

      List<String> cmdargs = Arrays.asList(args);
      if (!cmdargs.isEmpty()) cmdargs = cmdargs.subList(1, cmdargs.size());

      commandRegistry.executeCommand(cmdname, cmdargs);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

}
