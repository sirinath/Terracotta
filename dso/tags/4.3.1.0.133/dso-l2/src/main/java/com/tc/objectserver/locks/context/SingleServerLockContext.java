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
package com.tc.objectserver.locks.context;

import com.tc.net.ClientID;
import com.tc.object.locks.ServerLockContext;
import com.tc.object.locks.ThreadID;
import com.tc.util.Assert;

/**
 * This class is present to ensure that we dont waste memory on saving "next" pointer. This will be useful only when a
 * single context is present in the context queue of the lock.
 */
public class SingleServerLockContext extends ServerLockContext {

  public SingleServerLockContext(ClientID clientID, ThreadID threadID) {
    super(clientID, threadID);
  }

  @Override
  public ServerLockContext getNext() {
    return null;
  }

  @Override
  public ServerLockContext setNext(ServerLockContext next) {
    Assert.assertNull(next);
    return null;
  }
}
