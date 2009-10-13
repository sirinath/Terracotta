/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.util.SinglyLinkedList;

import java.util.Iterator;

public class SynchronizedSinglyLinkedList<E extends SinglyLinkedList.LinkedNode<E>> extends SinglyLinkedList<E> {
  @Override
  public synchronized boolean isEmpty() {
    return super.isEmpty();
  }

  @Override
  public synchronized void addFirst(E first) {
    super.addFirst(first);
  }
  
  @Override
  public synchronized E removeFirst() {
    return super.removeFirst();
  }

  @Override
  public synchronized E getFirst() {
    return super.getFirst();
  }

  @Override
  public synchronized void addLast(E last) {
    super.addLast(last);
  }

  @Override
  public synchronized E removeLast() {
    return super.removeLast();
  }

  
  @Override
  public synchronized E getLast() {
    return super.getLast();
  }

  @Override
  public synchronized E remove(E obj) {
    return super.remove(obj);
  }

  public <T extends E> Iterable<T> iterableOf(Class<T> clazz) {
    return new FilteredIterator<T>(clazz, this);
  }
  
  public <T extends E> Iterator<T> iteratorOf(Class<T> clazz) {
    return new FilteredIterator<T>(clazz, this);
  }
  
  static class FilteredIterator<T> implements Iterator, Iterable {

    private final Iterator<? super T> iterator;
    private final Class<T>            clazz;

    private T                         next;
    
    public FilteredIterator(Class<T> clazz, Iterable<? super T> iterable) {
      this.iterator = iterable.iterator();
      this.clazz = clazz;
      
      this.next = getNext();
    }
    
    public boolean hasNext() {
      return (next != null);
    }

    public Object next() {
      Object current = next;
      next = getNext();
      return current;
    }

    public void remove() {
      iterator.remove();
    }

    private T getNext() {
      while (iterator.hasNext()) {
        Object current = iterator.next();
        if (clazz.isInstance(current)) {
          return (T) current;
        }
      }
      return null;
    }

    public Iterator iterator() {
      return this;
    }
  }
}
