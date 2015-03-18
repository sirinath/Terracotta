package com.bigmemory.samples.readwritethrough;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.loader.CacheLoaderFactory;

import java.util.Properties;

/**
 * @author Aurelien Broszniowski
 */
public class MyCacheLoaderFactory extends CacheLoaderFactory {
  @Override
  public CacheLoader createCacheLoader(final Ehcache ehcache, final Properties properties) {
    return new MyCacheLoader();
  }
}
