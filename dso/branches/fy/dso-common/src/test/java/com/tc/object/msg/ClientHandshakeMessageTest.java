/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.session.SessionID;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;

import junit.framework.TestCase;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class ClientHandshakeMessageTest extends TestCase {

  public void testMessage() throws Exception {

    ClientHandshakeMessageImpl msg = new ClientHandshakeMessageImpl(new SessionID(0), new NullMessageMonitor(),
                                                                    new TCByteBufferOutputStream(4, 4096, false), null,
                                                                    TCMessageType.CLIENT_HANDSHAKE_MESSAGE);

    ObjectIDSet oids = new BitSetObjectIDSet();
    oids.add(new ObjectID(12345));
    msg.setObjectIDs(oids);

    ObjectIDSet validations = new BitSetObjectIDSet();
    validations.add(new ObjectID(1));
    validations.add(new ObjectID(2));
    validations.add(new ObjectID(100));
    validations.add(new ObjectID(200));
    msg.setObjectIDsToValidate(validations);

    msg.dehydrate();

    ClientHandshakeMessageImpl msg2 = new ClientHandshakeMessageImpl(SessionID.NULL_ID, new NullMessageMonitor(), null,
                                                                     (TCMessageHeader) msg.getHeader(), msg
                                                                         .getPayload());
    msg2.hydrate();
    System.out.println(msg2.getObjectIDs());
    System.out.println(msg2.getObjectIDsToValidate());

    assertThat(msg.getObjectIDs().size(), is(1));
    assertThat(msg.getObjectIDs(), hasItem(new ObjectID(12345)));

    assertThat(msg.getObjectIDsToValidate(), hasItems(new ObjectID(1), new ObjectID(2), new ObjectID(100), new ObjectID(200)));
    assertThat(msg.getObjectIDsToValidate().size(), is(4));
  }
}
