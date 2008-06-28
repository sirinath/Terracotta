/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.tcdev.refreshall;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import java.io.IOException;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

  public enum ConsoleStream {

    DEFAULT, STDOUT, STDERR;

    public MessageConsoleStream stream() {
      return stream;
    }

    private MessageConsoleStream stream;
    private Color                color;

  }

  // The plug-in ID
  public static final String PLUGIN_ID = "org.terracotta.tcdev";

  // The shared instance
  private static Activator   plugin;

  private static final RGB   BLACK     = new RGB(0, 0, 0);
  private static final RGB   BLUE      = new RGB(0, 0, 255);
  private static final RGB   RED       = new RGB(255, 0, 0);

  private MessageConsole     console;

  public Activator() {
    plugin = this;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    console = new MessageConsole("Terracotta build system", null);
    final Device device = console.getFont() != null ? console.getFont().getDevice() : null;
    initStream(ConsoleStream.DEFAULT, device, BLACK);
    initStream(ConsoleStream.STDOUT, device, BLUE);
    initStream(ConsoleStream.STDERR, device, RED);
    final IConsoleManager mgr = ConsolePlugin.getDefault().getConsoleManager();
    mgr.addConsoles(new IConsole[] { console });
    super.start(context);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    final IConsoleManager mgr = ConsolePlugin.getDefault().getConsoleManager();
    mgr.removeConsoles(new IConsole[] { console });
    console = null;
    destroyStream(ConsoleStream.DEFAULT);
    destroyStream(ConsoleStream.STDOUT);
    destroyStream(ConsoleStream.STDERR);
    super.stop(context);
  }

  private void initStream(final ConsoleStream consoleStream, final Device device, final RGB rgb) {
    consoleStream.color = new Color(device, rgb);
    consoleStream.stream = console.newMessageStream();
    consoleStream.stream.setActivateOnWrite(true);
    consoleStream.stream.setColor(consoleStream.color);
  }

  private void destroyStream(final ConsoleStream consoleStream) throws IOException {
    consoleStream.stream().setColor(null);
    consoleStream.stream.close();
    consoleStream.color.dispose();
    consoleStream.color = null;
    consoleStream.stream = null;
  }

  public static Activator getDefault() {
    return plugin;
  }

  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(PLUGIN_ID, path);
  }

}
