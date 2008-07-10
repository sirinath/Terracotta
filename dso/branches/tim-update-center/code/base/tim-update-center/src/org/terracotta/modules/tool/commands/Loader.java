/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Loader {

  public Loader(String cmdname, String[] args) throws CommandException, ParseException {
    AbstractCommand cmd = AbstractCommand.create(cmdname);
    Options options = cmd.getOptions();

    CommandLineParser parser = new GnuParser();
    CommandLine cli = parser.parse(options, args);
    
    if (cli.hasOption('h') || cli.hasOption("help")) {
      System.out.println(cmd.help());
      return;
    }
    
    cmd.execute(cli);
  }
    
}
