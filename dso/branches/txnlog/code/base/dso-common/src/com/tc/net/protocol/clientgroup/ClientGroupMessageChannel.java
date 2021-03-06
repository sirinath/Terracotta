/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.clientgroup;

import com.tc.net.GroupID;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageImpl;
import com.tc.net.protocol.tcm.TCMessageType;

public interface ClientGroupMessageChannel extends ClientMessageChannel {

  public ClientMessageChannel getChannel(GroupID groupID);

  public GroupID[] getGroupIDs();

  public TCMessage createMessage(GroupID groupID, TCMessageType type);

  public void broadcast(final TCMessageImpl message);

}
