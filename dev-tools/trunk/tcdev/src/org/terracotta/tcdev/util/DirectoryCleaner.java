package org.terracotta.tcdev.util;

import java.io.File;

public final class DirectoryCleaner {

  public static void cleanDirectory(final File directoryToClean) {
    final File[] files = directoryToClean.listFiles();
    for (int pos = 0; pos < files.length; ++pos) {
      final File fileToRemove = files[pos];
      if (fileToRemove.isDirectory()) {
        cleanDirectory(fileToRemove);
      }
      fileToRemove.delete();
    }
  }

}
