package org.terracotta.lassen.util;

import java.util.UUID;

public abstract class SecurityHelper {
  public static String generateUniqueCode() {
    final UUID uuid = UUID.randomUUID();
    final String uuidString = uuid.toString().replaceAll("\\W", ""); // strip away non word chars for easy copy/paste
    return uuidString;
  }
}
