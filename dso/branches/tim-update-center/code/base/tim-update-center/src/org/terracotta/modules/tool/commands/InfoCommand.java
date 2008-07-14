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

public class InfoCommand extends AbstractCommand {

  private final Modules modules;

  @Inject
  public InfoCommand(Modules modules) {
    this.modules = modules;
    assert modules != null;
    options.addOption("groupid", "groupid", true, "The groupId used to qualify the name of the requested TIM");
  }

  public void execute(CommandLine cli) throws CommandException {
    List<String> args = cli.getArgList();
    if (args.isEmpty()) { throw new CommandException(
                                                     "You need to at least specify the name of the TIM whose information you'd like to review."); }

    String groupId = cli.getOptionValue("groupid", ModuleId.DEFAULT_GROUPID);
    String artifactId = args.remove(0);
    String version = args.isEmpty() ? null : args.remove(0);
    Module module = (version == null) ? modules.getLatest(groupId, artifactId) : modules.get(ModuleId
        .create(groupId, artifactId, version));

    if (module == null) { throw new CommandException("Not found."); }

    ModuleId id = module.getId();
    groupId = id.isDefaultGroupId() ? "" : " [" + id.getGroupId() + "]";
    List<String> versions = module.getVersions();
    String other = versions.isEmpty() ? "" : " (also: " + StringUtils.join(versions.iterator(), ", ") + ")";

    out().println();
    out().println("   ArtifactId: " + id.getArtifactId());
    out().println("   Version   : " + id.getVersion() + other);
    out().println("   GroupId   : " + id.getGroupId());
    out().println();
    out().println("   TC Version: " + module.getTcVersion());
    List<ModuleId> dependencies = module.dependencies();
    if (!dependencies.isEmpty()) {
      out().print("   Requires  : ");
      Collections.sort(dependencies);
      for (ModuleId mid : dependencies) {
        out().println(
                      mid.getArtifactId() + (mid.isDefaultGroupId() ? "" : " [" + mid.getGroupId() + "] ") + " ("
                          + mid.getVersion() + ")");
        out().print("               ");
      }
    }

    out().println();
    out().println("   Author    : " + module.getVendor());
    out().println("   Copyright : " + module.getCopyright());
    out().println("   Homepage  : " + module.getWebsite());
    out().println("   Download  : " + module.getRepoUrl());
    out().println();
    out().println("   " + module.getDescription().replaceAll("  ", " "));
    out().println();
  }

}
