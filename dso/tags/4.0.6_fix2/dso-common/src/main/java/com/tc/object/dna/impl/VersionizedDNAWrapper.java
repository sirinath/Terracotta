/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAInternal;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.api.PhysicalAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VersionizedDNAWrapper<T extends DNA> implements DNAInternal {

  private final long    version;
  private final T       dna;
  private final boolean resetSupported;

  public VersionizedDNAWrapper(T dna, long version) {
    this(dna, version, false);
  }

  public VersionizedDNAWrapper(T dna, long version, boolean resetSupported) {
    this.dna = dna;
    this.version = version;
    this.resetSupported = resetSupported;
  }

  @Override
  public long getVersion() {
    return version;
  }

  @Override
  public boolean hasLength() {
    return dna.hasLength();
  }

  @Override
  public int getArraySize() {
    return dna.getArraySize();
  }

  @Override
  public String getTypeName() {
    return dna.getTypeName();
  }

  @Override
  public ObjectID getObjectID() throws DNAException {
    return dna.getObjectID();
  }

  @Override
  public ObjectID getParentObjectID() throws DNAException {
    return dna.getParentObjectID();
  }

  @Override
  public DNACursor getCursor() {
    return (resetSupported ? new ResetableDNACursor(dna.getCursor()) : dna.getCursor());
  }

  @Override
  public boolean isDelta() {
    return dna.isDelta();
  }

  @Override
  public String toString() {
    return dna.toString();
  }

  private static class ResetableDNACursor implements DNACursor {

    private final DNACursor cursor;
    private final List      actions = new ArrayList();
    private int             index   = -1;

    public ResetableDNACursor(DNACursor cursor) {
      this.cursor = cursor;
    }

    @Override
    public int getActionCount() {
      return cursor.getActionCount();
    }

    @Override
    public boolean next() throws IOException {
      if (++index < actions.size()) { return true; }
      boolean success = cursor.next();
      if (success) {
        actions.add(cursor.getAction());
      }
      return success;
    }

    @Override
    public boolean next(DNAEncoding encoding) throws IOException, ClassNotFoundException {
      if (++index < actions.size()) { return true; }
      boolean success = cursor.next(encoding);
      if (success) {
        actions.add(cursor.getAction());
      }
      return success;
    }

    @Override
    public void reset() throws UnsupportedOperationException {
      index = -1;
    }

    @Override
    public LogicalAction getLogicalAction() {
      return (index < actions.size() ? (LogicalAction) actions.get(index) : cursor.getLogicalAction());
    }

    @Override
    public PhysicalAction getPhysicalAction() {
      return (index < actions.size() ? (PhysicalAction) actions.get(index) : cursor.getPhysicalAction());
    }

    @Override
    public Object getAction() {
      return (index < actions.size() ? actions.get(index) : cursor.getAction());
    }

    @Override
    public String toString() {
      return cursor.toString();
    }

  }

  @Override
  public MetaDataReader getMetaDataReader() {
    return dna instanceof DNAInternal ? ((DNAInternal) dna).getMetaDataReader() : DNAImpl.NULL_META_DATA_READER;
  }

  @Override
  public boolean hasMetaData() {
    return dna instanceof DNAInternal ? ((DNAInternal) dna).hasMetaData() : false;
  }
}
