/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.util.diff;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.tc.util.Assert;
import com.tc.util.StandardStringifier;
import com.tc.util.Stringifier;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Passed along among {@link Differenceable}objects in order to display where the differences are in an object tree.
 */
public class DifferenceContext {

  private final DifferenceContext previous;
  private final String            thisContext;
  private final List              differences;
  private final Stringifier       stringifier;

  private DifferenceContext(DifferenceContext previous, String thisContext) {
    Assert.assertNotNull(previous);
    Assert.assertNotBlank(thisContext);

    this.previous = previous;
    this.thisContext = thisContext;
    this.differences = this.previous.differences;
    this.stringifier = this.previous.stringifier;
  }

  public DifferenceContext(Stringifier stringifier) {
    Assert.assertNotNull(stringifier);

    this.previous = null;
    this.thisContext = "";
    this.differences = new LinkedList();
    this.stringifier = stringifier;
  }

  public static DifferenceContext createInitial() {
    return createInitial(StandardStringifier.INSTANCE);
  }

  public static DifferenceContext createInitial(Stringifier stringifier) {
    return new DifferenceContext(stringifier);
  }

  public DifferenceContext sub(String context) {
    return new DifferenceContext(this, context);
  }

  /**
   * For <strong>TESTS ONLY </strong>.
   */
  Collection collection() {
    return this.differences;
  }

  Stringifier stringifier() {
    return this.stringifier;
  }

  String describe(Object o) {
    return this.stringifier.toString(o);
  }

  void addDifference(Difference difference) {
    Assert.assertNotNull(difference);
    Assert.eval(difference.where() == this);
    this.differences.add(difference);
  }

  Iterator getDifferences() {
    return this.differences.iterator();
  }

  boolean hasDifferences() {
    return this.differences.size() > 0;
  }

  /**
   * For <strong>TESTS ONLY </strong>.
   */
  int countDifferences() {
    return this.differences.size();
  }

  @Override
  public String toString() {
    if (this.previous != null) return this.previous.toString() + "/" + this.thisContext;
    else return this.thisContext;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((differences == null) ? 0 : differences.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object that) {
    if (!this.rawEquals(that)) return false;

    return new EqualsBuilder().append(this.differences, ((DifferenceContext) that).differences).isEquals();
  }

  boolean rawEquals(Object that) {
    if (!(that instanceof DifferenceContext)) return false;

    DifferenceContext diffThat = (DifferenceContext) that;

    if ((this.previous == null) != (diffThat.previous == null)) return false;
    if (this.previous != null && (!this.previous.rawEquals(diffThat.previous))) return false;

    return new EqualsBuilder().append(this.thisContext, diffThat.thisContext).isEquals();
  }

}