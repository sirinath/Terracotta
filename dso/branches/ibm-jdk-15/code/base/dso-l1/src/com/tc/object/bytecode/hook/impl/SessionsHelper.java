/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import com.tc.text.Banner;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SessionsHelper {

  // For dev/test use only, not intended for customer usage
  private static final String TC_SESSION_CLASSPATH = "tc.session.classpath";

  private SessionsHelper() {
    //
  }
  
  public static String[] injectClasses(String[] s) {
    List classPaths = new ArrayList(Arrays.asList(s));

    String tcSessionCP = System.getProperty(TC_SESSION_CLASSPATH);
    if (tcSessionCP != null) {
      tcSessionCP = tcSessionCP.replace('\\', '/'); // for windows

      String[] paths = tcSessionCP.split(File.pathSeparator);
      for (int i = 0; i < paths.length; i++) {
        String path = paths[i];
        if (!path.endsWith("/")) {
          path += "/";
        }

        if (!path.startsWith("/")) {
          path = "/" + path;
        }

        classPaths.add(path);
      }
    }
    s = new String[classPaths.size()];
    classPaths.toArray(s);
    return s;
  }

  public static void injectClasses(ClassLoader loader) throws Exception {
    List classPaths = new ArrayList();

    String tcSessionCP = System.getProperty(TC_SESSION_CLASSPATH);
    if (tcSessionCP != null) {
      tcSessionCP = tcSessionCP.replace('\\', '/'); // for windows

      String[] paths = tcSessionCP.split(File.pathSeparator);
      for (int i = 0; i < paths.length; i++) {
        String path = paths[i];
        if (!path.endsWith("/")) {
          path += "/";
        }

        if (!path.startsWith("/")) {
          path = "/" + path;
        }

        classPaths.add(path);
      }
    } else {
      File installRoot = ClassProcessorHelper.getTCInstallDir(false);
      File sessionsLib = new File(installRoot, "lib");
      File tcSessionLib = new File(sessionsLib, "session");

      if (!tcSessionLib.exists() || !tcSessionLib.isDirectory() || !tcSessionLib.canRead()) {
        Banner.errorBanner(tcSessionLib + " does not exist, or is not an accessible directory");
        Util.exit();
      }

      classPaths.add(appendPath(tcSessionLib.getAbsolutePath(), "tc-session.jar"));
    }

//    Method m = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
//    m.setAccessible(true);
//    for (Iterator iter = classPaths.iterator(); iter.hasNext();) {
//      m.invoke(loader, new Object[] { new URL("file", "", (String) iter.next()) });
//    }
    
    if (!invokeAddURLMethodIfPresent(loader, classPaths)) {
      if (!invokeAddPathsMethodIfPresent(loader, classPaths)) {
        throw new AssertionError("SessionsHelper.injectClasses cannot recognize classloader of type: " + loader.getClass().getName());
      }
    }
  }
  
  private static boolean invokeAddPathsMethodIfPresent(ClassLoader loader, List classPaths) {
    try {
      Method m = loader.getClass().getDeclaredMethod("addPaths", new Class[]{String[].class});
      m.setAccessible(true);
      String[] paths = new String[classPaths.size()];
      classPaths.toArray(paths);
      m.invoke(loader, new Object[] { paths });
      return true;
    } catch (SecurityException e) {
      return false;
    } catch (NoSuchMethodException e) {
      return false;
    } catch (IllegalArgumentException e) {
      return false;
    } catch (IllegalAccessException e) {
      return false;
    } catch (InvocationTargetException e) {
      return false;
    }
  }
  
  private static boolean invokeAddURLMethodIfPresent(ClassLoader loader, List classPaths) {
    Method m;
    try {
      m = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
      m.setAccessible(true);
      for (Iterator iter = classPaths.iterator(); iter.hasNext();) {
        m.invoke(loader, new Object[] { new URL("file", "", (String) iter.next()) });
      }
      return true;
    } catch (SecurityException e) {
      return false;
    } catch (NoSuchMethodException e) {
      return false;
    } catch (IllegalArgumentException e) {
      return false;
    } catch (MalformedURLException e) {
      return false;
    } catch (IllegalAccessException e) {
      return false;
    } catch (InvocationTargetException e) {
      return false;
    }
  }

  private static String appendPath(String value, String path) {
    if (value == null) { return path; }
    if (value.endsWith(File.separator)) { return value + path; }
    return value + File.separator + path;
  }

}
