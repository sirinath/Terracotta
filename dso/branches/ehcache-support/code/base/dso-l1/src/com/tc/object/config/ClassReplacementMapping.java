/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ClassReplacementMapping {
  private Map classNamesMapping = new HashMap();
  
  private Map classNamesSlashesReverseMapping = new HashMap();
  private Map asmTypesReverseMapping = new HashMap();
  
  public synchronized String addMapping(String originalClassName, String replacementClassName) {
    if (null == originalClassName ||
        0 == originalClassName.length() ||
        null == replacementClassName ||
        0 == replacementClassName.length()) {
      return null;
    }
    
    if (classNamesMapping.containsKey(originalClassName)) {
      return null;
    }
   
    String previous = (String)classNamesMapping.put(originalClassName, replacementClassName);
    
    String originalClassNameSlashes = originalClassName.replace('.', '/');
    String replacementClassNameSlashes = replacementClassName.replace('.', '/');
    classNamesSlashesReverseMapping.put(replacementClassNameSlashes, originalClassNameSlashes);
    asmTypesReverseMapping.put(ensureAsmType(replacementClassNameSlashes), ensureAsmType(originalClassNameSlashes));
    
    return previous;
  }
  
  public boolean hasReplacement(String originalClassName) {
    return classNamesMapping.containsKey(originalClassName);
  }

  private String ensureAsmType(String classNameSlashes) {
    if (null == classNameSlashes || 0 == classNameSlashes.length()) {
      return classNameSlashes;
    }
    
    if (classNameSlashes.charAt(0) != 'L' &&
        classNameSlashes.charAt(classNameSlashes.length()-1) != ';') {
      classNameSlashes = 'L' + classNameSlashes + ';';
    }
    return classNameSlashes;
  }
  
  public synchronized String getReplacementClassName(String original) {
    return (String)classNamesMapping.get(original); 
  }
  
  public synchronized String getOriginalClassNameSlashes(String replacement) {
    String original = (String)classNamesSlashesReverseMapping.get(replacement); 
    if (null == original) {
      original = replacement;
    }
    return original;
  }
  
  public synchronized String getOriginalAsmType(String replacement) {
    String original = (String)asmTypesReverseMapping.get(replacement); 
    if (null == original) {
      original = replacement;
    }
    return original;
  }

  public synchronized String ensureOriginalAsmTypes(String s) {
    if (s != null) {
      Iterator it = asmTypesReverseMapping.entrySet().iterator();
      Map.Entry entry;
      String original;
      String replacement;
      while (it.hasNext()) {
        entry = (Map.Entry)it.next();
        original = (String)entry.getKey();
        replacement = (String)entry.getValue();
        
        if (s.indexOf(original) != -1) {
          s = s.replaceAll(original, replacement);
        }
      }
    }
    return s;
  }
}