/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package org.terracotta.spring.blog;

public class BlogException extends Exception {

  public BlogException() {
    super();
  }

  public BlogException(String message) {
    super(message);
  }

  public BlogException(Throwable cause) {
    super(cause);
  }

  public BlogException(String message, Throwable cause) {
    super(message, cause);
  }

}
