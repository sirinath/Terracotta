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

import com.tc.exception.ImplementMe;
import com.tc.test.TCTestCase;

/**
 * Unit test for {@link Difference}.
 */
public class DifferenceTest extends TCTestCase {

  // This just is to get around the fact that Difference is abstract.
  private static class TestDifference extends Difference {
    public TestDifference(DifferenceContext where) {
      super(where);
    }

    @Override
    public Object a() {
      throw new ImplementMe();
    }

    @Override
    public Object b() {
      throw new ImplementMe();
    }

    @Override
    public String toString() {
      throw new ImplementMe();
    }
  }

  public void testConstruction() throws Exception {
    try {
      new TestDifference(null);
      fail("Didn't get NPE on no context");
    } catch (NullPointerException npe) {
      // ok
    }
  }
  
  public void testWhere() throws Exception {
    DifferenceContext context = DifferenceContext.createInitial().sub("a").sub("b");
    Difference test = new TestDifference(context);
    
    assertSame(context, test.where());
  }
  
  public void testEquals() throws Exception {
    DifferenceContext contextA = DifferenceContext.createInitial().sub("a");
    DifferenceContext contextB = DifferenceContext.createInitial().sub("b");
    
    Difference a = new TestDifference(contextA);
    Difference b = new TestDifference(contextB);
    Difference c = new TestDifference(contextA);
    
    assertEquals(a, c);
    assertEquals(c, a);
    
    assertFalse(a.equals(b));
    assertFalse(b.equals(a));
    assertFalse(c.equals(b));
    assertFalse(b.equals(c));
  }

}