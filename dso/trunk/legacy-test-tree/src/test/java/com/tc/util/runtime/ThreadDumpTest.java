/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.util.runtime;

import com.tc.lcp.LinkedJavaProcess;
import com.tc.process.StreamCollector;
import com.tc.test.TCTestCase;
import com.tc.test.TestConfigObject;
import com.tc.util.runtime.ThreadDump.PID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ThreadDumpTest extends TCTestCase {

  public ThreadDumpTest() {
    if (Vm.isIBM()) {
      disableTest();
    }
  }

  // XXX: This test is known to fail under jrockit on the monkey. When we decide to deal with JRockit, we'll have to get
  // this thing working too. One alternative: If there is a magic jrockit specific way to get thread dumps, feel to try
  // it instead of kill -3 or CTRL-Break

  public void testDump() throws IOException, InterruptedException {
    LinkedJavaProcess process = new LinkedJavaProcess(ThreadDump.class.getName());

    List args = new ArrayList<String>();
    args.add("-D" + TestConfigObject.TC_BASE_DIR + "=" + System.getProperty(TestConfigObject.TC_BASE_DIR));
    args.add("-D" + TestConfigObject.PROPERTY_FILE_LIST_PROPERTY_NAME + "="
             + System.getProperty(TestConfigObject.PROPERTY_FILE_LIST_PROPERTY_NAME));
    if (Vm.isIBM()) {
      args.add("-Xdump:console");
      args.add("-Xdump:java:file=-");
    }
    process.addAllJvmArgs(args);

    System.err.println("JAVA ARGS: " + args);

    process.start();

    StreamCollector err = new StreamCollector(process.STDERR());
    StreamCollector out = new StreamCollector(process.STDOUT());

    err.start();
    out.start();

    process.waitFor();

    err.join();
    out.join();

    String stderr = err.toString();
    String stdout = out.toString();

    System.out.println("**** STDOUT BEGIN ****\n" + stdout + "\n**** STDOUT END ****");
    System.out.println("**** STDERR BEGIN ****\n" + stderr + "\n**** STDERR END ****");

    String expect = Vm.isIBM() ? "^^^^^^^^ console dump ^^^^^^^^" : "full thread dump";

    assertTrue(stderr.toLowerCase().indexOf(expect) >= 0 || stdout.toLowerCase().indexOf(expect) >= 0);
  }

  // public void testPidMechanismsAreSame() {
  // int jniPID = GetPid.getInstance().getPid();
  // int fallback = ThreadDump.getPIDUsingFallback().getPid();
  // assertEquals(jniPID, fallback);
  // }

  public void testFindAllJavaPIDs() {
    Set<PID> allPIDs = ThreadDump.findAllJavaPIDs();
    System.err.println("ALL: " + allPIDs);

    PID pid = ThreadDump.getPID();
    System.err.println("PID: " + pid);

    assertTrue(allPIDs.contains(pid));
  }
}
