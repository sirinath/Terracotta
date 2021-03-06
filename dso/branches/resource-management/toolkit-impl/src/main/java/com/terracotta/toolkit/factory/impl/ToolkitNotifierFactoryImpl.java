/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.bytecode.PlatformService;
import com.terracotta.toolkit.TerracottaProperties;
import com.terracotta.toolkit.events.DestroyableToolkitNotifier;
import com.terracotta.toolkit.events.ToolkitNotifierImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.ToolkitObjectType;
import com.terracotta.toolkit.roots.ToolkitTypeRootsFactory;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An implementation of {@link ClusteredListFactory}
 */
public class ToolkitNotifierFactoryImpl extends
    AbstractPrimaryToolkitObjectFactory<ToolkitNotifier, ToolkitNotifierImpl> {
  private static final TCLogger                    LOGGER                            = TCLogging
                                                                                         .getLogger(ToolkitNotifierFactoryImpl.class);
  public static final String                       TOOLKIT_NOTIFIER_EXECUTOR_SERVICE = "toolkitNotifierExecutorService";

  private static final NotifierIsolatedTypeFactory FACTORY                           = new NotifierIsolatedTypeFactory();

  public ToolkitNotifierFactoryImpl(ToolkitInternal toolkit, ToolkitTypeRootsFactory rootsFactory,
                                    PlatformService platformService) {
    super(toolkit, rootsFactory.createAggregateIsolatedTypeRoot(ToolkitTypeConstants.TOOLKIT_NOTIFIER_ROOT_NAME,
                                                                FACTORY, platformService));

    final ExecutorService notifierService = createExecutorService(platformService);
    ExecutorService service = platformService.registerObjectByNameIfAbsent(TOOLKIT_NOTIFIER_EXECUTOR_SERVICE,
                                                                           notifierService);
    if (service == notifierService) {
      registerForShutdown(notifierService);
    }
  }

  private void registerForShutdown(final ExecutorService notifierService) {
    toolkit.registerBeforeShutdownHook(new Runnable() {
      @Override
      public void run() {
        LOGGER.info("Shutting Down Notifier Thread Pool");
        notifierService.shutdown();
      }
    });
  }

  private ExecutorService createExecutorService(PlatformService platformService) {
    int maxNotifierThreadCount = new TerracottaProperties(platformService).getInteger("maxToolkitNotifierThreadCount",
                                                                                      20);
    final ExecutorService notifierService = new ThreadPoolExecutor(0, maxNotifierThreadCount, 60L, TimeUnit.SECONDS,
                                                                   new LinkedBlockingDeque<Runnable>(),
                                                                   new ThreadFactory() {
                                                                     private final AtomicInteger count = new AtomicInteger();

                                                                     @Override
                                                                     public Thread newThread(Runnable runnable) {
                                                                       Thread thread = new Thread(
                                                                                                  runnable,
                                                                                                  "ToolkitNotifier-"
                                                                                                      + count
                                                                                                          .incrementAndGet());
                                                                       thread.setDaemon(true);
                                                                       return thread;
                                                                     }
                                                                   });
    return notifierService;
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.NOTIFIER;
  }

  private static class NotifierIsolatedTypeFactory implements
      IsolatedToolkitTypeFactory<ToolkitNotifier, ToolkitNotifierImpl> {

    @Override
    public ToolkitNotifier createIsolatedToolkitType(ToolkitObjectFactory factory, String name, Configuration config,
                                                     ToolkitNotifierImpl tcClusteredObject) {
      return new DestroyableToolkitNotifier(factory, tcClusteredObject, name);
    }

    @Override
    public ToolkitNotifierImpl createTCClusteredObject(Configuration config) {
      return new ToolkitNotifierImpl();
    }

  }

}