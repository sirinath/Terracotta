/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.SchemaProperty;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlAnySimpleType;
import org.apache.xmlbeans.XmlObject;

import com.terracottatech.config.Server;

import java.lang.reflect.Method;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;

final class XmlConfigPersistenceManager {

  private static final Class[]  NO_PARAMS        = new Class[0];
  private static final Object[] NO_ARGS          = new Object[0];
  private static final String   SET_STRING_VALUE = "setStringValue";
  private static final String   XSET             = "xset";
  private static final String   XGET             = "xget";
  private static final String   TYPE             = "type";

  String readElement(XmlObject parent, String elementName) {
    try {
      Class parentType = parent.schemaType().getJavaClass();
      XmlAnySimpleType element = ((XmlAnySimpleType) getXmlObject(parent, parentType, convertElementName(elementName)));
      if (element != null) return element.getStringValue();
      return getSchemaProperty(parentType, elementName).getDefaultText();
    } catch (Exception e) {
      e.printStackTrace();
      return "";
    }
  }

  void writeElement(XmlObject parent, String elementName, String value) {
    try {
      Class parentType = parent.schemaType().getJavaClass();
      XmlObject xmlObject = ensureElementHierarchy(parent, parentType, elementName, convertElementName(elementName));
      Class[] params = new Class[] { String.class };
      Object[] args = new Object[] { value };
      String methodName = SET_STRING_VALUE;
      Class objClass = xmlObject.getClass();
      Method method = objClass.getMethod(methodName, params);
      method.invoke(xmlObject, args);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private XmlObject ensureElementHierarchy(XmlObject parent, Class parentType, String elementName, String fieldName)
      throws Exception {
    XmlObject xmlObject = getXmlObject(parent, parentType, fieldName);
    if (xmlObject != null) return xmlObject;
    Class[] params = new Class[] { getPropertySchemaType(parentType, elementName).getJavaClass() };
    Object[] args = new Object[] { getSchemaProperty(parentType, elementName).getDefaultValue() };
    String methodName = XSET + fieldName;
    Method method = parentType.getMethod(methodName, params);
    method.invoke(parent, args);
    return getXmlObject(parent, parentType, fieldName);
  }

  private String convertElementName(String s) {
    StringBuffer sb = new StringBuffer();
    StringTokenizer st = new StringTokenizer(s, "-");
    String tok;
    while (st.hasMoreTokens()) {
      tok = st.nextToken();
      sb.append(Character.toUpperCase(tok.charAt(0)));
      sb.append(tok.substring(1));
    }
    return sb.toString();
  }

  private XmlObject getXmlObject(XmlObject parent, Class parentType, String fieldName) throws Exception {
    return (XmlObject) invokePrefixedParentNoParams(XGET, parent, parentType, fieldName);
  }

  private Object invokePrefixedParentNoParams(String prefix, XmlObject parent, Class parentType, String fieldName)
      throws Exception {
    Method method = parentType.getMethod(prefix + fieldName, NO_PARAMS);
    return (method != null) ? method.invoke(parent, NO_ARGS) : null;
  }

  private SchemaType getParentSchemaType(Class parentType) throws Exception {
    return (SchemaType) parentType.getField(TYPE).get(null);
  }

  private SchemaProperty getSchemaProperty(Class parentType, String elementName) throws Exception {
    QName qname = QName.valueOf(elementName);
    SchemaType type = getParentSchemaType(parentType);
    SchemaProperty property = type.getElementProperty(qname);
    if (property == null) property = type.getAttributeProperty(qname);
    return property;
  }

  private SchemaType getPropertySchemaType(Class parentType, String elementName) throws Exception {
    return getSchemaProperty(parentType, elementName).getType();
  }
}
