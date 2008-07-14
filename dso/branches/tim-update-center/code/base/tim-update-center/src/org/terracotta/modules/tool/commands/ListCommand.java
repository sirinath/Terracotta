/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang.StringUtils;
import org.terracotta.modules.tool.Module;
import org.terracotta.modules.tool.ModuleId;
import org.terracotta.modules.tool.Modules;

import com.google.inject.Inject;

import java.util.Collections;
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

  public void execute(CommandLine cli) {
    out().println("\n*** Terracotta Integration Modules for TC " + modules.tcVersion() + " ***\n");

    List<Module> latest = modules.listLatest();
    List<String> arglist = cli.getArgList();
    String keyword = arglist.isEmpty() ? "" : arglist.get(0);
    Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);

    if (arglist.size() > 1) out()
        .println("WARNING: Multiple keywords supplied. Only the first one will be used to filter the list.");

    for (Module module : latest) {
      ModuleId id = module.getId();

      if (keyword != null) {
        Matcher matcher = pattern.matcher(module.getSymbolicName());
        if (!matcher.find()) continue;
      }

      if (cli.hasOption('v') || cli.hasOption("details")) {
        String versions = module.getId().getVersion();
        List<String> allversions = module.getVersions();
        if (!allversions.isEmpty()) {
          Collections.reverse(allversions);
          versions = versions.concat("*, ").concat(StringUtils.join(allversions.iterator(), ", "));
        }
        out().println(id.getSymbolicName() + " (" + versions + ")");
        out().println("   Author   : " + module.getVendor());
        out().println("   Copyright: " + module.getCopyright());
        out().println("   Homepage : " + module.getWebsite());
        out().println("   Download : " + module.getRepoUrl());
        out().println();
        out().println("   " + module.getDescription().replaceAll("  ", " "));
        out().println();
      } else {
        out().println(id.getSymbolicName() + " (" + id.getVersion() + ")");
      }
    }
  }
}
