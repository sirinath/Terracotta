/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.BaseNewConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.BooleanConfigItem;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.StringArrayConfigItem;
import com.tc.config.schema.dynamic.XPathBasedConfigItem;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Root;
import com.terracottatech.config.Roots;

public class NewDSOApplicationConfigObject extends BaseNewConfigObject implements NewDSOApplicationConfig {
  private final ConfigItem            instrumentedClasses;
  private final StringArrayConfigItem transientFields;
  private final ConfigItem            locks;
  private final ConfigItem            roots;
  private final StringArrayConfigItem additionalBootJarClasses;
  private final BooleanConfigItem     supportSharingThroughReflection;
  private final StringArrayConfigItem webApplications;

  public NewDSOApplicationConfigObject(ConfigContext context) {
    super(context);

    this.context.ensureRepositoryProvides(DsoApplication.class);

    this.instrumentedClasses = new XPathBasedConfigItem(this.context, "instrumented-classes") {
      protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
        return ConfigTranslationHelper.translateIncludes(xmlObject);
      }
    };

    this.locks = new XPathBasedConfigItem(this.context, "locks") {
      protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
        return ConfigTranslationHelper.translateLocks(xmlObject);
      }
    };

    this.roots = new XPathBasedConfigItem(this.context, "roots") {
      protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
        return translateRoots(xmlObject);
      }
    };

    this.transientFields = this.context.stringArrayItem("transient-fields");
    this.additionalBootJarClasses = this.context.stringArrayItem("additional-boot-jar-classes");
    this.webApplications = this.context.stringArrayItem("web-applications");
    this.supportSharingThroughReflection = this.context.booleanItem("dso-reflection-enabled");
  }

  public StringArrayConfigItem webApplications() {
    return this.webApplications;
  }

  public ConfigItem instrumentedClasses() {
    return this.instrumentedClasses;
  }

  public StringArrayConfigItem transientFields() {
    return this.transientFields;
  }

  public ConfigItem locks() {
    return this.locks;
  }

  public ConfigItem roots() {
    return this.roots;
  }

  public StringArrayConfigItem additionalBootJarClasses() {
    return this.additionalBootJarClasses;
  }

  public BooleanConfigItem supportSharingThroughReflection() {
    return supportSharingThroughReflection;
  }

  private static Object translateRoots(XmlObject xmlObject) {
    if (xmlObject == null) return null;

    com.tc.object.config.schema.Root[] out;
    Root[] theRoots = ((Roots) xmlObject).getRootArray();
    out = new com.tc.object.config.schema.Root[theRoots == null ? 0 : theRoots.length];
    for (int i = 0; i < out.length; ++i) {
      out[i] = new com.tc.object.config.schema.Root(theRoots[i].getRootName(), theRoots[i].getFieldName());
    }
    return out;
  }
}
