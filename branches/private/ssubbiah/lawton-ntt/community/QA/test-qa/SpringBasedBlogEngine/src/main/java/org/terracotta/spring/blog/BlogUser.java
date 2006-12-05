/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.spring.blog;

/**
 * Blogger identity.
 */
public class BlogUser {

  public static final BlogUser ANONYMOUS = new BlogUser("Anonymous Coward");

  private String               username;

  public BlogUser() {
    username = null;
  }

  public BlogUser(final String username) {
    this();
    setUsername(username);
  }

  public synchronized void setUsername(final String username) {
    this.username = username;
  }

  public synchronized String getUsername() {
    return username;
  }

}
