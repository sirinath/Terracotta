/**
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.antrun.AbstractAntMojo;
import org.apache.maven.plugin.antrun.components.AntTargetConverter;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.codehaus.plexus.component.configurator.expression.DefaultExpressionEvaluator;

import com.tc.object.tools.BootJarTool;

/**
 * @author Eugene Kuleshov
 * 
 * @goal bootjar
 * @requiresDependencyResolution runtime
 * @execute phase="validate"
 */
public class BootjarMojo extends AbstractAntMojo {

  /**
   * The Maven project object
   * 
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * The plugin dependencies.
   * 
   * @parameter expression="${plugin.artifacts}"
   * @required
   * @readonly
   */
  private List artifacts;

  /**
   * @parameter expression="${jvm}"
   * @optional
   */
  private String jvm;

  /**
   * @parameter expression="${verbose}" default-value="false"
   */
  private boolean verbose;

  /**
   * @parameter expression="${overwrite}" default-value="false"
   */
  private boolean overwrite;

  /**
   * @parameter expression="${bootjar}" default-value="target/boot.jar"
   */
  private String bootJar;

  /**
   * @parameter expression="${config}" default-value="tc-config.xml"
   */
  private String config;

  /**
   * @parameter expression="${modules}"
   * @optional
   */
  private String modules;

  /**
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  public void execute() throws MojoExecutionException {
    final StringBuffer sb = new StringBuffer();
    for (Iterator it = artifacts.iterator(); it.hasNext();) {
      Artifact a = (Artifact) it.next();
      sb.append(a.getFile().getAbsolutePath()).append(File.pathSeparator);
    }

    Project antProject = new Project();
    antProject.setName("BootJar Project");

    antProject.addReference(AntTargetConverter.MAVEN_EXPRESSION_EVALUATOR_ID,
        new DefaultExpressionEvaluator());

    Java javaTask = new Java();
    javaTask.setProject(antProject);

    javaTask.setFork(true);

    if (jvm != null && jvm.length() > 0) {
      javaTask.setJvm(jvm);
    }

    javaTask.addSysproperty(variable("tc.classpath", sb.toString()));

    if (modules != null && modules.trim().length() > 0) {
      String location = modules.trim();
      File modulesDir = new File(location);
      if (modulesDir.isDirectory() && modulesDir.exists()) {
        location = modulesDir.toURI().toString();
      }
      javaTask.addSysproperty(variable("tc.tests.configuration.modules.url", location));
      getLog().debug("tc.tests.configuration.modules.url = " + location);
    }

    javaTask.setClasspath(new Path(antProject, sb.toString()));

    javaTask.setClassname(BootJarTool.class.getName());

    if (verbose) {
      javaTask.createArg().setLine("-v");
    }

    if (overwrite) {
      javaTask.createArg().setLine("-w");
    }

    javaTask.createArg().setLine("-o");
    javaTask.createArg().setFile(new File(bootJar));
    getLog().debug("bootjar file  = " + bootJar);

    javaTask.createArg().setLine("-f");
    javaTask.createArg().setFile(new File(config));
    getLog().debug("tc-config file  = " + config);

    Target target = new Target();
    target.setName("BootJar tool");
    target.setProject(antProject);
    target.addTask(javaTask);

    antProject.init();

    executeTasks(target, project);
  }

  private Environment.Variable variable(String key, String value) {
    Environment.Variable variable = new Environment.Variable();
    variable.setKey(key);
    variable.setValue(value);
    return variable;
  }

}
