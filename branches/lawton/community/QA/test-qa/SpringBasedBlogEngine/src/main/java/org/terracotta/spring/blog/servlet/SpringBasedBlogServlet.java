/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.spring.blog.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.terracotta.spring.blog.Blog;
import org.terracotta.spring.blog.BlogEngine;
import org.terracotta.spring.blog.BlogEntry;
import org.terracotta.spring.blog.BlogException;
import org.terracotta.spring.blog.BlogUser;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * This class is grotesque; someone who better knows how to write a web
 * front end in Spring should probably re-write this class.
 */
public final class SpringBasedBlogServlet extends HttpServlet {

  private static final String COMMAND_NAME                 = "action";

  private static final String CREATE_BLOG                  = "create";
  private static final String SHOW_BLOG                    = "view";
  private static final String REMOVE_BLOG                  = "remove";
  private static final String POST_TO_BLOG                 = "post";

  private static final String BLOG_NAME                    = "blog";

  private static final String COMMENT                      = "comment";
  private static final String POSTER_NAME                  = "poster_name";

  private static final String SHOW_BLOG_HYPERLINK_FORMAT   = "<a href=\"{0}?" + COMMAND_NAME + "=" + SHOW_BLOG + "&"
                                                             + BLOG_NAME + "={1}\">{2}</a>";
  private static final String REMOVE_BLOG_HYPERLINK_FORMAT = "<a href=\"{0}?" + COMMAND_NAME + "=" + REMOVE_BLOG + "&"
                                                             + BLOG_NAME + "={1}\">{2}</a>";

  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
      IOException {
    doGet(request, response);
  }

  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
      IOException {
    String command = request.getParameter(COMMAND_NAME);
    if (command == null) command = "";
    else command = command.trim();
    if (CREATE_BLOG.equalsIgnoreCase(command)) {
      createBlog(request, response);
    } else if (SHOW_BLOG.equalsIgnoreCase(command)) {
      showBlog(request, response);
    } else if (POST_TO_BLOG.equalsIgnoreCase(command)) {
      postToBlog(request, response);
    } else if (REMOVE_BLOG.equalsIgnoreCase(command)) {
      removeBlog(request, response);
    } else {
      listBlogs(request, response, null);
    }
  }

  private void createBlog(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    final Blog blogToCreate;
    try {
      blogToCreate = getBlog(request, true);
      listBlogs(request, response, "Blog [" + blogToCreate.getName() + "] created");
    } catch (BlogException be) {
      listBlogs(request, response, "Caught exception while trying to create blog: " + be.getMessage());
    }
  }

  private void showBlog(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
      IOException {
    try {
      final Blog blogToShow = getBlog(request, false);
      if (blogToShow == null) throw new BlogException("requested blog does not exist, please create it first");
      final StringBuffer body = new StringBuffer();
      body.append(htmlTag("h1", "Blog " + blogToShow.getName()));
      boolean hadEntries = false;
      for (Iterator<BlogEntry> pos = blogToShow.getEntries(); pos.hasNext(); hadEntries = true) {
        final BlogEntry entry = pos.next();
        body.append("<hr>Posted by: <em>").append(entry.getCreatedBy().getUsername()).append("</em> at ")
            .append(new Date(entry.getTimestamp()).toString()).append("<br>");
        body.append(htmlTag("em", entry.getText()));
      }
      if (!hadEntries) {
        body.append(htmlTag("em", "No entries"));
      }
      body.append("<hr>");
      body.append("<form action='" + request.getRequestURI() + "' method='post'>");
      body.append("<input type='hidden' name='").append(COMMAND_NAME).append("' value='").append(POST_TO_BLOG)
          .append("'>");
      body.append("<input type='hidden' name='").append(BLOG_NAME).append("' value='").append(blogToShow.getName())
          .append("'>");
      body.append("<textarea rows='8' cols='30' name='").append(COMMENT)
          .append("'>Comment on this blog here</textarea>");
      body.append("<br>");
      body.append("Your name (15 chars max): <input type='text' name='").append(POSTER_NAME)
          .append("' maxlength='15'>");
      body.append("<br>");
      body.append("<input type='submit' value='Post my comment!'>");
      body.append("</form>");
      body.append("<hr>");
      body.append("Return to the <a href='" + request.getRequestURI() + "'>blog list</a>");

      final PrintWriter webClient = getWriter(response);
      webClient.write(htmlHeader("Spring-based Blog Application - '" + blogToShow.getName() + "' BLOG"));
      webClient.write(htmlTag("body", body.toString()));
      webClient.write(htmlFooter());
      webClient.close();
    } catch (BlogException be) {
      listBlogs(request, response, "Unable to show blog: " + be.getMessage());
    }
  }

  private void postToBlog(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    try {
      final Blog blogToPostTo = getBlog(request, false);
      if (blogToPostTo == null) throw new BlogException("requested blog does not exist, please create it first");
      final String comment = request.getParameter(COMMENT);
      if (comment == null) throw new BlogException("no comment specified");
      final String poster = request.getParameter(POSTER_NAME);
      final BlogEntry entry = new BlogEntry(poster != null && poster.trim().length() > 0 ? new BlogUser(poster.trim())
          : BlogUser.ANONYMOUS, comment.trim());
      blogToPostTo.addEntry(entry);
      showBlog(request, response);
    } catch (BlogException be) {
      listBlogs(request, response, "Caught exception while trying to post to blog: " + be.getMessage());
    }
  }

  private void removeBlog(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {
    final Blog blogToRemove;
    try {
      blogToRemove = getBlog(request, false);
      getBlogEngine().removeBlog(blogToRemove);
      listBlogs(request, response, "Blog [" + (blogToRemove != null ? blogToRemove.getName() : "null") + "] removed.");
    } catch (BlogException be) {
      listBlogs(request, response, "Caught exception while trying to remove blog: " + be.getMessage());
    }
  }

  private void listBlogs(final HttpServletRequest request, final HttpServletResponse response,
                         final String noticeMessage) throws ServletException, IOException {
    final StringBuffer body = new StringBuffer();
    if (noticeMessage != null) {
      body.append("<font color='red'>").append(noticeMessage).append("</font>");
    }
    body.append(htmlTag("h2", "Blog listing"));
    body.append("<hr>");
    try {
      final StringBuffer listing = new StringBuffer();
      listing.append("<ul>");
      for (Iterator<Blog> pos = getBlogEngine().getBlogs(); pos.hasNext();) {
        final Blog nextBlog = pos.next();
        listing.append(htmlTag("li", getBlogHyperlink(request, nextBlog) + " (" + nextBlog.getEntryCount()
                                     + " postings) " + getRemoveBlogHyperlink(request, nextBlog)));
      }
      listing.append("</ul>");
      if ("<ul></ul>".equals(listing.toString())) {
        listing.replace(0, listing.length(), "<em>No blogs are available</em>");
      }
      body.append(listing);
    } catch (BlogException be) {
      body.append(htmlTag("tt", "Caught exception getting blog list:\n" + be.getMessage()));
    }

    // Append a trailer to create a new blog
    body.append("<hr>");
    body.append("<form action='" + request.getRequestURI() + "' method='post'>");
    body.append("<input type='hidden' name='").append(COMMAND_NAME).append("' value='").append(CREATE_BLOG)
        .append("'>");
    body.append("Create a new blog: <input type='text' name='").append(BLOG_NAME).append("'>");
    body.append("</form>");
    final PrintWriter webClient = getWriter(response);
    webClient.write(htmlHeader("Spring-based Blog Application - BLOG LIST"));
    webClient.write(htmlTag("body", body.toString()));
    webClient.write(htmlFooter());
    webClient.close();
  }

  private Blog getBlog(final HttpServletRequest request, boolean createIfNecessary) throws ServletException,
      BlogException {
    Blog requestedBlog = null;
    String blogName = request.getParameter(BLOG_NAME);
    if (blogName != null) {
      blogName = blogName.trim();
      requestedBlog = getBlogEngine().getBlog(blogName, createIfNecessary);
    }
    return requestedBlog;
  }

  private BlogEngine getBlogEngine() throws ServletException, BlogException {
    final ApplicationContext appContext = (ApplicationContext) getServletContext()
        .getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    if (appContext == null) throw new ServletException("Unable to get Spring application context");
    return (BlogEngine) appContext.getBean("blogEngine", BlogEngine.class);
  }

  private String getBlogHyperlink(final HttpServletRequest request, final Blog blog) {
    try {
      return MessageFormat.format(SHOW_BLOG_HYPERLINK_FORMAT, new Object[] { request.getRequestURI(),
          URLEncoder.encode(blog.getName(), "US-ASCII"), blog.getName() });
    } catch (UnsupportedEncodingException uee) {
      throw new RuntimeException("US-ASCII not supported");
    }
  }

  private String getRemoveBlogHyperlink(final HttpServletRequest request, final Blog blog) {
    try {
      return MessageFormat.format(REMOVE_BLOG_HYPERLINK_FORMAT, new Object[] { request.getRequestURI(),
          URLEncoder.encode(blog.getName(), "US-ASCII"), "[remove]" });
    } catch (UnsupportedEncodingException uee) {
      throw new RuntimeException("US-ASCII not supported");
    }
  }

  private static String htmlHeader(final String title) {
    return new StringBuffer("<html>").append(htmlTag("head", htmlTag("title", title))).toString();
  }

  private static String htmlFooter() {
    return "</html>";
  }

  private static String htmlTag(final String tag, final String contents) {
    if (contents == null || "".equals(contents.trim())) return "<" + tag + " />";
    else return new StringBuffer("<").append(tag).append(">").append(contents).append("</" + tag + ">").toString();
  }

  private static PrintWriter getWriter(final HttpServletResponse response) throws IOException {
    final Writer rawWriter = response.getWriter();
    return rawWriter instanceof PrintWriter ? (PrintWriter) rawWriter : new PrintWriter(rawWriter);
  }

}
