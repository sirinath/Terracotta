package com.tc.objectserver.storage.nativeCache.api;

import org.terracotta.nativecache.storage.portability.Portability;

import java.nio.ByteBuffer;

public class LongPortability implements Portability<Long> {

  public Long decode(ByteBuffer buffer) {
    return buffer.asLongBuffer().get();
  }

  public ByteBuffer encode(Long l) {
    ByteBuffer buffer = ByteBuffer.allocate(8);
    buffer.asLongBuffer().put(l);
    return buffer;
  }

  public boolean equals(Object o1, ByteBuffer o2) {
    Long l2 = decode(o2);

    if (o1 instanceof Long) {
      Long l1 = (Long) o1;
      if (l1.longValue() == l2.longValue()) { return true; }
    }
    return false;
  }

}
