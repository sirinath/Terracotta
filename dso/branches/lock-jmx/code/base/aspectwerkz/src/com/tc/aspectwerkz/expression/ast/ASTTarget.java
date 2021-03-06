/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

/* Generated By:JJTree: Do not edit this line. ASTTarget.java */

package com.tc.aspectwerkz.expression.ast;

import com.tc.aspectwerkz.expression.ExpressionInfo;


public class ASTTarget extends SimpleNode {

  private String m_identifier;

  public ASTTarget(int id) {
    super(id);
  }

  public ASTTarget(ExpressionParser p, int id) {
    super(p, id);
  }


  /**
   * Accept the visitor. *
   */
  public Object jjtAccept(ExpressionParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  public void setIdentifier(String identifier) {
    m_identifier = identifier;
  }

  public String getIdentifier() {
    return m_identifier;
  }

  public String getBoundedType(ExpressionInfo info) {
    // fast check if it seems to be a bounded type
    if (m_identifier.indexOf(".") < 0) {
      String boundedType = info.getArgumentType(m_identifier);
      if (boundedType != null) {
        return boundedType;
      }
    }
    return m_identifier;
  }
}
