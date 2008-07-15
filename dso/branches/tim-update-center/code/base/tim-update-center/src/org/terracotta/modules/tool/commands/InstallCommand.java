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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class InstallCommand extends AbstractCommand {

  private final Modules modules;

  @Inject
  public InstallCommand(Modules modules) {
    this.modules = modules;
    assert modules != null;
    options.addOption("all", "all", false, 
      "Install all compatible Integration Modules; ignores the name and version arguments specified.");
    options.addOption("groupid", "groupid", true,
      "The groupId used to qualify the name of the requested TIM. Ignored if the --all option is specified.");
    options.addOption("overwrite", "overwrite", false, "Overwrite if already installed.");
    options.addOption("pretend", "pretend", false, "Do not perform actual installation.");
  }
  
  private void install(Module module, boolean overwrite, boolean pretend) {
    StringWriter sw = new StringWriter();
    module.printDigest(new PrintWriter(sw));
    out().println("\n*** Installing " +  StringUtils.chomp(sw.toString()) + " and dependencies ***\n");
    module.install(overwrite, pretend, out());
    out().println();
  }
  
  private void install(String groupId, String artifactId, String version, boolean overwrite, boolean pretend) {
    Module module = null;

    if (version == null) module = modules.getLatest(groupId, artifactId);
    else module = modules.get(ModuleId.create(groupId, artifactId, version));
  
    if (module == null) {
      out().println();
      out().println("Unable to locate the Terracotta Integration Module named '" + artifactId + "'");
      out().println("It might be using a groupId other than '" + groupId + "'");
      out().println();
      return;
    }
    install(module, overwrite, pretend);
  }
  
  private void installAll(boolean overwrite, boolean pretend) {
    out().println("\n*** Installing all of the latest Integration Modules for TC " + modules.tcVersion() + " ***\n");
    List<Module> latest = modules.listLatest();
    for (Module module : latest) {
      install(module, overwrite, pretend);
    }
  }
  
  public void execute(CommandLine cli) throws CommandException {
    boolean overwrite = cli.hasOption("overwrite");
    boolean pretend = cli.hasOption("pretend");
    
    out().println();
    if (cli.hasOption("all")) {
      installAll(overwrite, pretend);
      return;
    } 
    
    List<String> args = cli.getArgList();
    if (args.isEmpty()) throw new CommandException("You need to at least specify the name of the Integration Module.");
    String artifactId = args.remove(0);
    String version = args.isEmpty() ? null : args.remove(0);
    String groupId = cli.getOptionValue("groupid", ModuleId.DEFAULT_GROUPID);
    install(groupId, artifactId, version, overwrite, pretend);
  }

}
