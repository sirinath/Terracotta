/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.Stats;
import com.tc.util.concurrent.ThreadPreferenceExecutor;
import com.tc.util.concurrent.ThreadUtil;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;
import javax.management.remote.message.MBeanServerRequestMessage;
import javax.management.remote.message.Message;

public class RemoteJMXProcessor implements Sink {

  private static final TCLogger logger = TCLogging.getLogger(RemoteJMXProcessor.class);

  private final Executor        executor;

  private static final Map<Integer, String> MESSAGE_METHOD_ID_TO_NAME = new HashMap<Integer, String>();

  static {
    try {
      Field[] fields = MBeanServerRequestMessage.class.getFields();
      for (Field field : fields) {
        if (field.getType() == int.class) {
          MESSAGE_METHOD_ID_TO_NAME.put((Integer) field.get(MBeanServerRequestMessage.class), field.getName());
        }
      }
    } catch (IllegalAccessException iae) {
      logger.warn("cannot translate MBeanServerRequestMessage method IDs to names", iae);
    }
  }

  public RemoteJMXProcessor() {
    TCProperties props = TCPropertiesImpl.getProperties();
    int maxThreads = props.getInt(TCPropertiesConsts.L2_REMOTEJMX_MAXTHREADS);
    int idleTime = props.getInt(TCPropertiesConsts.L2_REMOTEJMX_IDLETIME);

    // we're not using a standard thread pool executor here since it seems that some jmx tasks are inter-dependent (such
    // that if they are queued, things will lock up)
    executor = new ThreadPreferenceExecutor(getClass().getSimpleName(), maxThreads, idleTime, TimeUnit.SECONDS,
                                            TCLogging.getLogger(RemoteJMXProcessor.class));
  }

  @Override
  public void add(final EventContext context) {
    final CallbackExecuteContext callbackContext = (CallbackExecuteContext) context;
    String callDescription = buildJmxRequestDescription(callbackContext.getRequest());
    final String threadName = getClass().getSimpleName() + " " + callDescription;

    long before = System.nanoTime();
    try {
      int retries = 0;
      while (true) {
        try {
          executor.execute(new Runnable() {
            @Override
            public void run() {
              Thread currentThread = Thread.currentThread();
              ClassLoader prevLoader = currentThread.getContextClassLoader();
              String prevName = currentThread.getName();
              currentThread.setContextClassLoader(callbackContext.getThreadContextLoader());
              currentThread.setName(threadName);

              try {
                Message result = callbackContext.getCallback().execute(callbackContext.getRequest());
                callbackContext.getFuture().set(result);
              } catch (Throwable t) {
                callbackContext.getFuture().setException(t);
              } finally {
                currentThread.setContextClassLoader(prevLoader);
                currentThread.setName(prevName);
              }
            }
          });
          break;
        } catch (RejectedExecutionException e) {
          ThreadUtil.reallySleep(10);
          retries++;
        }
        if (retries % 100 == 0) {
          logger.warn("JMX Processor is saturated. Retried processing [" + callDescription + "] " + retries + " times.");
        }
      }
    } catch (Throwable t) {
      callbackContext.getFuture().setException(t);
    } finally {
      long delay = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);
      if (delay > 5000) {
        logger.warn("Slow JMX Processor request: " + callDescription + ", took " + delay + " ms");
      }
    }
  }

  private static String buildJmxRequestDescription(Message message) {
    if (message instanceof MBeanServerRequestMessage) {
      MBeanServerRequestMessage request = (MBeanServerRequestMessage) message;
      String methodName = methodIdToName(request.getMethodId());
      request.getParams();

      StringBuilder sb = new StringBuilder(methodName).append("(");
      for (Object o : request.getParams()) {
        String param = null;
        if (o != null) {
          if (o instanceof ObjectName || o instanceof String) {
            param = "'" + o.toString() + "'";
          } else {
            param = o.getClass().getSimpleName();
          }
        }

        sb.append(param);
        sb.append(", ");
      }
      if (request.getParams().length > 0) {
        sb.deleteCharAt(sb.length() - 1);
        sb.deleteCharAt(sb.length() - 1);
      }
      sb.append(")");

      return sb.toString();
    }
    return message == null ? null : message.getClass().getSimpleName();
  }

  private static String methodIdToName(int methodId) {
    String methodName = MESSAGE_METHOD_ID_TO_NAME.get(methodId);
    if (methodName != null) {
      return methodName;
    } else {
      return "?" + methodId + "?";
    }
  }

  @Override
  public boolean addLossy(EventContext context) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addMany(Collection contexts) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public AddPredicate getPredicate() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAddPredicate(AddPredicate predicate) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void enableStatsCollection(boolean enable) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Stats getStats(long frequency) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Stats getStatsAndReset(long frequency) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isStatsCollectionEnabled() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void resetStats() {
    throw new UnsupportedOperationException();
  }

}
