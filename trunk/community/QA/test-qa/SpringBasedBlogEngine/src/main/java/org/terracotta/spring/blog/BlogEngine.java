/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.spring.blog;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class BlogEngine {

  private final Map<String, Blog> blogs;

  public BlogEngine() {
    blogs = new HashMap<String, Blog>();
  }

  public Blog getBlog(final String name) throws BlogException {
    return getBlog(name, true);
  }

  public Blog getBlog(final String name, boolean createIfNecessary) throws BlogException {
    Blog blog;
    synchronized (blogs) {
      blog = blogs.get(name);
      if (blog == null && createIfNecessary) {
        blog = new Blog(name);
        blogs.put(name, blog);
      }
    }
    return blog;
  }

  public void removeBlog(final Blog blog) {
    synchronized (blogs) {
      blogs.remove(blog.getName());
    }
  }

  public Iterator<Blog> getBlogs() {
    synchronized (blogs) {
      return Collections.unmodifiableCollection(blogs.values()).iterator();
    }
  }

}
