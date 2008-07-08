/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.terracotta.modules.tool.TimRepositoryStub;

import java.io.PrintWriter;
import java.io.StringWriter;

import junit.framework.TestCase;

/**
 * Test case for {@link ListCommand}.
 */
public class ListCommandTest extends TestCase {
  protected AbstractCommand command;
  protected StringWriter out;
  protected StringWriter err;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    command = new ListCommand(new TimRepositoryStub());
    this.out = new StringWriter();
    this.err = new StringWriter();
    command.setOut(new PrintWriter(this.out));
    command.setErr(new PrintWriter(this.err));
  }

  /**
   * Test method for {@link org.terracotta.modules.tool.commands.ListCommand#execute()}.
   */
  public final void testExecute() {
    command.execute();
    assertTrue(out.toString().length() > 0);
  }

}
