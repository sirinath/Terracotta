/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.expression;

import com.tc.aspectwerkz.expression.ast.ASTAnd;
import com.tc.aspectwerkz.expression.ast.ASTArgParameter;
import com.tc.aspectwerkz.expression.ast.ASTArgs;
import com.tc.aspectwerkz.expression.ast.ASTAttribute;
import com.tc.aspectwerkz.expression.ast.ASTCall;
import com.tc.aspectwerkz.expression.ast.ASTCflow;
import com.tc.aspectwerkz.expression.ast.ASTCflowBelow;
import com.tc.aspectwerkz.expression.ast.ASTClassPattern;
import com.tc.aspectwerkz.expression.ast.ASTConstructorPattern;
import com.tc.aspectwerkz.expression.ast.ASTExecution;
import com.tc.aspectwerkz.expression.ast.ASTExpression;
import com.tc.aspectwerkz.expression.ast.ASTFieldPattern;
import com.tc.aspectwerkz.expression.ast.ASTGet;
import com.tc.aspectwerkz.expression.ast.ASTHandler;
import com.tc.aspectwerkz.expression.ast.ASTHasField;
import com.tc.aspectwerkz.expression.ast.ASTHasMethod;
import com.tc.aspectwerkz.expression.ast.ASTIf;
import com.tc.aspectwerkz.expression.ast.ASTMethodPattern;
import com.tc.aspectwerkz.expression.ast.ASTModifier;
import com.tc.aspectwerkz.expression.ast.ASTNot;
import com.tc.aspectwerkz.expression.ast.ASTOr;
import com.tc.aspectwerkz.expression.ast.ASTParameter;
import com.tc.aspectwerkz.expression.ast.ASTPointcutReference;
import com.tc.aspectwerkz.expression.ast.ASTRoot;
import com.tc.aspectwerkz.expression.ast.ASTSet;
import com.tc.aspectwerkz.expression.ast.ASTStaticInitialization;
import com.tc.aspectwerkz.expression.ast.ASTTarget;
import com.tc.aspectwerkz.expression.ast.ASTThis;
import com.tc.aspectwerkz.expression.ast.ASTWithin;
import com.tc.aspectwerkz.expression.ast.ASTWithinCode;
import com.tc.aspectwerkz.expression.ast.ExpressionParserVisitor;
import com.tc.aspectwerkz.expression.ast.Node;
import com.tc.aspectwerkz.expression.ast.SimpleNode;

/**
 * TODO: do we need that, there is a dump() method in jjtree API
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 * @author Michael Nascimento
 */
public class DumpVisitor implements ExpressionParserVisitor {
  private Node m_root;

  private int indent = 0;

  private DumpVisitor(final Node root) {
    m_root = root;
  }

  public static void dumpAST(final Node root) {
    DumpVisitor dumper = new DumpVisitor(root);
    dumper.visit((SimpleNode) dumper.m_root, null);
  }

  public Object visit(SimpleNode node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.jjtGetChild(0).jjtAccept(this, data);
    --indent;
    return data;
  }

  public Object visit(ASTRoot node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.jjtGetChild(0).jjtAccept(this, data);
    --indent;
    return data;
  }

  public Object visit(ASTExpression node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.jjtGetChild(0).jjtAccept(this, data);
    --indent;
    return data;
  }

  public Object visit(ASTOr node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    for (int i = 0; i < node.jjtGetNumChildren(); i++) {
      data = node.jjtGetChild(i).jjtAccept(this, data);
    }
    --indent;
    return data;
  }

  public Object visit(ASTAnd node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    for (int i = 0; i < node.jjtGetNumChildren(); i++) {
      data = node.jjtGetChild(i).jjtAccept(this, data);
    }
    --indent;
    return data;
  }

  public Object visit(ASTNot node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.jjtGetChild(0).jjtAccept(this, data);
    --indent;
    return data;
  }

  public Object visit(ASTExecution node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.jjtGetChild(0).jjtAccept(this, data);
    --indent;
    return data;
  }

  public Object visit(ASTCall node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.jjtGetChild(0).jjtAccept(this, data);
    --indent;
    return data;
  }

  public Object visit(ASTSet node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.jjtGetChild(0).jjtAccept(this, data);
    --indent;
    return data;
  }

  public Object visit(ASTGet node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.jjtGetChild(0).jjtAccept(this, data);
    --indent;
    return data;
  }

  public Object visit(ASTHandler node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.jjtGetChild(0).jjtAccept(this, data);
    --indent;
    return data;
  }

  public Object visit(ASTWithin node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.jjtGetChild(0).jjtAccept(this, data);
    --indent;
    return data;
  }

  public Object visit(ASTWithinCode node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.jjtGetChild(0).jjtAccept(this, data);
    --indent;
    return data;
  }

  public Object visit(ASTStaticInitialization node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.jjtGetChild(0).jjtAccept(this, data);
    --indent;
    return data;
  }

  public Object visit(ASTIf node, Object data) {
    System.out.println(indentString() + "if()");
    return data;
  }

  public Object visit(ASTCflow node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.jjtGetChild(0).jjtAccept(this, data);
    --indent;
    return data;
  }

  public Object visit(ASTCflowBelow node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.jjtGetChild(0).jjtAccept(this, data);
    --indent;
    return data;
  }


  public Object visit(ASTHasMethod node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.jjtGetChild(0).jjtAccept(this, data);
    --indent;
    return data;
  }


  public Object visit(ASTHasField node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.jjtGetChild(0).jjtAccept(this, data);
    --indent;
    return data;
  }

  public Object visit(ASTTarget node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    System.out.println(node.getIdentifier());
    --indent;
    return data;
  }

  public Object visit(ASTThis node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    System.out.println(node.getIdentifier());
    --indent;
    return data;
  }

  public Object visit(ASTClassPattern node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    int nr = node.jjtGetNumChildren();
    for (int i = 0; i < nr; i++) {
      data = node.jjtGetChild(i).jjtAccept(this, data);
    }
    --indent;
    return data;
  }

  public Object visit(ASTMethodPattern node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    int nr = node.jjtGetNumChildren();
    for (int i = 0; i < nr; i++) {
      data = node.jjtGetChild(i).jjtAccept(this, data);
    }
    --indent;
    return data;
  }

  public Object visit(ASTConstructorPattern node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    int nr = node.jjtGetNumChildren();
    for (int i = 0; i < nr; i++) {
      data = node.jjtGetChild(i).jjtAccept(this, data);
    }
    --indent;
    return data;
  }

  public Object visit(ASTFieldPattern node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    int nr = node.jjtGetNumChildren();
    for (int i = 0; i < nr; i++) {
      data = node.jjtGetChild(i).jjtAccept(this, data);
    }
    --indent;
    return data;
  }

  public Object visit(ASTPointcutReference node, Object data) {
    System.out.println(indentString() + node);
    return data;
  }

  public Object visit(ASTParameter node, Object data) {
    System.out.println(indentString() + node);
    return data;
  }

  public Object visit(ASTArgs node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    if (node.jjtGetNumChildren() > 0) {
      data = node.jjtGetChild(0).jjtAccept(this, data);
    }
    --indent;
    return data;
  }

  public Object visit(ASTArgParameter node, Object data) {
    System.out.println(indentString() + node);
    return data;
  }

  public Object visit(ASTAttribute node, Object data) {
    System.out.println(indentString() + node);
    return data;
  }

  public Object visit(ASTModifier node, Object data) {
    System.out.println(indentString() + node);
    return data;
  }

  private String indentString() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < indent; ++i) {
      sb.append(" ");
    }
    return sb.toString();
  }
}