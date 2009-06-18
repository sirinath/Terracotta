/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.cli.command;

import com.tc.objectserver.persistence.sleepycat.util.PartitionDBData;

import java.io.File;
import java.io.Writer;

public class PartitionDBCommand extends BaseCommand {

  public PartitionDBCommand(Writer writer) {
    super(writer);
  }

  public String description() {
    return "This utility partition the sleepycat data for usage in case of other servers are added";
  }

  public void execute(String[] args) {
    if (args.length < 2) {
      printUsage();
      return;
    }

    File[] dir = new File[] { new File(args[0]) };
    int numberOfPartition = Integer.parseInt(args[1]);

    if (dir[0].exists()) {
      try {
        PartitionDBData partitionDBData = new PartitionDBData(dir, writer);
        partitionDBData.partitionData(numberOfPartition);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      println("invalid sleepycat database source.");
      printUsage();
    }
  }

  public String name() {
    return "Partition Sleepycat Data";
  }

  public String optionName() {
    return "partition-db";
  }

  public void printUsage() {
    println("\tUsage: " + optionName() + " <sleepycat source directory> <number of partitions>");
    println("\t" + description());
  }

}
