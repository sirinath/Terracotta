package com.bigmemory.samples.readwritethrough;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.CacheWriterFactory;

import java.util.Properties;

/**
 * @author Aurelien Broszniowski
 */
public class MyCacheWriterFactory extends CacheWriterFactory {
  @Override
  public CacheWriter createCacheWriter(final Ehcache ehcache, final Properties properties) {
    return new MyCacheWriter();
  }
}
