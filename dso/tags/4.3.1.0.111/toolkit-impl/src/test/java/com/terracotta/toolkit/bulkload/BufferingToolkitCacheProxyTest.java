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
package com.terracotta.toolkit.bulkload;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.internal.cache.BufferingToolkitCache;
import org.terracotta.toolkit.nonstop.NonStopException;

import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.collections.map.ToolkitCacheImpl;
import com.terracotta.toolkit.nonstop.ToolkitObjectLookup;
import com.terracotta.toolkit.util.AbstractToolkitInstanceProxyTest;
import com.terracotta.toolkit.util.ToolkitInstanceProxy;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author tim
 */
public class BufferingToolkitCacheProxyTest extends AbstractToolkitInstanceProxyTest {
  private ToolkitCacheImpl cache;
  private ToolkitObjectLookup<ToolkitCacheImpl> lookup;
  private BufferingToolkitCache proxiedCache;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    cache = mock(ToolkitCacheImpl.class);
    lookup = lookup(cache);
    proxiedCache = ToolkitInstanceProxy.newNonStopProxy("foo", ToolkitObjectType.CACHE, nonStopContext, BufferingToolkitCache.class, lookup);
  }

  @Test(expected = NonStopException.class)
  public void testStartBufferingTimeout() throws Exception {
    doThrow(new ToolkitAbortableOperationException()).when(cache).startBuffering();
    proxiedCache.startBuffering();
  }

  @Test(expected = NonStopException.class)
  public void testFlushTimeout() throws Exception {
    doThrow(new ToolkitAbortableOperationException()).when(cache).flushBuffer();
    proxiedCache.flushBuffer();
  }

  @Test(expected = NonStopException.class)
  public void testStopBufferingTimeout() throws Exception {
    doThrow(new ToolkitAbortableOperationException()).when(cache).stopBuffering();
    proxiedCache.stopBuffering();
  }

  @Test
  public void testStartBuffering() throws Exception {
    proxiedCache.startBuffering();
    verify(cache).startBuffering();
  }

  @Test
  public void testStopBuffering() throws Exception {
    proxiedCache.stopBuffering();
    verify(cache).stopBuffering();
  }

  @Test
  public void testFlushBuffer() throws Exception {
    proxiedCache.flushBuffer();
    verify(cache).flushBuffer();
  }
}
