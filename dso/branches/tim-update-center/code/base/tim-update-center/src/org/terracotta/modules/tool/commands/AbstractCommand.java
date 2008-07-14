/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;

import com.tc.util.ProductInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convenience base class for commands that offers common facilities used by many command implementations.
 * 
 * @author Jason Voegele (jvoegele@terracotta.org)
 */
public abstract class AbstractCommand implements Command {
  protected Options   options = createOptions();

  private PrintWriter out     = new PrintWriter(System.out, true);
  private PrintWriter err     = new PrintWriter(System.err, true);

  protected final Options createOptions() {
    Options opts = new Options();
    opts.addOption("h", "help", false, "Display help information.");
    return opts;
  }

  public String help() {
    String text = loadHelp();
    return (text.length() == 0) ? name() + " [options] [arguments]" : text;
  }

  private static final String  EMPTY_HELP_STRING = "";

  protected String loadHelp() {
    return loadHelp(getClass().getSimpleName());
  }
  
  public void printHelp() {
    out().println(help());
  }

  private static final Pattern classNamePattern  = Pattern.compile("([A-Za-z0-9_]+)Command");

  /**
   * Default implementation that returns the name of the class (in lowercase) minus the "Command" suffix if it has one,
   */
  public String name() {
    String commandName = getClass().getSimpleName();
    Matcher matcher = classNamePattern.matcher(commandName);
    if (matcher.matches()) {
      commandName = matcher.group(1);
    }
    return commandName.toLowerCase();
  }

  public Options options() {
    return options;
  }

  protected String loadHelp(String topic) {
    String resourceName = "/" + getClass().getPackage().getName().replace('.', '/') + "/" + topic + ".help";
    InputStream in = AbstractCommand.class.getResourceAsStream(resourceName);
    try {
      if (in == null) return EMPTY_HELP_STRING;
      List<String> lines = IOUtils.readLines(in);
      StringBuffer buffer = new StringBuffer();
      for (String line : lines) {
        buffer.append(line);
        buffer.append(System.getProperty("line.separator"));
      }
      return buffer.toString();
    } catch (IOException e) {
      this.err().println("Unable to load resource: " + resourceName);
      return EMPTY_HELP_STRING;
    }
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
