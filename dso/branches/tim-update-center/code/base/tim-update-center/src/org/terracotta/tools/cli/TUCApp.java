/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.tools.cli;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.terracotta.modules.tool.GuiceModule;
import org.terracotta.modules.tool.commands.CommandException;
import org.terracotta.modules.tool.commands.CommandRegistry;
import org.terracotta.modules.tool.commands.HelpCommand;
import org.terracotta.modules.tool.commands.InfoCommand;
import org.terracotta.modules.tool.commands.InstallCommand;
import org.terracotta.modules.tool.commands.ListCommand;
import org.terracotta.modules.tool.commands.UpdateCommand;

import com.google.inject.Guice;
import com.google.inject.Injector;

import java.util.ArrayList;
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

    try {
      String cmdname = "help";
      List<String> cmdargs = new ArrayList<String>();
      if (args.length != 0) {
        if (args[0].startsWith("-")) {
          Options options = new Options();
          options.addOption("h", "help", false, "Display help information.");
          CommandLineParser parser = new GnuParser();    
          parser.parse(options, args);
        } else {
          cmdname = args[0];
          cmdargs = new ArrayList<String>(Arrays.asList(args));
          cmdargs.remove(0);
        }
      } 
      commandRegistry.executeCommand(cmdname, cmdargs);
    } catch (CommandException e) {
      System.err.println();
      System.err.println(e.getMessage());
      System.exit(1);
    } catch (Exception e) {
      System.err.println();
      System.err.println(e.getMessage());
      System.exit(2);
    }
  }

}
