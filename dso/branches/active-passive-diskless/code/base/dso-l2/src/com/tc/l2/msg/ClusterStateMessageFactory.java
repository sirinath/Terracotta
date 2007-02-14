/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.l2.state.Enrollment;
import com.tc.net.groups.GroupMessage;

public class ClusterStateMessageFactory {

  public static ClusterStateMessage createElectionStartedMessage(Enrollment e) {
    return new ClusterStateMessage(ClusterStateMessage.START_ELECTION, e);
  }

  public static GroupMessage createElectionWonMessage(Enrollment e) {
    return new ClusterStateMessage(ClusterStateMessage.ELECTION_WON, e);
  }

  public static GroupMessage createAbortElectionMessage(GroupMessage initiatingMsg, Enrollment e) {
    return new ClusterStateMessage(initiatingMsg.getMessageID(), ClusterStateMessage.ABORT_ELECTION, e);
  }

  public static GroupMessage createElectionStartedMessage(ClusterStateMessage initiatingMsg, Enrollment e) {
    return new ClusterStateMessage(initiatingMsg.getMessageID(), ClusterStateMessage.START_ELECTION, e);
  }

  public static GroupMessage createResultConflictMessage(ClusterStateMessage initiatingMsg, Enrollment e) {
    return new ClusterStateMessage(initiatingMsg.getMessageID(), ClusterStateMessage.RESULT_CONFLICT, e);
  }
  
  public static GroupMessage createResultAgreedMessage(ClusterStateMessage initiatingMsg, Enrollment e) {
    return new ClusterStateMessage(initiatingMsg.getMessageID(), ClusterStateMessage.RESULT_AGREED, e);
  }

}
