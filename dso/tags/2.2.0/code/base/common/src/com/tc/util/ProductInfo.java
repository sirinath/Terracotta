/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.StringUtils;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Utility class to retrieve the build information for the product.
 */
public final class ProductInfo {
  private static final BundleHelper bundleHelper              = new BundleHelper(ProductInfo.class);

  private static final DateFormat   DATE_FORMAT               = new SimpleDateFormat("yyyyMMdd-HHmmss");

  private static final String       BUILD_DATA_RESOURCE_NAME  = "/build-data.txt";

  private static final String       BUILD_DATA_ROOT_KEY       = "terracotta.build.";
  private static final String       BUILD_DATA_VERSION_KEY    = "version";
  private static final String       BUILD_DATA_TIMESTAMP_KEY  = "timestamp";
  private static final String       BUILD_DATA_HOST_KEY       = "host";
  private static final String       BUILD_DATA_USER_KEY       = "user";
  private static final String       BUILD_DATA_CHANGESET_KEY  = "revision";
  private static final String       BUILD_DATA_CHANGE_TAG_KEY = "change-tag";
  private static final String       BUILD_DATA_BRANCH_KEY     = "branch";

  private static final String       UNKNOWN_VALUE             = "[unknown]";

  // WARNING: DO NOT DO NOT DO NOT MOVE THIS DECLARATION HIGHER!
  //
  // Yes, this is really obnoxious. What's going on is this: TCLogging, in its static initializer, actually calls back
  // into this class so that it can write out the product version. However, if this class (ProductInfo) is actually
  // initialized first, before TCLogging (as, for example, is the case when you invoke its main() method to print out
  // the version), then what *was* happening is that it would first try to initialize the logger, which triggers
  // TCLogging's static initializer, which calls back into this class's getThisProductInfo() method, which in turn calls
  // the constructor, which would try to use the DATE_FORMAT constant...which is a perfectly reasonable thing to do, but
  // in this one case, DATE_FORMAT wouldn't be initialized yet, since this declaration used to be above all the
  // constants above and thus would be called first. Moving it lower, below all the other constants, corrects this
  // problem.
  //
  // Yup, order of static initializers still *can* be a problem in Java, clearly. At least the order is defined, though.
  // ;)
  private static final TCLogger   logger                      = TCLogging.getLogger(ProductInfo.class);

  private final String            moniker;
  private final String            version;
  private final Date              timestamp;
  private final String            host;
  private final String            user;
  private final String            changeset;
  private final String            changeTag;
  private final String            branch;

  private ProductInfo(InputStream in, String fromWhere) {
    Properties properties = new Properties();

    moniker = bundleHelper.getString("moniker");
    
    if (in != null) {
      try {
        properties.load(in);
      } catch (IOException ioe) {
        logger.warn(bundleHelper.format("load.properties.failure", new Object[] {fromWhere}));
      }
    }

    this.version = getProperty(properties, BUILD_DATA_VERSION_KEY, UNKNOWN_VALUE);
    String timestampString = getProperty(properties, BUILD_DATA_TIMESTAMP_KEY, null);
    this.host = getProperty(properties, BUILD_DATA_HOST_KEY, UNKNOWN_VALUE);
    this.user = getProperty(properties, BUILD_DATA_USER_KEY, UNKNOWN_VALUE);
    this.changeset = getProperty(properties, BUILD_DATA_CHANGESET_KEY, UNKNOWN_VALUE);
    this.changeTag = getProperty(properties, BUILD_DATA_CHANGE_TAG_KEY, null);
    this.branch = getProperty(properties, BUILD_DATA_BRANCH_KEY, UNKNOWN_VALUE);

    Date realTimestamp = null;
    if (timestampString != null) {
      try {
        realTimestamp = DATE_FORMAT.parse(timestampString);
      } catch (ParseException pe) {
        logger.warn(bundleHelper.format("invalid.timestamp", new Object[] {timestampString}));
      }
    }

    this.timestamp = realTimestamp;
  }

  private String getProperty(Properties properties, String name, String defaultValue) {
    String out = properties.getProperty(BUILD_DATA_ROOT_KEY + name);
    if (StringUtils.isBlank(out)) out = defaultValue;
    return out;
  }

  private static ProductInfo thisProductInfo = null;

  public static synchronized ProductInfo getThisProductInfo() {
    if (thisProductInfo == null) {
      InputStream in = ProductInfo.class.getResourceAsStream(BUILD_DATA_RESOURCE_NAME);
      thisProductInfo = new ProductInfo(in, "resource '" + BUILD_DATA_RESOURCE_NAME + "'");
    }

    return thisProductInfo;
  }

  public String moniker() {
    return this.moniker;
  }
  
  public String rawVersion() {
    return this.version;
  }

  public Date buildTimestamp() {
    return this.timestamp;
  }

  public String buildTimestampAsString() {
    if (this.timestamp == null) return UNKNOWN_VALUE;
    else return DATE_FORMAT.format(this.timestamp);
  }

  public String buildHost() {
    return this.host;
  }

  public String buildUser() {
    return this.user;
  }

  public String buildChangeset() {
    return this.changeset;
  }

  public String buildChangeTag() {
    return this.changeTag;
  }

  public String buildBranch() {
    return this.branch;
  }

  public String copyright() {
    return bundleHelper.getString("copyright");
  }

  public String toShortString() {
    return moniker + " Version " + version;
  }

  public String toLongString() {
    return toShortString() + ", as of " + buildTimestampAsString() + " (Revision " + buildChangeset()
           + (buildChangeTag() != null ? " (" + buildChangeTag() + ")" : "") + " by " + buildUser() + "@" + buildHost()
           + " from " + buildBranch() + ")";
  }

  public String toString() {
    return toShortString();
  }

  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption("v", "verbose", false, bundleHelper.getString("option.verbose"));
    options.addOption("h", "help", false, bundleHelper.getString("option.help"));

    CommandLineParser parser = new GnuParser();
    CommandLine cli = parser.parse(options, args);

    if (cli.hasOption("h")) {
      new HelpFormatter().printHelp("java " + ProductInfo.class.getName(), options);
    }

    if (cli.hasOption("v")) {
      System.out.println(getThisProductInfo().toLongString());
    } else {
      System.out.println(getThisProductInfo().toShortString());
    }
  }
}
