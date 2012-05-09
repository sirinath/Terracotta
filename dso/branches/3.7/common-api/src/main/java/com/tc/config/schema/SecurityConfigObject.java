/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.terracottatech.config.Security;

public class SecurityConfigObject extends BaseConfigObject implements SecurityConfig {

  public SecurityConfigObject(ConfigContext context) {
    super(context);
    context.ensureRepositoryProvides(Security.class);
  }

  public String getSslCertificateUri() {
    return ((Security) this.context.bean()).getSsl().getCertificate();
  }

}
