/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.spring.blog;

/**
 * Maintains basic information about a particular blog entry.
 */
public class BlogEntry {

  private BlogUser createdBy;
  private long     timestamp;
  private String   text;

  /**
   * Creates an empty, anonymous entry.
   */
  public BlogEntry() {
    createdBy = BlogUser.ANONYMOUS;
    timestamp = System.currentTimeMillis();
    text = null;
  }

  /**
   * Creates an anonymous entry, timestamped at object creation time.
   */
  public BlogEntry(final String text) {
    this(BlogUser.ANONYMOUS, System.currentTimeMillis(), text);
  }

  /**
   * Creates a signed entry, timestamped at object creation time.
   */
  public BlogEntry(final BlogUser createdBy, final String text) {
    this(createdBy, System.currentTimeMillis(), text);
  }

  private BlogEntry(final BlogUser createdBy, final long timestamp, final String text) {
    this();
    setCreatedBy(createdBy);
    this.timestamp = timestamp;
    setText(text);
  }

  public synchronized BlogUser getCreatedBy() {
    return createdBy;
  }

  public synchronized void setCreatedBy(final BlogUser createdBy) {
    this.createdBy = createdBy != null ? createdBy : BlogUser.ANONYMOUS;
  }

  public synchronized String getText() {
    return text;
  }

  public synchronized void setText(final String text) {
    this.text = text;
  }

  public synchronized long getTimestamp() {
    return timestamp;
  }

}
