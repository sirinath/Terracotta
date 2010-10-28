/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.search.SearchQueryResult;
import com.tc.search.SearchQueryResults;

import java.util.List;

public class SearchQueryResultsImpl implements SearchQueryResults {

  private final List<SearchQueryResult> results;
  private final List<Integer>           aggregatorResults;

  public SearchQueryResultsImpl(List<SearchQueryResult> kResults, List<Integer> aResults) {
    results = kResults;
    aggregatorResults = aResults;
  }

  public List<Integer> getAggregatorResults() {
    return aggregatorResults;
  }

  public List<SearchQueryResult> getResults() {
    return results;
  }

}
