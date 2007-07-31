/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.cflow;


import com.tc.aspectwerkz.expression.ExpressionInfo;
import com.tc.aspectwerkz.expression.ExpressionNamespace;
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

import java.util.List;

/**
 * A visitor to create the bindings between cflow aspect and cflow subexpression.
 * For each visited cflow / cflowbelow node, one CflowBinding is created
 * with the cflow(below) subexpression as expressionInfo.
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class CflowAspectExpressionVisitor implements ExpressionParserVisitor {

  private ExpressionInfo m_expressionInfo;
  private Node m_root;
  private String m_namespace;

  public CflowAspectExpressionVisitor(ExpressionInfo expressionInfo, Node root, String namespace) {
    m_expressionInfo = expressionInfo;
    m_root = root;
    m_namespace = namespace;
  }

  /**
   * Visit the expression and populate the list with CflowBinding for each cflow() or cflowbelow()
   * subexpression encountered (including thru pointcut references)
   *
   * @param bindings
   * @return the list of bindings
   */
  public List populateCflowAspectBindings(List bindings) {
    visit(m_root, bindings);
    return bindings;
  }

  public Object visit(Node node, Object data) {
    return node.jjtGetChild(0).jjtAccept(this, data);
  }

  public Object visit(SimpleNode node, Object data) {
    return node.jjtGetChild(0).jjtAccept(this, data);
  }

  public Object visit(ASTRoot node, Object data) {
    return node.jjtGetChild(0).jjtAccept(this, data);
  }

  public Object visit(ASTExpression node, Object data) {
    return node.jjtGetChild(0).jjtAccept(this, data);
  }

  public Object visit(ASTAnd node, Object data) {
    // the AND and OR can have more than 2 nodes [see jjt grammar]
    for (int i = 0; i < node.jjtGetNumChildren(); i++) {
      node.jjtGetChild(i).jjtAccept(this, data);
    }
    return data;
  }

  public Object visit(ASTOr node, Object data) {
    // the AND and OR can have more than 2 nodes [see jjt grammar]
    for (int i = 0; i < node.jjtGetNumChildren(); i++) {
      node.jjtGetChild(i).jjtAccept(this, data);
    }
    return data;
  }

  public Object visit(ASTNot node, Object data) {
    return node.jjtGetChild(0).jjtAccept(this, data);
  }

  /**
   * Resolve pointcut references
   *
   * @param node
   * @param data
   * @return
   */
  public Object visit(ASTPointcutReference node, Object data) {
    ExpressionNamespace namespace = ExpressionNamespace.getNamespace(m_namespace);
    CflowAspectExpressionVisitor expression = namespace.getExpressionInfo(node.getName()).getCflowAspectExpression();
    return expression.populateCflowAspectBindings((List) data);
  }

  public Object visit(ASTExecution node, Object data) {
    return data;
  }

  public Object visit(ASTCall node, Object data) {
    return data;
  }

  public Object visit(ASTSet node, Object data) {
    return data;
  }

  public Object visit(ASTGet node, Object data) {
    return data;
  }

  public Object visit(ASTHandler node, Object data) {
    return data;
  }

  public Object visit(ASTWithin node, Object data) {
    return data;
  }

  public Object visit(ASTWithinCode node, Object data) {
    return data;
  }

  public Object visit(ASTStaticInitialization node, Object data) {
    return data;
  }

  public Object visit(ASTIf node, Object data) {
    return data;
  }

  /**
   * build a cflow binding with the cflow sub expression
   *
   * @param node
   * @param data
   * @return
   */
  public Object visit(ASTCflow node, Object data) {
    int cflowID = node.hashCode();
    Node subNode = node.jjtGetChild(0);
    ExpressionInfo subExpression = new ExpressionInfo(subNode, m_namespace);
    subExpression.inheritPossibleArgumentFrom(m_expressionInfo);
    ((List) data).add(new CflowBinding(cflowID, subExpression, m_expressionInfo, false));
    return data;
  }

  /**
   * build a cflowbelow binding with the cflowbelow sub expression
   *
   * @param node
   * @param data
   * @return
   */
  public Object visit(ASTCflowBelow node, Object data) {
    int cflowID = node.hashCode();
    Node subNode = node.jjtGetChild(0);
    ExpressionInfo subExpression = new ExpressionInfo(subNode, m_namespace);
    subExpression.inheritPossibleArgumentFrom(m_expressionInfo);
    ((List) data).add(new CflowBinding(cflowID, subExpression, m_expressionInfo, true));
    return data;
  }

  public Object visit(ASTArgs node, Object data) {
    return data;
  }

  public Object visit(ASTHasMethod node, Object data) {
    return data;
  }

  public Object visit(ASTHasField node, Object data) {
    return data;
  }

  public Object visit(ASTTarget node, Object data) {
    return data;
  }

  public Object visit(ASTThis node, Object data) {
    return data;
  }

  public Object visit(ASTClassPattern node, Object data) {
    throw new UnsupportedOperationException("Should not be reached");
  }

  public Object visit(ASTMethodPattern node, Object data) {
    throw new UnsupportedOperationException("Should not be reached");
  }

  public Object visit(ASTConstructorPattern node, Object data) {
    throw new UnsupportedOperationException("Should not be reached");
  }

  public Object visit(ASTFieldPattern node, Object data) {
    throw new UnsupportedOperationException("Should not be reached");
  }

  public Object visit(ASTParameter node, Object data) {
    throw new UnsupportedOperationException("Should not be reached");
  }

  public Object visit(ASTArgParameter node, Object data) {
    throw new UnsupportedOperationException("Should not be reached");
  }

  public Object visit(ASTAttribute node, Object data) {
    throw new UnsupportedOperationException("Should not be reached");
  }

  public Object visit(ASTModifier node, Object data) {
    throw new UnsupportedOperationException("Should not be reached");
  }
}
