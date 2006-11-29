/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.QNameSet;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlDocumentProperties;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.xml.stream.XMLInputStream;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

import com.tc.exception.ImplementMe;
import com.terracottatech.configV2.Server;
import com.terracottatech.configV2.Servers;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

/**
 * A mock {@link Servers}, for use in tests.
 */
public class MockServers implements Servers {

  public MockServers() {
    super();
  }

  public Server[] getServerArray() {
    throw new ImplementMe();
  }

  public Server getServerArray(int arg0) {
    throw new ImplementMe();
  }

  public int sizeOfServerArray() {
    return 1;
  }

  public void setServerArray(Server[] arg0) {
    throw new ImplementMe();
  }

  public void setServerArray(int arg0, Server arg1) {
    throw new ImplementMe();
  }

  public Server insertNewServer(int arg0) {
    throw new ImplementMe();
  }

  public Server addNewServer() {
    throw new ImplementMe();
  }

  public void removeServer(int arg0) {
    throw new ImplementMe();
  }

  public SchemaType schemaType() {
    throw new ImplementMe();
  }

  public boolean validate() {
    throw new ImplementMe();
  }

  public boolean validate(XmlOptions arg0) {
    throw new ImplementMe();
  }

  public XmlObject[] selectPath(String arg0) {
    throw new ImplementMe();
  }

  public XmlObject[] selectPath(String arg0, XmlOptions arg1) {
    throw new ImplementMe();
  }

  public XmlObject[] execQuery(String arg0) {
    throw new ImplementMe();
  }

  public XmlObject[] execQuery(String arg0, XmlOptions arg1) {
    throw new ImplementMe();
  }

  public XmlObject changeType(SchemaType arg0) {
    throw new ImplementMe();
  }

  public XmlObject substitute(QName arg0, SchemaType arg1) {
    throw new ImplementMe();
  }

  public boolean isNil() {
    throw new ImplementMe();
  }

  public void setNil() {
    throw new ImplementMe();
  }

  public boolean isImmutable() {
    throw new ImplementMe();
  }

  public XmlObject set(XmlObject arg0) {
    throw new ImplementMe();
  }

  public XmlObject copy() {
    throw new ImplementMe();
  }

  public boolean valueEquals(XmlObject arg0) {
    throw new ImplementMe();
  }

  public int valueHashCode() {
    throw new ImplementMe();
  }

  public int compareTo(Object arg0) {
    throw new ImplementMe();
  }

  public int compareValue(XmlObject arg0) {
    throw new ImplementMe();
  }

  public XmlObject[] selectChildren(QName arg0) {
    throw new ImplementMe();
  }

  public XmlObject[] selectChildren(String arg0, String arg1) {
    throw new ImplementMe();
  }

  public XmlObject[] selectChildren(QNameSet arg0) {
    throw new ImplementMe();
  }

  public XmlObject selectAttribute(QName arg0) {
    throw new ImplementMe();
  }

  public XmlObject selectAttribute(String arg0, String arg1) {
    throw new ImplementMe();
  }

  public XmlObject[] selectAttributes(QNameSet arg0) {
    throw new ImplementMe();
  }

  public Object monitor() {
    throw new ImplementMe();
  }

  public XmlDocumentProperties documentProperties() {
    throw new ImplementMe();
  }

  public XmlCursor newCursor() {
    throw new ImplementMe();
  }

  public XMLInputStream newXMLInputStream() {
    throw new ImplementMe();
  }

  public XMLStreamReader newXMLStreamReader() {
    throw new ImplementMe();
  }

  public String xmlText() {
    throw new ImplementMe();
  }

  public InputStream newInputStream() {
    throw new ImplementMe();
  }

  public Reader newReader() {
    throw new ImplementMe();
  }

  public Node newDomNode() {
    throw new ImplementMe();
  }

  public Node getDomNode() {
    throw new ImplementMe();
  }

  public void save(ContentHandler arg0, LexicalHandler arg1) {
    throw new ImplementMe();
  }

  public void save(File arg0) {
    throw new ImplementMe();
  }

  public void save(OutputStream arg0) {
    throw new ImplementMe();
  }

  public void save(Writer arg0) {
    throw new ImplementMe();
  }

  public XMLInputStream newXMLInputStream(XmlOptions arg0) {
    throw new ImplementMe();
  }

  public XMLStreamReader newXMLStreamReader(XmlOptions arg0) {
    throw new ImplementMe();
  }

  public String xmlText(XmlOptions arg0) {
    throw new ImplementMe();
  }

  public InputStream newInputStream(XmlOptions arg0) {
    throw new ImplementMe();
  }

  public Reader newReader(XmlOptions arg0) {
    throw new ImplementMe();
  }

  public Node newDomNode(XmlOptions arg0) {
    throw new ImplementMe();
  }

  public void save(ContentHandler arg0, LexicalHandler arg1, XmlOptions arg2) {
    throw new ImplementMe();
  }

  public void save(File arg0, XmlOptions arg1) {
    throw new ImplementMe();
  }

  public void save(OutputStream arg0, XmlOptions arg1) {
    throw new ImplementMe();
  }

  public void save(Writer arg0, XmlOptions arg1) {
    throw new ImplementMe();
  }

  public void dump() {
    throw new ImplementMe();
  }
}
