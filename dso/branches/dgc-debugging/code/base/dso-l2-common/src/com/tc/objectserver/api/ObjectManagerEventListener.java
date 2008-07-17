/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.api;

import java.util.SortedSet;

/**
 * Interface for those interested in listening to Object Manager events. I'm thinking this event interface should really
 * only be for "low volume" events since there is fair amount of overhead per event. So things like "object looked up",
 * or "cache hit" aren't very good candidates for this interface
 */
public interface ObjectManagerEventListener {

  /**
   * notify the listener that GCStats object has been updated
   * @param stats statistics about this collection
   */
  public void updateGCStatus(GCStats stats);

  /**
   * Called after each GC run is complete
   * 
   * @param stats statistics about this collection
   * @param deleted List of deleted ObjectIDs
   */
  public void garbageCollectionComplete(GCStats stats, SortedSet deleted);
}
