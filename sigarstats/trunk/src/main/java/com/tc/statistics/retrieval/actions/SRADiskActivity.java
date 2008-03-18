/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.tc.exception.TCRuntimeException;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

import java.util.ArrayList;
import java.util.List;

/**
 * This statistic gives the disk activity going on in the system
 * <p/>
 * It contains {@link StatisticData} with the following elements:
 * <ul>
 * <li>bytes read</li>
 * <li>bytes written</li>
 * <li>reads</li>
 * <li>writes</li>
 */
public class SRADiskActivity implements StatisticRetrievalAction {

  public final static String ACTION_NAME = "disk activity";
  public static final String ELEMENT_BYTES_READ = "bytes read";
  public static final String ELEMENT_BYTES_WRITTEN = "bytes written";
  public static final String ELEMENT_READS = "reads";
  public static final String ELEMENT_WRITES = "writes";

  private final Sigar sigar;

  public SRADiskActivity() {
    sigar = new Sigar();
  }

  public StatisticData[] retrieveStatisticData() {
    try {
      long bytesRead = 0;
      long bytesWrite = 0;
      long reads = 0;
      long writes = 0;

      FileSystem[] list = sigar.getFileSystemList();
      for (int i = 0; i < list.length; i++) {
        try {
          FileSystemUsage usage = sigar.getFileSystemUsage(list[i].getDirName());
          bytesRead += usage.getDiskReadBytes();
          bytesWrite += usage.getDiskWriteBytes();
          reads += usage.getDiskReads();
          writes += usage.getDiskWrites();
        } catch (SigarException e) {
          //ignore
          //e.g. on win32 D:\ fails with "Device not ready"
          //if there is no cd in the drive.
        }
      }
      List data = new ArrayList();
      data.add(new StatisticData(ACTION_NAME, ELEMENT_BYTES_READ, new Long(bytesRead)));
      data.add(new StatisticData(ACTION_NAME, ELEMENT_BYTES_WRITTEN, new Long(bytesWrite)));
      data.add(new StatisticData(ACTION_NAME, ELEMENT_READS, new Long(reads)));
      data.add(new StatisticData(ACTION_NAME, ELEMENT_WRITES, new Long(writes)));

      return (StatisticData[])data.toArray(new StatisticData[data.size()]);

    } catch (SigarException e) {
      throw new TCRuntimeException(e);
    }
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public static void main(String[] args) {
    final SRADiskActivity sra = new SRADiskActivity();
    new Thread(new Runnable() {
      public void run() {
        while (true) {
          StatisticData[] data = sra.retrieveStatisticData();
          for (int i = 0; i < data.length; i++) {
            StatisticData statisticData = data[i];
            System.out.println(statisticData.toString());
          }
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
          }
        }
      }
    }).start();
  }
}