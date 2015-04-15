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
package com.tc.l2.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.msg.TestTransactionBatch;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.objectserver.tx.TestCommitTransactionMessage;
import com.tc.objectserver.tx.TestCommitTransactionMessageFactory;
import com.tc.objectserver.tx.TestServerTransaction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

/*
 * This test really belongs in the TC Messaging module but it's dependencies
 * currently prevent that.  It needs some heavy refactoring.
 */
public class ServerTxnAckMessageTest extends TestCase {
  private RelayedCommitTransactionMessage relayedCommitTransactionMessage;
  private Set<ServerTransactionID>        serverTransactionIDs;
  private static final int                channelId = 2;
  private final NodeID                    nodeID    = new ServerID("foo", "foobar".getBytes());

  @Override
  public void setUp() {
    TestCommitTransactionMessage testCommitTransactionMessage = (TestCommitTransactionMessage) new TestCommitTransactionMessageFactory()
        .newCommitTransactionMessage(GroupID.NULL_ID);
    testCommitTransactionMessage.setBatch(new TestTransactionBatch(new TCByteBuffer[] { TCByteBufferFactory
                                              .getInstance(false, 3452) }), new ObjectStringSerializerImpl());
    testCommitTransactionMessage.setChannelID(new ClientID(channelId));

    this.serverTransactionIDs = new HashSet();
    ClientID cid = new ClientID(channelId);
    ServerTransactionID stid1 = new ServerTransactionID(cid, new TransactionID(4234));
    ServerTransactionID stid2 = new ServerTransactionID(cid, new TransactionID(6543));
    ServerTransactionID stid3 = new ServerTransactionID(cid, new TransactionID(1654));
    ServerTransactionID stid4 = new ServerTransactionID(cid, new TransactionID(3460));
    this.serverTransactionIDs.add(stid1);
    this.serverTransactionIDs.add(stid2);
    this.serverTransactionIDs.add(stid3);
    this.serverTransactionIDs.add(stid4);

    List transactions = new ArrayList();
    transactions.add(new TestServerTransaction(stid1, new TxnBatchID(32), new GlobalTransactionID(23)));
    transactions.add(new TestServerTransaction(stid2, new TxnBatchID(12), new GlobalTransactionID(54)));
    transactions.add(new TestServerTransaction(stid3, new TxnBatchID(43), new GlobalTransactionID(55)));
    transactions.add(new TestServerTransaction(stid4, new TxnBatchID(9), new GlobalTransactionID(78)));

    this.relayedCommitTransactionMessage = RelayedCommitTransactionMessageFactory
        .createRelayedCommitTransactionMessage(cid,
                                               testCommitTransactionMessage.getBatchData(), transactions, 700,
                                               new GlobalTransactionID(99),
                                               testCommitTransactionMessage.getSerializer());
    this.relayedCommitTransactionMessage.setMessageOrginator(this.nodeID);
  }

  @Override
  public void tearDown() {
    this.relayedCommitTransactionMessage = null;
    this.serverTransactionIDs = null;
  }

  private void validate(ServerRelayedTxnAckMessage stam, ServerRelayedTxnAckMessage stam1) {
    assertEquals(stam.getType(), stam1.getType());
    assertEquals(stam.getMessageID(), stam1.getMessageID());
    assertEquals(stam.inResponseTo(), stam1.inResponseTo());
    assertEquals(stam.messageFrom(), stam1.messageFrom());

    Set acked = stam.getAckedServerTxnIDs();
    Set acked1 = stam1.getAckedServerTxnIDs();
    assertEquals(acked.size(), acked1.size());
    for (Iterator iter = acked.iterator(); iter.hasNext();) {
      ServerTransactionID stid = (ServerTransactionID) iter.next();
      assertTrue(acked1.contains(stid));
      acked1.remove(stid);
    }
    assertTrue(acked1.isEmpty());

    assertEquals(stam.getDestinationID(), this.nodeID);
  }

  private ServerRelayedTxnAckMessage writeAndRead(ServerRelayedTxnAckMessage stam) throws Exception {
    TCByteBufferOutputStream bo = new TCByteBufferOutputStream();
    stam.serializeTo(bo);
    System.err.println("Written : " + stam);
    TCByteBufferInputStream bi = new TCByteBufferInputStream(bo.toArray());
    ServerRelayedTxnAckMessage stam1 = new ServerRelayedTxnAckMessage();
    stam1.deserializeFrom(bi);
    System.err.println("Read : " + stam1);
    return stam1;
  }

  public void testBasicSerialization() throws Exception {
    ServerRelayedTxnAckMessage stam = new ServerRelayedTxnAckMessage(this.relayedCommitTransactionMessage, this.serverTransactionIDs);
    ServerRelayedTxnAckMessage stam1 = writeAndRead(stam);
    validate(stam, stam1);
  }
}
