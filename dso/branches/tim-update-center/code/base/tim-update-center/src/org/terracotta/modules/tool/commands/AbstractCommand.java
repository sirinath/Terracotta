/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import com.tc.util.ProductInfo;

import java.io.PrintWriter;

/**
 * Convenience base class for commands that offers common facilities used by
 * many command implementations.
 *
 * @author Jason Voegele (jvoegele@terracotta.org)
 */
public abstract class AbstractCommand implements Command {
  private PrintWriter out = new PrintWriter(System.out);
  private PrintWriter err = new PrintWriter(System.err);

  protected String getTerracottaVersion() {
    return ProductInfo.getInstance().version();
  }

  /**
   * Returns the <code>PrintWriter</code> to which commands should print standard output.
   */
  protected PrintWriter out() {
    return this.out;
  }

  /**
   * Sets the <code>PrintWriter</code> that commands will use for standard output.
   */
  protected void setOut(PrintWriter out) {
    this.out = out;
  }

  /**
   * Returns the <code>PrintWriter</code> to which commands should print error output.
   */
  protected PrintWriter err() {
    return this.err;
  }

  /**
   * Sets the <code>PrintWriter</code> that commands will use for error output.
   */
  protected void setErr(PrintWriter err) {
    this.err = err;
  }

}
