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

import com.tc.server.TCServerMain;

/**
 * @author Eugene Kuleshov
 * 
 * @goal start
 * @requiresDependencyResolution runtime
 * @execute phase="validate"
 */
public class DsoStartMojo extends AbstractAntMojo {

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
   * @parameter expression="${spawn}"  default-value="true"
   */
  private boolean spawn;
  
  /**
   * @parameter expression="${config}" default-value="tc-config.xml"
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
    antProject.setName("DSO Start Project");

    antProject.addReference(AntTargetConverter.MAVEN_EXPRESSION_EVALUATOR_ID,
        new DefaultExpressionEvaluator());

    Java javaTask = new Java();
    javaTask.setProject(antProject);

    javaTask.setFork(true);
    javaTask.setSpawn(spawn);
    
    if (jvm != null && jvm.length() > 0) {
      javaTask.setJvm(jvm);
    }

    javaTask.setClasspath(new Path(antProject, sb.toString()));

    javaTask.setClassname(TCServerMain.class.getName());

    javaTask.createArg().setLine("-f");
    javaTask.createArg().setFile(new File(config));
    getLog().debug("tc-config file  = " + config);

    if (name != null && name.length() > 0) {
      javaTask.createArg().setLine("-n");
      javaTask.createArg().setValue(name);
      getLog().debug("server name = " + name);
    }

    Target target = new Target();
    target.setName("DSO Start tool");
    target.setProject(antProject);
    target.addTask(javaTask);

    antProject.init();

    executeTasks(target, project);
  }

}
