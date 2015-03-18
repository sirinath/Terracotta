package com.bigmemory.samples.readwritethrough;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import java.io.IOException;

/**
 * <p/>
 * Read and Write through
 * <p/>
 */
public class ReadWriteThrough {
  public static void main(String[] args) throws IOException {
    System.out.println("**** Retrieve config from xml ****");
    CacheManager manager = CacheManager.newInstance(ReadWriteThrough.class.getResource("/xml/ehcache-readwritethrough.xml"));
    try {
      Cache readWriteThroughCache = manager.getCache("readWriteThroughCache");

      System.out.println("We want to read from the cache, it is going to miss and read from the CacheLoader (hitting the SOR)");
      readWriteThroughCache.get(1);

      System.out.println("We want to read again from the cache, now, it is going to hit and not read from the CacheLoader");
      readWriteThroughCache.get(1);

      System.out.println("We write into the cache, it is going to call the CacheWriter to write to the SOR");
      readWriteThroughCache.putWithWriter(new Element(1, "something"));

      System.out.println("We want to read again from the cache, it is still going to hit and read from the CacheLoader");
      readWriteThroughCache.get(1);

    } finally {
      if (manager != null) manager.shutdown();
    }
  }

}
