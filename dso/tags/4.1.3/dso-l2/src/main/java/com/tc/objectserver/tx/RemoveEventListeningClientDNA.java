package com.tc.objectserver.tx;

import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAInternal;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAImpl;

/**
 * @author manish
 */
public class RemoveEventListeningClientDNA implements DNAInternal {
  private final ObjectID oid;
  private final ClientID clientID;

  public RemoveEventListeningClientDNA(final ObjectID oid, final ClientID clientID) {
    this.oid = oid;
    this.clientID = clientID;
  }

  @Override
  public int getArraySize() {
    return 0;
  }

  @Override
  public DNACursor getCursor() {
    return new RemoveEvenListeningClientDNACursor(clientID);
  }

  @Override
  public ObjectID getObjectID() throws DNAException {
    return oid;
  }

  @Override
  public ObjectID getParentObjectID() throws DNAException {
    return ObjectID.NULL_ID;
  }

  @Override
  public String getTypeName() {
    return null;
  }

  @Override
  public long getVersion() {
    return DNA.NULL_VERSION;
  }

  @Override
  public boolean hasLength() {
    return false;
  }

  @Override
  public boolean isDelta() {
    return true;
  }

  @Override
  public MetaDataReader getMetaDataReader() {
    return DNAImpl.NULL_META_DATA_READER;
  }

  @Override
  public boolean hasMetaData() {
    return true;
  }

  private static class RemoveEvenListeningClientDNACursor implements DNACursor {

    private final LogicalAction action;
    private boolean        completed;

    public RemoveEvenListeningClientDNACursor(final ClientID clientID) {
      this.action = new LogicalAction(SerializationUtil.REMOVE_EVENT_LISTENING_CLIENT,
                                      new Object[] { clientID.toLong() });
    }

    @Override
    public Object getAction() {
      return action;
    }

    @Override
    public int getActionCount() {
      return 1;
    }

    @Override
    public LogicalAction getLogicalAction() {
      return action;
    }

    @Override
    public PhysicalAction getPhysicalAction() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean next() {
      if (!completed) {
        completed = true;
        return true;
      } else {
        return false;
      }
    }

    @Override
    public boolean next(final DNAEncoding arg) {
      return next();
    }

    @Override
    public void reset() throws UnsupportedOperationException {
      throw new UnsupportedOperationException("Reset is not supported by this class");
    }
  }
}
