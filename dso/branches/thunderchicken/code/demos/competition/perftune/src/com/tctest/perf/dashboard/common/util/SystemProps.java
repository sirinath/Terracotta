package com.tctest.perf.dashboard.common.util;

public final class SystemProps {

  public static final String INGESTOR_SLEEP_TIME_MILLIS = "com.tctest.IngestorSleepTimeMillis";
  public static final String INGESTOR_THREAD_COUNT = "com.tctest.IngestorThreadCount";
  public static final String INGESTOR_QUEUE_SIZE = "com.tctest.QueueSize";
  
  public static final String EVENT_CHRONICLE_CLASS = "com.tctest.EventChronicleClass";

  public static int getInt(String sysProp, int defaultValue){
    String valueString = System.getProperty(sysProp);
    System.out.println("<!><!><!><!> System prop: " + sysProp + ", value " + valueString);
    return (valueString == null? defaultValue: Integer.parseInt(valueString));
  }

}
