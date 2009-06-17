/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.object.ObjectID;

public interface RequestValueMappingForKeyMessage {

  void initialize(ObjectID mapID, Object portableKey);

  void send();

}
