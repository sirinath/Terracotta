package org.terracotta.management.cli.rest;

import org.terracotta.management.cli.Command;
import org.terracotta.management.cli.CommandInvocationException;

/**
 * @author Ludovic Orban
 */
public class SetupUrlEncodingCommand implements Command<Context> {
  @Override
  public void execute(Context context) throws CommandInvocationException {
  }

  @Override
  public String helpMessage() {
    return "URL-encode the output";
  }
}
