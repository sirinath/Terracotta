/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tctest.modes.TestMode;
import com.tctest.modes.TestMode.Mode;

import java.util.ArrayList;
import java.util.List;

public class TestModesHandler {
  private final ArrayList<TestMode> normalRuns               = new ArrayList<TestMode>();
  private final ArrayList<TestMode> crashRuns                = new ArrayList<TestMode>();
  private final ArrayList<TestMode> activePassiveRuns        = new ArrayList<TestMode>();
  private final ArrayList<TestMode> activeActiveRuns         = new ArrayList<TestMode>();

  private int                       indexOfNormalRuns        = 0;
  private int                       indexOfCrashRuns         = 0;
  private int                       indexOfActivePassiveRuns = 0;
  private int                       indexOfActiveActiveRuns  = 0;

  public TestModesHandler(TestMode[] modes) {
    for (TestMode mode : modes) {
      switch (mode.getMode()) {
        case ACTIVE_ACTIVE:
          activeActiveRuns.add(mode);
          break;
        case ACTIVE_PASSIVE:
          activePassiveRuns.add(mode);
          break;
        case CRASH:
          crashRuns.add(mode);
          break;
        case NORMAL:
          normalRuns.add(mode);
          break;
      }
    }
  }

  public int getIndexFor(String strMode) {
    Mode mode = Mode.fromString(strMode);
    switch (mode) {
      case ACTIVE_ACTIVE:
        return indexOfActiveActiveRuns;
      case ACTIVE_PASSIVE:
        return indexOfActivePassiveRuns;
      case CRASH:
        return indexOfCrashRuns;
      case NORMAL:
        return indexOfNormalRuns;
    }
    throw new AssertionError("No index found");
  }

  public boolean hasMoreRuns(List<String> modes) {
    for (String strMode : modes) {
      if (canRunMode(strMode)) { return true; }
    }

    return false;
  }

  private boolean canRunMode(String strMode) {
    Mode mode = Mode.fromString(strMode);
    switch (mode) {
      case ACTIVE_ACTIVE:
        if (activeActiveRuns.size() <= indexOfActiveActiveRuns) { return false; }
        break;
      case ACTIVE_PASSIVE:
        if (activePassiveRuns.size() <= indexOfActivePassiveRuns) { return false; }
        break;
      case CRASH:
        if (crashRuns.size() <= indexOfCrashRuns) { return false; }
        break;
      case NORMAL:
        if (normalRuns.size() <= indexOfNormalRuns) { return false; }
        break;
    }
    return true;
  }

  public TestMode getTestModeFor(String strMode) {
    Mode mode = Mode.fromString(strMode);
    TestMode testMode = null;
    if (!canRunMode(strMode)) { return null; }

    switch (mode) {
      case ACTIVE_ACTIVE:
        testMode = activeActiveRuns.get(indexOfActiveActiveRuns);
        indexOfActiveActiveRuns++;
        break;
      case ACTIVE_PASSIVE:
        testMode = activePassiveRuns.get(indexOfActivePassiveRuns);
        indexOfActivePassiveRuns++;
        break;
      case CRASH:
        testMode = crashRuns.get(indexOfCrashRuns);
        indexOfCrashRuns++;
        break;
      case NORMAL:
        testMode = normalRuns.get(indexOfNormalRuns);
        indexOfNormalRuns++;
        break;
    }

    return testMode;
  }
}