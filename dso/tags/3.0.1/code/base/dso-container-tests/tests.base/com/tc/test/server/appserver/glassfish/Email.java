package com.tc.test.server.appserver.glassfish;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.email.EmailTask;
import org.apache.tools.ant.taskdefs.email.EmailTask.Encoding;

import com.tc.test.AppServerInfo;

import java.net.InetAddress;

// XXX: Temp code to find out how often glassfish retries are happening
class Email {

  public static void mail(String message, AppServerInfo info) {
    try {
      Project project = new Project();
      DefaultLogger logger = new DefaultLogger();
      logger.setErrorPrintStream(System.err);
      logger.setOutputPrintStream(System.out);
      logger.setMessageOutputLevel(Integer.MAX_VALUE);
      project.addBuildListener(logger);

      EmailTask email = new EmailTask();
      Encoding encoding = new EmailTask.Encoding();
      encoding.setValue(EmailTask.PLAIN);
      email.setEncoding(encoding);
      email.setProject(project);
      email.setFrom("teck@terracottatech.com");
      email.setToList("teck@terracottatech.com");
      email.setMailhost("mail.terracottatech.com");

      String subject = "Glassfish retry:";
      subject += " (" + info.toString() + ")";
      subject += " " + InetAddress.getLocalHost().getCanonicalHostName();
      subject += " " + System.getProperty("java.version");

      email.setSubject(subject);
      email.setMessage(message);
      email.execute();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

}
