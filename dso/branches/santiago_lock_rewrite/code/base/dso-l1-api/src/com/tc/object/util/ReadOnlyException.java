/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.util;

import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;

/**
 * Indicates a read-only transaction is trying to access a shared object.  This is most likely 
 * a problem with an incorrect lock configuration.
 */
public class ReadOnlyException extends RuntimeException {
  
  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();
  
  /** Indicates a default invalid VM_ID to use */
  public static final long INVALID_VMID = -1;
  
  /**
   * @param message Message, which will be wrapped
   */
  protected ReadOnlyException(String message) {
    super(wrapper.wrap(message));
  }
  
  /**
   * @param message Message
   * @param threadName Thread name
   * @param vmId VM identifier
   */
  public ReadOnlyException(String threadName, long vmId) {
    this(ReadOnlyException.createDisplayableString(READ_ONLY_TEXT, threadName, vmId));
  }
  
  /**
   * @param message Message
   * @param threadName Thread name
   * @param vmId VM identifier
   * @param details Additional details
   */
  public ReadOnlyException(String threadName, long vmId, String details) {
    this(ReadOnlyException.createDisplayableString(READ_ONLY_TEXT, threadName, vmId) + "\n    " + details);
  }
  
  private static String createDisplayableString(String message, String threadName, long vmId) {
    if (vmId == INVALID_VMID) {
      return message + "\n\n    Caused by Thread: " + threadName;
    }
    return message + "\n\n    Caused by Thread: " + threadName + "  in  VM(" + vmId + ")";
  }
  
  private static final String READ_ONLY_TEXT = "Attempt to write to a shared object inside the scope of a lock declared as a"
    + "\nread lock. All writes to shared objects must be within the scope of one or"
    + "\nmore shared locks with write access defined in your Terracotta configuration."
    + "\n\nPlease alter the locks section of your Terracotta configuration so that this"
    + "\naccess is auto-locked or protected by a named lock with write access."
    + "\n\nFor more information on this issue, please visit our Troubleshooting Guide at:"
    + "\nhttp://terracotta.org/kit/troubleshooting ";
}