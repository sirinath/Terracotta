package net.sf.ehcache.store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A typesafe enumeration of eviction policies. The policy used to evict elements from the
 * {@link net.sf.ehcache.store.MemoryStore}. This can be one of:
 * <ol>
 * <li>LRU - least recently used
 * <li>LFU - least frequently used
 * <li>FIFO - first in first out, the oldest element by creation time
 * </ol>
 * The default value is LRU
 * 
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 * @since 1.2
 */
public final class MemoryStoreEvictionPolicyTC {

  /**
   * LRU - least recently used.
   */
  public static final MemoryStoreEvictionPolicyTC LRU  = new MemoryStoreEvictionPolicyTC("LRU");

  /**
   * LFU - least frequently used.
   */

  public static final MemoryStoreEvictionPolicyTC LFU  = new MemoryStoreEvictionPolicyTC("LFU");

  /**
   * FIFO - first in first out, the oldest element by creation time.
   */
  public static final MemoryStoreEvictionPolicyTC FIFO = new MemoryStoreEvictionPolicyTC("FIFO");

  public static final MemoryStoreEvictionPolicyTC DSO  = new MemoryStoreEvictionPolicyTC("DSO");

  private static final Log                        LOG  = LogFactory.getLog(MemoryStoreEvictionPolicyTC.class.getName());

  // for debug only
  private final String                            myName;

  /**
   * This class should not be subclassed or have instances created.
   * 
   * @param policy
   */
  private MemoryStoreEvictionPolicyTC(String policy) {
    myName = policy;
  }

  /**
   * @return a String representation of the policy
   */
  public String toString() {
    return myName;
  }

  /**
   * Converts a string representation of the policy into a policy.
   * 
   * @param policy either LRU, LFU or FIFO
   * @return one of the static instances
   */
  public static MemoryStoreEvictionPolicyTC fromString(String policy) {
    if (policy != null) {
      if (policy.equalsIgnoreCase("LRU")) {
        return LRU;
      } else if (policy.equalsIgnoreCase("LFU")) {
        return LFU;
      } else if (policy.equalsIgnoreCase("FIFO")) {
        return FIFO;
      } else if (policy.equalsIgnoreCase("DSO")) { return DSO; }
    }

    if (LOG.isWarnEnabled()) {
      LOG
          .warn("The memoryStoreEvictionPolicy of " + policy + " cannot be resolved. The policy will be"
                + " set to LRU");
    }
    return LRU;
  }
}
