/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object.serialization;

import com.tc.logging.TCLogger;
import com.tc.object.ClientObjectManager;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.applicator.BaseApplicator;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;

import java.io.IOException;
import java.io.Serializable;

public class SerializedMapValueApplicator extends BaseApplicator {

  protected static final String CREATE_TIME_FIELD_NAME      = "createTime";
  protected static final String LAST_ACCESS_TIME_FIELD_NAME = "lastAccessedTime";

  public SerializedMapValueApplicator(final DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }

  public void dehydrate(final ClientObjectManager objectManager, final TCObject tco, final DNAWriter writer,
                        final Object pojo) {

    SerializedMapValue<Serializable> se = (SerializedMapValue<Serializable>) pojo;
    writer.addEntireArray(se.internalGetValue());
    writer.addPhysicalAction(CREATE_TIME_FIELD_NAME, se.internalGetCreateTime());
    writer.addPhysicalAction(LAST_ACCESS_TIME_FIELD_NAME, se.internalGetLastAccessedTime());
  }

  public Object getNewInstance(final ClientObjectManager objectManager, final DNA dna) {
    throw new UnsupportedOperationException();
  }

  public TraversedReferences getPortableObjects(final Object pojo, final TraversedReferences addTo) {
    return addTo;
  }

  public void hydrate(final ClientObjectManager objectManager, final TCObject tco, final DNA dna, final Object pojo)
      throws IOException, ClassNotFoundException {
    SerializedMapValue<Serializable> se = (SerializedMapValue<Serializable>) pojo;

    DNACursor cursor = dna.getCursor();
    while (cursor.next(encoding)) {
      PhysicalAction a = cursor.getPhysicalAction();
      if (a.isEntireArray()) {
        se.internalSetValue((byte[]) a.getObject());
      } else {
        // tco.setValue(a.getFieldName(), a.getObject());
        if (CREATE_TIME_FIELD_NAME.equals(a.getFieldName())) {
          se.internalSetCreateTime((Integer) a.getObject());
        } else if (LAST_ACCESS_TIME_FIELD_NAME.equals(a.getFieldName())) {
          se.internalSetLastAccessedTime((Integer) a.getObject());
        } else {
          throw new AssertionError("Unknown physical action: " + a);
        }
      }
    }
  }
}
