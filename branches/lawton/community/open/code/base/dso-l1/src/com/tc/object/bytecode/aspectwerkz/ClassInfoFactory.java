/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.bytecode.aspectwerkz;

import com.tc.aspectwerkz.reflect.ClassInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class ClassInfoFactory {
    private final Map classInfoCache = new HashMap();

    public void setClassInfo(ClassInfo classInfo) {
        synchronized (classInfoCache) {
            classInfoCache.put(classInfo.getName(), classInfo);
        }
    }

    public ClassInfo getClassInfo(String className) {
        ClassInfo info;
        synchronized (classInfoCache) {
            info = (ClassInfo) classInfoCache.get(className);
            if (info == null) {
                info = new SimpleClassInfo(className);
                classInfoCache.put(className, info);
            }
        }
        return info;
    }
}
