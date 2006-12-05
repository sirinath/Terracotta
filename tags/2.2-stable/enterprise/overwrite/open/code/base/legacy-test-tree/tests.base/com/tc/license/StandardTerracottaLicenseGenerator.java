/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.license;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.CopyUtils;
import org.apache.commons.lang.ArrayUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class StandardTerracottaLicenseGenerator {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  public static void main(String[] args) throws Exception {
    Options options = new Options();

    Option serialNumberOption = new Option("s", "serial-number", true, "The serial number of the license (required)");
    serialNumberOption.setRequired(true);
    serialNumberOption.setType(Number.class);
    options.addOption(serialNumberOption);

    Option licenseeOption = new Option("n", "licensee", true, "The licensee (required)");
    licenseeOption.setRequired(true);
    licenseeOption.setType(String.class);
    options.addOption(licenseeOption);

    Option typeOption = new Option("t", "type", true, "The license type (required)");
    typeOption.setRequired(true);
    typeOption.setType(String.class);
    options.addOption(typeOption);

    Option expirationOption = new Option("e", "expiration", true, "The expiration date");
    expirationOption.setRequired(false);
    expirationOption.setType(String.class);
    options.addOption(expirationOption);

    Option maxL2ConnectionsOption = new Option("c", "max-l2-connections", true,
                                               "The maximum number of L2 connections to permit");
    maxL2ConnectionsOption.setRequired(false);
    maxL2ConnectionsOption.setType(Number.class);
    options.addOption(maxL2ConnectionsOption);

    Option maxL2RuntimeOption = new Option("r", "max-l2-runtime-minutes", true,
                                           "The maximum number of minutes the L2 should be able to run for");
    maxL2RuntimeOption.setRequired(false);
    maxL2RuntimeOption.setType(Number.class);
    options.addOption(maxL2RuntimeOption);

    Option enabledModulesOption = new Option("m", "enabled-modules", true, "Which modules to enable (required)");
    enabledModulesOption.setRequired(true);
    enabledModulesOption.setType(String.class);
    options.addOption(enabledModulesOption);

    Option outputFileOption = new Option("o", "output-file", true, "Where to write the license (required)");
    outputFileOption.setRequired(true);
    outputFileOption.setType(File.class);
    options.addOption(outputFileOption);

    Option keyFileOption = new Option("k", "key-file", true, "The location of the private key (required)");
    keyFileOption.setRequired(true);
    keyFileOption.setType(File.class);
    options.addOption(keyFileOption);

    Option keyDSOHAOption = new Option("h", "dso-ha-enabled", false, "DSO High Availability Enabled");
    keyDSOHAOption.setRequired(false);
    keyDSOHAOption.setType(Boolean.class);
    options.addOption(keyDSOHAOption);

    CommandLine cli = null;
    try {
      cli = new GnuParser().parse(options, args);
    } catch (MissingOptionException moe) {
      new HelpFormatter().printHelp("java " + StandardTerracottaLicenseGenerator.class.getName(), options);
      System.exit(2);
    }

    int serialNumber = ((Number) cli.getOptionObject("s")).intValue();
    String licensee = (String) cli.getOptionObject("n");
    String type = (String) cli.getOptionObject("t");

    Date expiration = null;
    if (cli.hasOption("e")) expiration = DATE_FORMAT.parse((String) cli.getOptionObject("e"));

    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    if (expiration != null) {
      cal.setTime(expiration);
      cal.set(Calendar.HOUR_OF_DAY, 0);
      expiration = cal.getTime();
    }

    int maxL2Connections = Integer.MAX_VALUE;
    if (cli.hasOption("c")) maxL2Connections = ((Number) cli.getOptionObject("c")).intValue();

    int maxL2Runtime = Integer.MAX_VALUE;
    if (cli.hasOption("r")) maxL2Runtime = 60 * ((Number) cli.getOptionObject("r")).intValue();

    String[] enabledModules = ((String) cli.getOptionObject("m")).split(",\\s*");
    for (int i = 0; i < enabledModules.length; ++i)
      enabledModules[i] = enabledModules[i].trim();

    File outputFile = (File) cli.getOptionObject("o");
    File keyFile = (File) cli.getOptionObject("k");

    boolean dsoHAEnabled = cli.hasOption("h");

    System.err.println("#" + serialNumber + ", licensee '" + licensee + "', type " + type + ", expiration "
                       + expiration + ", max L2 connections " + maxL2Connections + ", max L2 runtime " + maxL2Runtime
                       + ", enabled modules " + ArrayUtils.toString(enabledModules) + ", output file " + outputFile
                       + ", key file " + keyFile + ", DSO HA enabled " + dsoHAEnabled);

    StandardTerracottaLicense license = new StandardTerracottaLicense(serialNumber, licensee, type, expiration,
                                                                      maxL2Connections, maxL2Runtime, enabledModules,
                                                                      dsoHAEnabled);

    Signature signature = Signature.getInstance("SHA1withDSA");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    CopyUtils.copy(new FileInputStream(keyFile), baos);
    KeySpec privateKeySpec = new PKCS8EncodedKeySpec(baos.toByteArray());
    KeyFactory factory = KeyFactory.getInstance("DSA");
    PrivateKey privateKey = factory.generatePrivate(privateKeySpec);

    System.err.println("License: " + license);

    FileOutputStream out = new FileOutputStream(outputFile);
    license.writeTo(out, signature, privateKey);
    out.close();

    System.err.println("Written to '" + outputFile.getAbsolutePath() + "'.");
  }

}
