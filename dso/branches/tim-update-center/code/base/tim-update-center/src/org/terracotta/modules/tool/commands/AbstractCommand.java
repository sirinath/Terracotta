/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.tc.util.ProductInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * Convenience base class for commands that offers common facilities used by
 * many command implementations.
 *
 * @author Jason Voegele (jvoegele@terracotta.org)
 */
public abstract class AbstractCommand implements Command {
  private PrintWriter out = new PrintWriter(System.out);
  private PrintWriter err = new PrintWriter(System.err);

  static AbstractCommand create(String name) throws CommandException {
    try {
      Class klazz = Class.forName("org.terracotta.modules.tool.commands." + StringUtils.capitalize(name) + "Command");
      return (AbstractCommand) klazz.newInstance();
    } catch (ClassNotFoundException e) {
      throw new CommandException("Unknown command: " + name);
    } catch (InstantiationException e) {
      throw new CommandException("Unable to create command: " + name, e);
    } catch (IllegalAccessException e) {
      throw new CommandException("Unable to access command: " + name, e);
    }
  }

  static String loadHelp(String topic) {
    String resourceName = topic + ".help";
    InputStream in = AbstractCommand.class.getResourceAsStream(resourceName);
    try {
      if (in == null) return "missing resource: " + resourceName;
      List<String> lines = IOUtils.readLines(in);
      StringBuffer buffer = new StringBuffer();
      for (String line : lines) { 
        buffer.append(line);
        buffer.append(System.getProperty("line.separator"));
      }
      return buffer.toString();
    } catch (IOException e) {
      return "unable to load resource: " + resourceName;
    }
  }
  

  protected Options getOptions() {
    Options options = new Options();
    options.addOption("h", "help", false, "Help");
    return options;
  }
  
  protected String getTerracottaVersion() {
    return ProductInfo.getInstance().version();
  }

  /**
   * Returns the <code>PrintWriter</code> to which commands should print standard output.
   */
  protected PrintWriter out() {
    return this.out;
  }

  /**
   * Sets the <code>PrintWriter</code> that commands will use for standard output.
   */
  protected void setOut(PrintWriter out) {
    this.out = out;
  }

  /**
   * Returns the <code>PrintWriter</code> to which commands should print error output.
   */
  protected PrintWriter err() {
    return this.err;
  }

  /**
   * Sets the <code>PrintWriter</code> that commands will use for error output.
   */
  protected void setErr(PrintWriter err) {
    this.err = err;
  }

}
