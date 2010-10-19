/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.search.SearchQueryResult;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SearchQueryResultImpl implements SearchQueryResult {

  private final Map<String, String> attributes = new HashMap<String, String>();

  private String                    key;

  public SearchQueryResultImpl() {
    // do nothing
  }

  public SearchQueryResultImpl(String key, Map<String, String> attributeMap) {
    this.key = key;
    this.attributes.putAll(attributeMap);
  }

  public Map<String, String> getAttributes() {
    return Collections.unmodifiableMap(this.attributes);
  }

  public String getKey() {
    return this.key;
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) {
    try {
      this.key = serialInput.readString();
      int mapSize = serialInput.readInt();
      for (int i = 0; i < mapSize; i++) {
        String attrKey = serialInput.readString();
        String attrValue = serialInput.readString();
        this.attributes.put(attrKey, attrValue);
      }
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeString(this.key);
    serialOutput.writeInt(this.attributes.size());
    for (Map.Entry<String, String> entry : this.attributes.entrySet()) {
      serialOutput.writeString(entry.getKey());
      serialOutput.writeString(entry.getValue());
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SearchQueryResultImpl other = (SearchQueryResultImpl) obj;
    if (key == null) {
      if (other.key != null) return false;
    } else if (!key.equals(other.key)) return false;
    return true;
  }

  @Override
  public String toString() {
    return "SearchQueryResultImpl [attributes=" + attributes + ", key=" + key + "]";
  }

}
