/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.idprovider.impl;

import com.tc.object.ObjectID;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.tx.ClientTransaction;
import com.tc.util.sequence.Sequence;

public class ObjectIDProviderImpl implements ObjectIDProvider {

  private final Sequence sequence;

  public ObjectIDProviderImpl(Sequence sequence) {
    this.sequence = sequence;
  }

  public ObjectID next(ClientTransaction txn, Object pojo) {
    return new ObjectID(this.sequence.next());
  }
}