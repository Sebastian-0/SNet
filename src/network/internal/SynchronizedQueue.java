/*
 * Copyright (C) 2016 Sebastian Hjelm
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package network.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

/**
 * This class acts as a wrapper class for queues to make them synchronized.
 * @author Sebastian Hjelm
 *
 * @param <E> The type parameter for this queue
 */
public class SynchronizedQueue<E> implements Queue<E> {
  private volatile Queue<E> queue;
  
  protected volatile Object lock;
  
  /**
   * Creates a new {@code SynchronizedQueue} and links it to the specified queue.
   * @param queue The queue to link this queue with
   * @throws NullPointerException If the specified queue is null
   */
  public SynchronizedQueue(Queue<E> queue) {
    if (queue == null)
      throw new NullPointerException("The specified queue is null!");
    
    this.queue = queue;
    lock  = new Object();
  }
  

  @Override
  public int size() {
    synchronized (lock) { return queue.size(); } 
  }

  @Override
  public boolean isEmpty() {
    synchronized (lock) { return queue.isEmpty(); }
  }

  @Override
  public boolean contains(Object o) {
    synchronized (lock) { return queue.contains(o); }
  }

  @Override
  public Iterator<E> iterator() {
    synchronized (lock) { return queue.iterator(); }
  }

  @Override
  public Object[] toArray() {
    synchronized (lock) { return queue.toArray(); }
  }

  @Override
  public <T> T[] toArray(T[] a) {
    synchronized (lock) { return queue.toArray(a); }
  }

  @Override
  public boolean remove(Object o) {
    synchronized (lock) { return queue.remove(0); }
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    synchronized (lock) { return queue.containsAll(c); }
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    synchronized (lock) { return queue.addAll(c); }
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    synchronized (lock) { return queue.removeAll(c); }
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    synchronized (lock) { return queue.retainAll(c); }
  }

  @Override
  public void clear() {
    synchronized (lock) { queue.clear(); }
  }

  @Override
  public boolean add(E e) {
    synchronized (lock) { return queue.add(e); }
  }

  @Override
  public boolean offer(E e) {
    synchronized (lock) { return queue.offer(e); }
  }

  @Override
  public E remove() {
    synchronized (lock) { return queue.remove(); }
  }

  @Override
  public E poll() {
    synchronized (lock) { return queue.poll(); }
  }

  @Override
  public E element() {
    synchronized (lock) { return queue.element(); }
  }

  @Override
  public E peek() {
    synchronized (lock) { return queue.peek(); }
  }
}