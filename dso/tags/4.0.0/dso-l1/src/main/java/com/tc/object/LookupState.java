/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

enum LookupState implements LookupStateTransition {

  UNINITALIZED {

    @Override
    public LookupState makeLookupRequest() {
      return LOOKUP_REQUEST;
    }

    @Override
    public LookupState makePrefetchRequest() {
      return PREFETCH_REQUEST;
    }
  },

  LOOKUP_REQUEST {

    @Override
    public LookupState makeMissingObject() {
      return MISSING_OBJECT_ID;
    }

    @Override
    public LookupState makePending() {
      return PENDING_LOOKUP;
    }

  },

  PREFETCH_REQUEST {

    @Override
    public boolean isPrefetch() {
      return true;
    }

    @Override
    public LookupState makeLookupRequest() {
      return LOOKUP_REQUEST;
    }

    @Override
    public LookupState makePending() {
      return PENDING_PREFETCH;
    }
  },

  PENDING_LOOKUP {

    @Override
    public boolean isPending() {
      return true;
    }

    @Override
    public LookupState makeUnPending() {
      return LOOKUP_REQUEST;
    }

    @Override
    public LookupState makeMissingObject() {
      return MISSING_OBJECT_ID;
    }
  },

  PENDING_PREFETCH {

    @Override
    public boolean isPrefetch() {
      return true;
    }

    @Override
    public LookupState makeLookupRequest() {
      return PENDING_LOOKUP;
    }

    @Override
    public boolean isPending() {
      return true;
    }

    @Override
    public LookupState makeUnPending() {
      return PREFETCH_REQUEST;
    }
  },

  MISSING_OBJECT_ID {
    @Override
    public LookupState makeMissingObject() {
      // DEV-9048: It's currently possible that the responses to prefetch and the client's lookup request both
      // land at the same time on the client. In the case that both are marked as missing objects, it's safe
      // to ignore the transition from MISSING_OBJECT_ID->MISSING_OBJECT_ID since essentially both responses are
      // telling the client the same thing.
      return this;
    }

    @Override
    public boolean isMissing() {
      return true;
    }
  };

  @Override
  public LookupState makeLookupRequest() {
    throw new IllegalStateException("Current State : " + toString() + ". Can't go to " + LOOKUP_REQUEST);
  }

  @Override
  public LookupState makeMissingObject() {
    throw new IllegalStateException("Current State : " + toString() + ". Can't go to " + MISSING_OBJECT_ID);
  }

  @Override
  public LookupState makePrefetchRequest() {
    throw new IllegalStateException("Current State : " + toString() + ". Can't go to " + PREFETCH_REQUEST);
  }

  @Override
  public LookupState makePending() {
    throw new IllegalStateException("Current State : " + toString() + ". Can't go to " + PENDING_LOOKUP + " or "
                                    + PENDING_PREFETCH);
  }

  @Override
  public LookupState makeUnPending() {
    throw new IllegalStateException("Current State : " + toString() + ". Can't go to " + LOOKUP_REQUEST + " or "
                                    + PREFETCH_REQUEST);
  }

  @Override
  public boolean isPrefetch() {
    return false;
  }

  @Override
  public boolean isMissing() {
    return false;
  }

  @Override
  public boolean isPending() {
    return false;
  }
}