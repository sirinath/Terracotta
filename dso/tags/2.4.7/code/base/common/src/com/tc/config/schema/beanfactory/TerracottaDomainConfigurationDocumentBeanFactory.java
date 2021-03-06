/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.beanfactory;

import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.xml.sax.SAXException;

import com.tc.config.Loader;
import com.tc.util.Assert;
import com.terracottatech.config.TcConfigDocument;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

/**
 * A {@link ConfigBeanFactory} that creates {@link TerracottaDomainConfigurationDocument} beans.
 */
public class TerracottaDomainConfigurationDocumentBeanFactory implements ConfigBeanFactory {

  public TerracottaDomainConfigurationDocumentBeanFactory() {
    // Nothing here yet.
  }

  public BeanWithErrors createBean(InputStream in, String sourceDescription) throws IOException, SAXException,
      ParserConfigurationException, XmlException {
    Assert.assertNotBlank(sourceDescription);

    List errors = new ArrayList();
    XmlOptions options = createXmlOptions(errors, sourceDescription);
    Loader configLoader = new Loader();

    TcConfigDocument document = configLoader.parse(in, options);
    document.validate(options);
    return new BeanWithErrors(document, (XmlError[]) errors.toArray(new XmlError[errors.size()]));
  }

  public static XmlOptions createXmlOptions(List errors, String sourceDescription)
      throws SAXException, ParserConfigurationException {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(
                                                 TerracottaDomainConfigurationDocumentBeanFactory.class
                                                     .getClassLoader());
    try {
      XmlOptions options = new XmlOptions();
      options = options.setLoadLineNumbers();
      options = options.setDocumentSourceName(sourceDescription);
      options = options.setErrorListener(errors);
      return options;

    } finally {
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }

  }
}
