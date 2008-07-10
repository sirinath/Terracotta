/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;

/**
 * Base interface implemented by all commands.
 * 
 * @author Jason Voegele (jvoegele@terracotta.org)
 */
public interface Command {

  /**
   * Descriptive help text for this command.
   */
  public String help();

  /**
   * Execute whatever actions that this command performs.
   * 
   * @param Any arguments that need to be passed to this command.
   */
  public void execute(CommandLine cli);
}
