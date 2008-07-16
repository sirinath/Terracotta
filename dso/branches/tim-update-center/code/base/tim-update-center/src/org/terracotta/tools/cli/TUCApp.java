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
import org.terracotta.modules.tool.config.Config;

import com.google.inject.Guice;
import com.google.inject.Injector;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class TUCApp {

  private static Config createConfig() {
    Properties props = new Properties();
    // TODO: use properties file for creating Config
    //props.load(TUCApp.class.getResourceAsStream("/org/terracotta/modules/tool/config.properties"));
    props.setProperty("tim-update.tcVersion", "2.6.2");
    String dataDir = new File(System.getProperty("user.home"), ".tc").getAbsolutePath();
    String dataFile = new File(dataDir, "tuc-manifest.xml").getAbsolutePath();
    props.setProperty("tim-update.dataFile", dataFile);
    props.setProperty("tim-update.dataFileUrl", "http://localhost/tuc-manifest.xml");
    return Config.createConfig(props);
  }

  public static void main(String args[]) {
    Config config = null;
    try {
      config = createConfig();
    }
    catch (Exception e) {
      System.err.println("Could not read configuration: " + e.getMessage());
      System.exit(1);
    }

    Injector injector = Guice.createInjector(new GuiceModule(config));

    CommandRegistry commandRegistry = injector.getInstance(CommandRegistry.class);
    commandRegistry.addCommand(injector.getInstance(HelpCommand.class));
    commandRegistry.addCommand(injector.getInstance(InfoCommand.class));
    commandRegistry.addCommand(injector.getInstance(InstallCommand.class));
    commandRegistry.addCommand(injector.getInstance(ListCommand.class));
    commandRegistry.addCommand(injector.getInstance(UpdateCommand.class));

    try {
      String commandName = "help";
      List<String> commandArgs = new ArrayList<String>();
      if (args.length != 0) {
        if (args[0].startsWith("-")) {
          Options options = new Options();
          options.addOption("h", "help", false, "Display help information.");
          CommandLineParser parser = new GnuParser();    
          parser.parse(options, args);
        } else {
          commandName = args[0];
          commandArgs = new ArrayList<String>(Arrays.asList(args));
          commandArgs.remove(0);
        }
      } 
      commandRegistry.executeCommand(commandName, commandArgs);
    } catch (CommandException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(2);
    }
  }

}
