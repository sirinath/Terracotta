/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.terracotta.modules.tool.Module;
import org.terracotta.modules.tool.Modules;

import com.google.inject.Inject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command class implementing the <code>list</code> command.
 */
public class ListCommand extends AbstractCommand {

  private final Modules modules;

  @Inject
  public ListCommand(Modules modules) {
    this.modules = modules;
    assert modules != null;
    options.addOption("v", "details", false, "Display detailed information");
  }
  
  private boolean isQualified(List<String> keywords, String text) {
    if (keywords.isEmpty()) return true;
    for (String keyword : keywords) {
      Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(text);
      if (matcher.find()) return true;
    }
    return false;
  }
  
  public void execute(CommandLine cli) {
    List<Module> latest = modules.listLatest();
    List<String> keywords = cli.getArgList();
    out().println("\n*** Terracotta Integration Modules for TC " + modules.tcVersion() + " ***\n");
    for (Module module : latest) {
      if (!isQualified(keywords, module.getSymbolicName())) continue;
      if (cli.hasOption('v') || cli.hasOption("details")) module.printSummary(out());
      else module.printDigest(out());
    }
  }
}
