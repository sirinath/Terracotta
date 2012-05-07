/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlBoolean;
import org.apache.xmlbeans.XmlException;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.terracottatech.config.Security;
import com.terracottatech.config.Servers;

public class SecurityConfigObject extends BaseConfigObject implements SecurityConfig {

  public SecurityConfigObject(ConfigContext context) {
    super(context);
    context.ensureRepositoryProvides(Security.class);
  }

  public boolean isEnabled() {
    return ((Security) this.context.bean()).getEnabled();
  }

  public String getKeyStorePath() {
    return ((Security) this.context.bean()).getKeystore();
  }

  public String getTrustStorePath() {
    return ((Security) this.context.bean()).getTruststore();
  }

  public static void initializeSecurity(Servers servers, DefaultValueProvider defaultValueProvider) throws ConfigurationSetupException {
    try {
      if (!servers.isSetSecurity()) {
        servers.setSecurity(getDefaultSecurity(servers, defaultValueProvider));
      } else {
        Security security = servers.getSecurity();
        if (!security.isSetEnabled()) {
          security.setEnabled(getDefaultEnabled(servers, defaultValueProvider));
        }
      }

    } catch (XmlException e) {
      throw new ConfigurationSetupException(e);
    }
  }

  private static Security getDefaultSecurity(Servers servers, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    final boolean defaultEnabled = getDefaultEnabled(servers, defaultValueProvider);
    Security security = Security.Factory.newInstance();
    security.setEnabled(defaultEnabled);
    return security;
  }

  private static boolean getDefaultEnabled(Servers servers, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(servers.schemaType(), "security/enabled"))
        .getBooleanValue();
  }

}
