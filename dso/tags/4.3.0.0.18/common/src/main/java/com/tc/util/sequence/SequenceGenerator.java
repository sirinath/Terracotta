/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.sequence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SequenceGenerator {

  public static class SequenceGeneratorException extends Exception {

    public SequenceGeneratorException(Exception e) {
      super(e);
    }

  }

  public interface SequenceGeneratorListener {

    public void sequenceCreatedFor(Object key) throws SequenceGeneratorException;

    public void sequenceDestroyedFor(Object key);

  }

  private final Map                       map = new ConcurrentHashMap<Object, Sequence>();
  private final SequenceGeneratorListener listener;

  public SequenceGenerator() {
    this(null);
  }

  public SequenceGenerator(SequenceGeneratorListener listener) {
    this.listener = listener;
  }

  public long getNextSequence(Object key) throws SequenceGeneratorException {
    Sequence seq = (Sequence) map.get(key);
    if (seq != null) return seq.next();
    synchronized (map) {
      if (!map.containsKey(key)) {
        if (listener != null) listener.sequenceCreatedFor(key);
        map.put(key, (seq = new SimpleSequence()));
      } else {
        seq = (Sequence) map.get(key);
      }
    }
    return seq.next();
  }

  public void clearSequenceFor(Object key) {
    if (map.remove(key) != null && listener != null) {
      listener.sequenceDestroyedFor(key);
    }
  }

}
