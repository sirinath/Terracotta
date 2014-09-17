/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.util.Assert;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class TerracottaOperatorEventImpl implements TerracottaOperatorEvent, Comparable<TerracottaOperatorEventImpl> {
  private final long                    time;
  private final String                  eventMessage;
  private final EventType               eventType;
  private final EventSubsystem          subSystem;
  private volatile Map<String, Integer> nodes;
  private boolean                       isRead = false;
  private final String                  collapseString;

  public TerracottaOperatorEventImpl(EventType eventType, EventSubsystem subSystem, String message,
                                     String collapseString) {
    this(eventType, subSystem, System.currentTimeMillis(), message, collapseString, new HashMap<String, Integer>());
  }

  public TerracottaOperatorEventImpl(EventType eventType, EventSubsystem subSystem, String message,
                                     long time, String collapseString) {
    this(eventType, subSystem, time, message, collapseString, new HashMap<String, Integer>());
  }

  private TerracottaOperatorEventImpl(EventType eventType, EventSubsystem subsystem, long time, String message,
                                      String collapseString, Map<String, Integer> nodes) {
    // Using a CHM here can trigger CDV-1377 if event sent/serialized from a custom mode L1
    if (nodes instanceof ConcurrentHashMap) { throw new AssertionError("CHM not allowed here"); }

    this.eventType = eventType;
    this.subSystem = subsystem;
    this.time = time;
    this.eventMessage = message;
    this.collapseString = collapseString;
    this.nodes = nodes;

  }

  @Override
  public String getEventMessage() {
    return this.eventMessage;
  }

  @Override
  public Date getEventTime() {
    return new Date(this.time);
  }

  @Override
  public EventType getEventType() {
    return this.eventType;
  }

  @Override
  public String getEventTypeAsString() {
    return this.eventType.name();
  }

  @Override
  public synchronized String getNodeName() {
    StringBuilder val = new StringBuilder();
    for (Entry<String, Integer> node : this.nodes.entrySet()) {
      Assert.assertTrue(node.getValue().intValue() >= 1);
      val.append(node.getKey());
      if (node.getValue().intValue() > 1) {
        val.append("(").append(node.getValue().intValue()).append(")");
      }
      val.append(" ");
    }
    return val.toString();
  }

  @Override
  public synchronized void addNodeName(String nodeId) {
    Integer numOfSuchEvents = this.nodes.get(nodeId);
    if (numOfSuchEvents == null) {
      this.nodes.put(nodeId, 1);
    } else {
      this.nodes.put(nodeId, numOfSuchEvents.intValue() + 1);
    }
  }

  @Override
  public EventSubsystem getEventSubsystem() {
    return this.subSystem;
  }

  @Override
  public String getEventSubsystemAsString() {
    return this.subSystem.name();
  }

  @Override
  public String getCollapseString() {
    return collapseString;
  }

  @Override
  public int compareTo(TerracottaOperatorEventImpl o) {
    return (int) (this.time - o.time);
  }

  @Override
  public boolean isRead() {
    return this.isRead;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TerracottaOperatorEventImpl)) return false;
    TerracottaOperatorEventImpl event = (TerracottaOperatorEventImpl) o;
    if (this.eventType != event.eventType) return false;
    if (this.subSystem != event.subSystem) return false;
    if (this.time != event.time) return false;
    if (!this.collapseString.equals(event.collapseString)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((collapseString == null) ? 0 : collapseString.hashCode());
    result = prime * result + ((eventType == null) ? 0 : eventType.hashCode());
    result = prime * result + ((subSystem == null) ? 0 : subSystem.hashCode());
    result = prime * result + (int) (time ^ (time >>> 32));
    return result;
  }

  @Override
  public void markRead() {
    this.isRead = true;
  }

  @Override
  public void markUnread() {
    this.isRead = false;
  }

  @Override
  public String toString() {
    return getEventType() + " " + getEventTime() + " " + getNodeName() + " " + getEventSubsystemAsString() + " "
           + getEventMessage();
  }

  @Override
  public String extractAsText() {
    return toString();
  }

  @Override
  public TerracottaOperatorEvent cloneEvent() {
    Map<String, Integer> nodesCopy;
    synchronized (this) {
      nodesCopy = new HashMap<String, Integer>(this.nodes);
    }
    return new TerracottaOperatorEventImpl(this.eventType, this.subSystem, this.time, this.eventMessage,
                                           this.collapseString, nodesCopy);

  }

  // STRICTLY FOR TESTS
  public Map<String, Integer> getNodes() {
    return this.nodes;
  }
}
