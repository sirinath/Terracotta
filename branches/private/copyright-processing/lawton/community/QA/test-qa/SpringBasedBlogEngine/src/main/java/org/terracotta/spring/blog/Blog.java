/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.spring.blog;

import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A simple, ordered collection of blog entries.
 */
public class Blog {

  private final String    name;
  private List<BlogEntry> entries;

  public Blog() throws BlogException {
    this("Default blog created at: " + Calendar.getInstance());
  }

  public Blog(final String name) throws BlogException {
    this.name = name;
    entries = new LinkedList<BlogEntry>();
  }

  public String getName() {
    return name;
  }

  public synchronized int getEntryCount() {
    return entries.size();
  }

  public synchronized Iterator<BlogEntry> getEntries() {
    return Collections.unmodifiableList(entries).iterator();
  }

  public synchronized void addEntry(final BlogEntry entry) {
    entries.add(entry);
  }

}
