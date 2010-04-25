/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.TCClassNotFoundException;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ServerTCMap;

public class TCObjectServerMapImpl extends TCObjectLogical implements TCObject, TCObjectServerMap {

  private final ClientObjectManager    objectManager;
  private final RemoteServerMapManager serverMapManager;

  public TCObjectServerMapImpl(final ClientObjectManager objectManager, final RemoteServerMapManager serverMapManager,
                           final ObjectID id, final Object peer, final TCClass tcc, final boolean isNew) {
    super(id, peer, tcc, isNew);
    this.objectManager = objectManager;
    this.serverMapManager = serverMapManager;
  }

  /**
   * Returns the value for a particular Key in a ServerTCMap.
   * 
   * @param pojo Object
   * @param key Key Object : Note currently only literal keys or shared keys are supported. Even if the key is portable,
   *        but not shared, it is not supported.
   * @return value Object in the mapping, null if no mapping present.
   */
  public Object getValueForKeyInMap(final ServerTCMap map, final Object key) {

    final TCObject tcObject = map.__tc_managed();
    if (tcObject == null) { throw new UnsupportedOperationException(
                                                                    "getValueForKeyInMap is not supported in a non-shared ServerTCMap"); }
    final ObjectID mapID = tcObject.getObjectID();
    Object portableKey = key;
    if (key instanceof Manageable) {
      final TCObject keyObject = ((Manageable) key).__tc_managed();
      if (keyObject == null) { throw new UnsupportedOperationException(
                                                                       "Key is portable, but not shared. This is currently not supported with ServerTCMap. Map ID = "
                                                                           + mapID + " key = " + key); }
      portableKey = keyObject.getObjectID();
    }

    if (!LiteralValues.isLiteralInstance(portableKey)) {
      // formatter
      throw new UnsupportedOperationException(
                                              "Key is not portable. It needs to be a liternal or portable and shared for ServerTCMap. Key = "
                                                  + portableKey + " map id = " + mapID);
    }

    final Object value = this.serverMapManager.getMappingForKey(mapID, portableKey);

    if (value instanceof ObjectID) {
      try {
        return this.objectManager.lookupObject((ObjectID) value);
      } catch (final ClassNotFoundException e) {
        throw new TCClassNotFoundException(e);
      }
    } else {
      return value;
    }
  }

  public int getSize(final ServerTCMap map) {
    final TCObject tcObject = map.__tc_managed();
    if (tcObject == null) { throw new UnsupportedOperationException(
                                                                    "getSize is not supported in a non-shared ServerTCMap"); }
    final ObjectID mapID = tcObject.getObjectID();

    return this.serverMapManager.getSize(mapID);
  }

}
