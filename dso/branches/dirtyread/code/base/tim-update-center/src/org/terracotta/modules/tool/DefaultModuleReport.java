/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.tc.util.runtime.Os;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

public class DefaultModuleReport extends ModuleReport {

  private static final String INSTALLED_MARKER    = "+";
  private static final String NOTINSTALLED_MARKER = "-";
  private static final String OUTOFDATE_MARKER    = "!";

  private static final int    INDENT_WIDTH        = 3;
  private static final String INDENT_CHAR         = " ";
  private static final String DEFAULT_GROUPID     = "org.terracotta.modules";
  private static final String LEGEND              = "(+) Installed  (-) Not installed  (!) Installed but newer version exists";

  private String canonicalize(File path) {
    try {
      return path.getCanonicalFile().toString();
    } catch (IOException e) {
      return path.toString();
    }
  }

  private String marker(Module module) {
    if (!module.isInstalled()) return NOTINSTALLED_MARKER;
    return module.isLatest() ? INSTALLED_MARKER : OUTOFDATE_MARKER;
  }

  private boolean isUsingDefaultGroupId(AbstractModule module) {
    return DEFAULT_GROUPID.equals(module.groupId());
  }

  private String indent(String text) {
    StringBuffer result = new StringBuffer();
    String[] lines = StringUtils.split(text, '\n');
    for (String line : lines) {
      result.append(StringUtils.repeat(INDENT_CHAR, INDENT_WIDTH)).append(line).append('\n');
    }
    return StringUtils.chomp(result.toString());
  }

  @Override
  public String title(AbstractModule module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);

    out.print(module.artifactId().concat(" ").concat(module.version()).concat(""));
    if (!isUsingDefaultGroupId(module)) out.print(" [" + module.groupId() + "]");

    String text = writer.toString();
    return StringUtils.chomp(StringUtils.trim(text));
  }

  @Override
  public String headline(AbstractModule module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);

    out.print("(" + marker((Module) module) + ") ");
    out.print(title(module));

    String text = writer.toString();
    return StringUtils.chomp(StringUtils.trim(text));
  }

  @Override
  public String digest(Module module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);
    out.println(headline(module));
    out.println(" ");

    out.println("Installed: " + (module.isInstalled() ? "YES" : "NO"));
    out.println(" ");
    if (!StringUtils.isEmpty(module.vendor())) out.println("Author   : " + module.vendor());
    if (!StringUtils.isEmpty(module.copyright())) out.println("Copyright: " + module.copyright());
    if (!StringUtils.isEmpty(module.website().toString())) out.println("Homepage : " + module.website());
    if (!StringUtils.isEmpty(module.contactAddress())) out.println("Contact  : " + module.contactAddress());
    if (!StringUtils.isEmpty(module.docUrl().toString())) out.println("Docs     : " + module.docUrl());
    out.println("Download : " + module.repoUrl());
    out.println("Status   : " + module.tcProjectStatus() + "\n ");

    if (!StringUtils.isEmpty(module.description())) {
      out.println(module.description().replaceAll("\n[ ]+", "\n"));
      out.println();
    }

    String compatibility = module.tcVersion().equals("*") ? "any Terracotta version." : "TC " + module.tcVersion();
    out.println("Compatible with " + compatibility);

    String text = indent(writer.toString());
    return StringUtils.chomp(StringUtils.trim(text));
  }

  private String dependencies(Module module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);

    out.println("Dependencies:\n ");
    if (module.dependencies().isEmpty()) out.println(indent("None."));

    for (AbstractModule dependency : module.dependencies()) {
      String line = "o " + dependency.artifactId() + " " + dependency.version();
      if (!isUsingDefaultGroupId(dependency)) line = line.concat(" [" + dependency.groupId() + "]");
      out.println(indent(line));
    }

    String text = writer.toString();
    return StringUtils.chomp(StringUtils.trim(text));
  }

  private String mavenCoordinates(Module module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);
    out.println("Maven Coordinates:\n ");
    out.println(indent("groupId   : " + module.groupId()));
    out.println(indent("artifactId: " + module.artifactId()));
    out.println(indent("version   : " + module.version()));

    String text = writer.toString();
    return StringUtils.chomp(StringUtils.trim(text));
  }

  private String configInfo(Module module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);

    Element parent = new Element("modules");
    Element child = new Element("module");
    parent.addContent(child);
    child.setAttribute("name", module.artifactId());
    child.setAttribute("version", module.version());
    if (!isUsingDefaultGroupId(module)) child.setAttribute("group-id", module.groupId());

    out.println("Configuration:\n ");
    StringWriter sw = new StringWriter();
    Format formatter = Format.getPrettyFormat();
    formatter.setIndent(StringUtils.repeat(" ", INDENT_WIDTH));
    XMLOutputter xmlout = new XMLOutputter(formatter);
    try {
      xmlout.output(parent, new PrintWriter(sw));
      out.println(indent(sw.toString()));
    } catch (IOException e) {
      out.println(e.getMessage());
    }

    String text = writer.toString();
    return StringUtils.chomp(StringUtils.trim(text));
  }

  private String installationInfo(Module module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);

    if (module.isInstalled()) {
      Modules owner = module.owner();

      // XXX compute actual location if installed - remember that
      // a TIM may also be installed at the root of the repository
      File location = new File(owner.repository(), module.installPath().toString());
      location = new File(location, module.filename());
      File actualLocation = location.exists() ? location.getParentFile() : owner.repository();
      out.println("Installed at " + canonicalize(actualLocation));

      if (module.isLatest()) out.println("This is the latest version.\n ");
      else out.println("A newer version is available.\n ");
    }

    if (module.versions().isEmpty()) {
      out.println("There are no other versions of this TIM that are compatible with TC " + module.tcVersion());
    } else {
      out.println("The following versions are also available:\n ");
      List<Module> siblings = module.siblings();
      Collections.reverse(siblings);
      for (Module sibling : siblings) {
        String marker = "(" + (sibling.isInstalled() ? INSTALLED_MARKER : NOTINSTALLED_MARKER) + ") ";
        String line = marker + sibling.version();
        out.println(indent(line));
      }
      out.println("\n ");
    }

    // XXX NEED A BETTER WAY HEURISTIC PRESENT THIS INSTALL/UPDATE INSTRUCTION
    if (!module.isInstalled() || !module.isLatest()) {
      out.println("Issue the following command to install the latest version:\n ");
      String script = "tim-get.";
      script += (Os.isWindows() ? "bat" : "sh");
      script += INDENT_CHAR + (module.isInstalled() ? "update" : "install");
      script += INDENT_CHAR + module.artifactId();
      script += INDENT_CHAR + module.version();
      if (isUsingDefaultGroupId(module)) script += INDENT_CHAR + module.groupId();
      out.println(indent(script));
    }

    String text = writer.toString();
    return StringUtils.chomp(StringUtils.trim(text));
  }

  @Override
  public String summary(Module module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);

    out.println(digest(module));
    out.println();
    out.println(indent(dependencies(module)));
    out.println();
    out.println(indent(mavenCoordinates(module)));
    out.println();
    out.println(indent(configInfo(module)));
    out.println();
    out.println(indent(installationInfo(module)));

    String text = writer.toString();
    return StringUtils.chomp(StringUtils.trim(text));
  }

  @Override
  public String footer(AbstractModule module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);
    out.println(LEGEND);
    String text = writer.toString();
    return StringUtils.chomp(StringUtils.trim(text));
  }

  @Override
  public String header(AbstractModule module) {
    return StringUtils.EMPTY;
  }
}
