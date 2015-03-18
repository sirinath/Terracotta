package com.bigmemory.samples.readwritethrough;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.loader.CacheLoader;

import java.util.Collection;
import java.util.Map;

public class MyCacheLoader implements CacheLoader {

  public Object load(final Object o) throws CacheException {
    return new Element(o, "somevalue");
  }

  public Map loadAll(final Collection collection) {
    return null;
  }

  public Object load(final Object o, final Object o1) {
    return null;
  }

  public Map loadAll(final Collection collection, final Object o) {
    return null;
  }

  public String getName() {
    return null;
  }

  public CacheLoader clone(final Ehcache ehcache) throws CloneNotSupportedException {
    return null;
  }

  public void init() {

  }

  public void dispose() throws CacheException {

  }

  public Status getStatus() {
    return null;
  }
}
