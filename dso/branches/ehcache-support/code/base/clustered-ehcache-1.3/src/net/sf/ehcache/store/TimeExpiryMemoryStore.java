/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package net.sf.ehcache.store;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tcclient.ehcache.TimeExpiryMap;

import java.util.Map;

public class TimeExpiryMemoryStore extends MemoryStore {
  private static final Log LOG = LogFactory.getLog(TimeExpiryMemoryStore.class.getName());

  public TimeExpiryMemoryStore(Ehcache cache, DiskStore diskStore) {
    super(cache, diskStore);

    try {
      map = loadMapInstance(cache.getName());
    } catch (CacheException e) {
      LOG.error(cache.getName() + "Cache: Cannot start TimeExpiryMemoryStore. Initial cause was " + e.getMessage(), e);
    }
  }
  
  private Map loadMapInstance(String cacheName) throws CacheException {
    try {
      Class.forName("com.tcclient.ehcache.TimeExpiryMap");
      Map candidateMap = new SpoolingTimeExpiryMap(1 /* 1 sec */, cache.getTimeToLiveSeconds(), cacheName);
      if (LOG.isDebugEnabled()) {
        LOG.debug(cache.getName() + " Cache: Using SpoolingTimeExpiryMap implementation");
      }
      return candidateMap;
    } catch (Exception e) {
      // Give up
      throw new CacheException(cache.getName() + "Cache: Cannot find com.tcclient.ehcache.TimeExpiryMap.");
    }
  }
  
  public final void stopTimeMonitoring() {
    ((SpoolingTimeExpiryMap) map).stopTimeMonitoring();
  }

  public final void evictExpiredElements() {
    ((SpoolingTimeExpiryMap) map).evictExpiredElements();
  }

  public final synchronized int getHitCount() {
    return ((SpoolingTimeExpiryMap) map).getHitCount();
  }

  public final synchronized int getMissCountExpired() {
    return ((SpoolingTimeExpiryMap) map).getMissCountExpired();
  }

  public final synchronized int getMissCountNotFound() {
    return ((SpoolingTimeExpiryMap) map).getMissCountNotFound();
  }

  public final synchronized boolean isExpired(final Object key) {
    return ((SpoolingTimeExpiryMap) map).isExpired(key);
  }

  public final synchronized void clearStatistics() {
    ((SpoolingTimeExpiryMap) map).clearStatistics();
  }

  public final class SpoolingTimeExpiryMap extends TimeExpiryMap {

    public SpoolingTimeExpiryMap(long timeToIdleSec, long timeToLiveSec, String cacheName) {
      super(timeToIdleSec, timeToLiveSec, cacheName);
    }

    protected final synchronized void processExpired(Object key, Object value) {
      // If cache is null, the cache has been disposed and the invalidator thread will be stopping soon.
      
      if (cache == null) { return; }
      // Already removed from the map at this point
      Element element = (Element) value;
      // When max size is 0
      if (element == null) { return; }
      
      LOG.info("Cache " + cache.getName() + " ProcessExpired -- key: " + element.getKey() + ", value: " + element.getValue());

      // check for expiry before going to the trouble of spooling
      if (element.isExpired()) {
        notifyExpiry(element);
      } else {
        evict(element);
      }
    }

    public final void evictExpiredElements() {
      timeExpiryDataStore.evictExpiredElements();
    }
  }

}
