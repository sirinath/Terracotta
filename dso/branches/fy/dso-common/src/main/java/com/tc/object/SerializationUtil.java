/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Some utility stuff for logical serialization
 */
public class SerializationUtil {

  // NOTE: DO NOT USE VALUE 0. A zero indicates a mapping that does exist
  public final static int            ADD                                      = 1;
  public final static int            ADD_AT                                   = 2;
  public final static int            PUT                                      = 5;
  public final static int            CLEAR                                    = 6;
  public final static int            REMOVE                                   = 7;
  public final static int            REMOVE_AT                                = 8;
  public final static int            SET                                      = 9;
  public final static int            REMOVE_RANGE                             = 16;
  public final static int            REPLACE_IF_VALUE_EQUAL                   = 25;
  public final static int            PUT_IF_ABSENT                            = 26;
  public final static int            REMOVE_IF_VALUE_EQUAL                    = 27;
  public final static int            CLEAR_LOCAL_CACHE                        = 29;
  public final static int            EVICTION_COMPLETED                       = 30;
  public final static int            CLUSTERED_NOTIFIER                       = 31;
  public final static int            DESTROY                                  = 32;
  public final static int            FIELD_CHANGED                            = 33;
  public final static int            INT_FIELD_CHANGED                        = 34;
  public final static int            SET_LAST_ACCESSED_TIME                   = 35;
  public final static int            EXPIRE_IF_VALUE_EQUAL                    = 36;
  public final static int            PUT_VERSIONED                            = 37;
  public final static int            REMOVE_VERSIONED                         = 38;
  public final static int            PUT_IF_ABSENT_VERSIONED                  = 39;
  public final static int            CLEAR_VERSIONED                          = 40;
  public final static int            REGISTER_SERVER_EVENT_LISTENER           = 41;
  public final static int            UNREGISTER_SERVER_EVENT_LISTENER           = 42;
  public final static int            REGISTER_SERVER_EVENT_LISTENER_PASSIVE     = 43;
  public final static int            UNREGISTER_SERVER_EVENT_LISTENER_PASSIVE   = 44;
  public final static int            REMOVE_EVENT_LISTENING_CLIENT        = 45;
  public final static int            NO_OP                                    = 46;

  public final static String         ADD_AT_SIGNATURE                         = "add(ILjava/lang/Object;)V";
  public final static String         ADD_ALL_AT_SIGNATURE                     = "addAll(ILjava/util/Collection;)Z";
  public final static String         ADD_SIGNATURE                            = "add(Ljava/lang/Object;)Z";
  public final static String         ADD_ALL_SIGNATURE                        = "addAll(Ljava/util/Collection;)Z";
  public final static String         CLEAR_SIGNATURE                          = "clear()V";
  public final static String         CLEAR_LOCAL_CACHE_SIGNATURE              = "clearLocalCache()V";
  public final static String         PUT_SIGNATURE                            = "put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
  public final static String         SET_SIGNATURE                            = "set(ILjava/lang/Object;)Ljava/lang/Object;";
  public final static String         REMOVE_AT_SIGNATURE                      = "remove(I)Ljava/lang/Object;";
  public final static String         REMOVE_SIGNATURE                         = "remove(Ljava/lang/Object;)Z";
  public final static String         REMOVE_KEY_SIGNATURE                     = "remove(Ljava/lang/Object;)Ljava/lang/Object;";
  public final static String         REMOVE_RANGE_SIGNATURE                   = "removeRange(II)V";
  public final static String         PUT_IF_ABSENT_SIGNATURE                  = "putIfAbsent(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
  public final static String         REMOVE_IF_VALUE_EQUAL_SIGNATURE          = "remove(Ljava/lang/Object;Ljava/lang/Object;)Z";
  public final static String         REPLACE_IF_VALUE_EQUAL_SIGNATURE         = "replace(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z";
  public final static String         CLUSTERED_NOTIFIER_SIGNATURE             = "clusteredNotifier()V";
  public final static String         DESTROY_SIGNATURE                        = "destroy()V";
  public final static String         FIELD_CHANGED_SIGNATURE                  = "fieldChanged()V";
  public final static String         INT_FIELD_CHANGED_SIGNATURE              = "intFieldChanged(Ljava/lang/String;I)V";
  public final static String         SET_LAST_ACCESSED_TIME_SIGNATURE         = "setLastAccessedTime(Ljava/lang/Object;Ljava/lang/Object;J)V";
  public final static String         EXPIRE_IF_VALUE_EQUAL_SIGNATURE          = "expireIfValueEqual(Ljava/lang/Object;Ljava/lang/Object;)Z";
  public final static String         PUT_VERSIONED_SIGNATURE                  = "putVersioned(Ljava/lang/Object;Ljava/lang/Object;)Z";
  public final static String         REMOVE_VERSIONED_SIGNATURE               = "removeVersioned(Ljava/lang/Object;Ljava/lang/Object;)Z";
  public final static String         PUT_IF_ABSENT_VERSIONED_SIGNATURE = "putIfAbsentOrOlderVersion(Ljava/lang/Object;Ljava/lang/Object;J)V";
  public final static String         CLEAR_VERSIONED_SIGNATURE            = "clearVersioned()V";
  public final static String         NO_OP_SIGNATURE                      = "()V";
  public final static String         REGISTER_SERVER_EVENT_LISTENER_SIGNATURE = "registerListener(Ljava/util/Set)V";
  public final static String         UNREGISTER_SERVER_EVENT_LISTENER_SIGNATURE = "registerListener(Ljava/util/Set)V";

  private final Map<String, Integer> mappings                          = new HashMap<String, Integer>();

  public SerializationUtil() {
    mappings.put(ADD_AT_SIGNATURE, ADD_AT);
    mappings.put(ADD_ALL_AT_SIGNATURE, ADD_AT);
    mappings.put(ADD_SIGNATURE, ADD);
    mappings.put(ADD_ALL_SIGNATURE, ADD);
    mappings.put(CLEAR_SIGNATURE, CLEAR);
    mappings.put(PUT_SIGNATURE, PUT);
    mappings.put(SET_SIGNATURE, SET);
    mappings.put(REMOVE_AT_SIGNATURE, REMOVE_AT);
    mappings.put(REMOVE_SIGNATURE, REMOVE);
    mappings.put(REMOVE_IF_VALUE_EQUAL_SIGNATURE, REMOVE_IF_VALUE_EQUAL);
    mappings.put(REMOVE_KEY_SIGNATURE, REMOVE);
    mappings.put(REMOVE_RANGE_SIGNATURE, REMOVE_RANGE);
    mappings.put(PUT_IF_ABSENT_SIGNATURE, PUT_IF_ABSENT);
    mappings.put(REPLACE_IF_VALUE_EQUAL_SIGNATURE, REPLACE_IF_VALUE_EQUAL);
    mappings.put(CLEAR_LOCAL_CACHE_SIGNATURE, CLEAR_LOCAL_CACHE);
    mappings.put(CLUSTERED_NOTIFIER_SIGNATURE, CLUSTERED_NOTIFIER);
    mappings.put(DESTROY_SIGNATURE, DESTROY);
    mappings.put(FIELD_CHANGED_SIGNATURE, FIELD_CHANGED);
    mappings.put(INT_FIELD_CHANGED_SIGNATURE, INT_FIELD_CHANGED);
    mappings.put(SET_LAST_ACCESSED_TIME_SIGNATURE, SET_LAST_ACCESSED_TIME);
    mappings.put(EXPIRE_IF_VALUE_EQUAL_SIGNATURE, EXPIRE_IF_VALUE_EQUAL);
    mappings.put(PUT_VERSIONED_SIGNATURE, PUT_VERSIONED);
    mappings.put(REMOVE_VERSIONED_SIGNATURE, REMOVE_VERSIONED);
  }

  public String[] getSignatures() {
    return mappings.keySet().toArray(new String[0]);
  }

  public int methodToID(String name) {
    Integer i = mappings.get(name);
    if (i == null) throw new AssertionError("Illegal method name:" + name);
    return i;
  }

  private static final Pattern PARENT_FIELD_PATTERN = Pattern.compile("^this\\$\\d+$");

  public boolean isParent(String fieldName) {
    return PARENT_FIELD_PATTERN.matcher(fieldName).matches();
  }
}
