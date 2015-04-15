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
package com.tc.config.schema.defaults;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

/**
 * A mock {@link DefaultValueProvider}, for use in tests.
 */
public class MockDefaultValueProvider implements DefaultValueProvider {

  private int          numDefaultFors;
  private XmlObject    returnedDefaultFor;
  private SchemaType   lastBaseType;
  private String       lastXPath;
  private XmlException defaultForException;
  private int          numHasDefaults;
  private SchemaType   lastHasDefaultsSchemaType;
  private String       lastHasDefaultsXPath;
  private boolean      returnedHasDefault;
  private int          numIsOptionals;
  private boolean      returnedIsOptional;
  private int          numPossibleForXPathToHaveDefaults;
  private String       lastPossibleForXPathToHaveDefaultsXPath;
  private boolean      returnedPossibleForXPathToHaveDefault;

  public MockDefaultValueProvider() {
    this.returnedDefaultFor = null;
    this.defaultForException = null;
    this.returnedHasDefault = false;
    this.returnedIsOptional = false;
    this.returnedPossibleForXPathToHaveDefault = false;

    reset();
  }

  public void reset() {
    this.numDefaultFors = 0;
    this.lastBaseType = null;
    this.lastXPath = null;
    this.numHasDefaults = 0;
    this.lastHasDefaultsSchemaType = null;
    this.lastHasDefaultsXPath = null;
    this.numIsOptionals = 0;
    this.numPossibleForXPathToHaveDefaults = 0;
    this.lastPossibleForXPathToHaveDefaultsXPath = null;
  }

  @Override
  public XmlObject defaultFor(SchemaType baseType, String xpath) throws XmlException {
    ++this.numDefaultFors;
    this.lastBaseType = baseType;
    this.lastXPath = xpath;

    if (this.defaultForException != null) throw this.defaultForException;

    return this.returnedDefaultFor;
  }

  @Override
  public boolean hasDefault(SchemaType baseType, String xpath) {
    ++this.numHasDefaults;
    this.lastHasDefaultsSchemaType = baseType;
    this.lastHasDefaultsXPath = xpath;
    return this.returnedHasDefault;
  }

  @Override
  public boolean isOptional(SchemaType baseType, String xpath) {
    ++this.numIsOptionals;
    this.lastBaseType = baseType;
    this.lastXPath = xpath;
    return this.returnedIsOptional;
  }

  @Override
  public boolean possibleForXPathToHaveDefault(String xpath) {
    ++this.numPossibleForXPathToHaveDefaults;
    this.lastPossibleForXPathToHaveDefaultsXPath = xpath;
    return this.returnedPossibleForXPathToHaveDefault;
  }

  public SchemaType getLastBaseType() {
    return lastBaseType;
  }

  public String getLastXPath() {
    return lastXPath;
  }

  public int getNumDefaultFors() {
    return numDefaultFors;
  }

  public void setReturnedDefaultFor(XmlObject returnedDefaultFor) {
    this.returnedDefaultFor = returnedDefaultFor;
  }

  public void setDefaultForException(XmlException defaultForException) {
    this.defaultForException = defaultForException;
  }

  public int getNumHasDefaults() {
    return numHasDefaults;
  }

  public int getNumIsOptionals() {
    return numIsOptionals;
  }

  public int getNumPossibleForXPathToHaveDefaults() {
    return numPossibleForXPathToHaveDefaults;
  }

  public void setReturnedHasDefault(boolean returnedHasDefault) {
    this.returnedHasDefault = returnedHasDefault;
  }

  public void setReturnedIsOptional(boolean returnedIsOptional) {
    this.returnedIsOptional = returnedIsOptional;
  }

  public void setReturnedPossibleForXPathToHaveDefault(boolean returnedPossibleForXPathToHaveDefault) {
    this.returnedPossibleForXPathToHaveDefault = returnedPossibleForXPathToHaveDefault;
  }

  public SchemaType getLastHasDefaultsSchemaType() {
    return lastHasDefaultsSchemaType;
  }

  public String getLastHasDefaultsXPath() {
    return lastHasDefaultsXPath;
  }

  public String getLastPossibleForXPathToHaveDefaultsXPath() {
    return lastPossibleForXPathToHaveDefaultsXPath;
  }

}
