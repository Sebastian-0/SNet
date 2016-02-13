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
public class SynchronizedQueue<E> implements Queue<E>
{
  private volatile Queue<E> queue_;
  
  protected volatile Object lock_;
  
  /**
   * Creates a new {@code SynchronizedQueue} and links it to the specified queue.
   * @param queue The queue to link this queue with
   * @throws NullPointerException If the specified queue is null
   */
  public SynchronizedQueue(Queue<E> queue)
  {
    if (queue == null)
      throw new NullPointerException("The specified queue is null!");
    
    queue_ = queue;
    lock_  = new Object();
  }
  

  @Override
  public int size()
  {
    synchronized (lock_) { return queue_.size(); } 
  }

  @Override
  public boolean isEmpty()
  {
    synchronized (lock_) { return queue_.isEmpty(); }
  }

  @Override
  public boolean contains(Object o)
  {
    synchronized (lock_) { return queue_.contains(o); }
  }

  @Override
  public Iterator<E> iterator()
  {
    synchronized (lock_) { return queue_.iterator(); }
  }

  @Override
  public Object[] toArray()
  {
    synchronized (lock_) { return queue_.toArray(); }
  }

  @Override
  public <T> T[] toArray(T[] a)
  {
    synchronized (lock_) { return queue_.toArray(a); }
  }

  @Override
  public boolean remove(Object o)
  {
    synchronized (lock_) { return queue_.remove(0); }
  }

  @Override
  public boolean containsAll(Collection<?> c)
  {
    synchronized (lock_) { return queue_.containsAll(c); }
  }

  @Override
  public boolean addAll(Collection<? extends E> c)
  {
    synchronized (lock_) { return queue_.addAll(c); }
  }

  @Override
  public boolean removeAll(Collection<?> c)
  {
    synchronized (lock_) { return queue_.removeAll(c); }
  }

  @Override
  public boolean retainAll(Collection<?> c)
  {
    synchronized (lock_) { return queue_.retainAll(c); }
  }

  @Override
  public void clear()
  {
    synchronized (lock_) { queue_.clear(); }
  }

  @Override
  public boolean add(E e)
  {
    synchronized (lock_) { return queue_.add(e); }
  }

  @Override
  public boolean offer(E e)
  {
    synchronized (lock_) { return queue_.offer(e); }
  }

  @Override
  public E remove()
  {
    synchronized (lock_) { return queue_.remove(); }
  }

  @Override
  public E poll()
  {
    synchronized (lock_) { return queue_.poll(); }
  }

  @Override
  public E element()
  {
    synchronized (lock_) { return queue_.element(); }
  }

  @Override
  public E peek()
  {
    synchronized (lock_) { return queue_.peek(); }
  }
}