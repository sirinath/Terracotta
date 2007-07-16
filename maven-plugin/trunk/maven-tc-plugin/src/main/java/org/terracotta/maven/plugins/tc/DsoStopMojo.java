/**
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.antrun.AbstractAntMojo;
import org.apache.maven.plugin.antrun.components.AntTargetConverter;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.codehaus.plexus.component.configurator.expression.DefaultExpressionEvaluator;

import com.tc.admin.TCStop;

/**
 * @author Eugene Kuleshov
 * 
 * @goal stop
 * @requiresDependencyResolution runtime
 * @execute phase="validate"
 */
public class DsoStopMojo extends AbstractAntMojo {

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
   * @parameter expression="${config}"
   * @optional
   */
  private String config;
  
  /**
   * @parameter expression="${name}"
   * @optional
   */
  private String name;

  
  public void execute() throws MojoExecutionException, MojoFailureException {
    final StringBuffer sb = new StringBuffer();
    for (Iterator it = artifacts.iterator(); it.hasNext();) {
      Artifact a = (Artifact) it.next();
      sb.append(a.getFile().getAbsolutePath()).append(File.pathSeparator);
    }

    Project antProject = new Project();
    antProject.setName("DSO Stop Project");

    antProject.addReference(AntTargetConverter.MAVEN_EXPRESSION_EVALUATOR_ID,
        new DefaultExpressionEvaluator());

    Java javaTask = new Java();
    javaTask.setProject(antProject);


    if (jvm != null && jvm.length() > 0) {
      javaTask.setFork(true);
      javaTask.setJvm(jvm);
    }

    javaTask.setClasspath(new Path(antProject, sb.toString()));

    javaTask.setClassname(TCStop.class.getName());

    StringBuffer args = new StringBuffer();
    if (config != null && config.length() > 0) {
      args.append(" -f ").append(config);
    }
    if (name != null && name.length() > 0) {
      args.append(" -n ").append(name);
    }
    javaTask.setArgs(args.toString());

    Target target = new Target();
    target.setName("DSO Stop tool");
    target.setProject(antProject);
    target.addTask(javaTask);

    antProject.init();

    executeTasks(target, project);
  }

}
