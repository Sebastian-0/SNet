/*
 * Copyright (C) 2016 Sebastian Hjelm
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package network;

import sbasicgui.util.Pool;
import sbasicgui.util.Poolable;

/**
 * A class that describes a server/client message received through a socket. To
 *  create a message, use the {@link #createMessage(AbstractConnector, String)} method.
 *  All the messages are stored within a pool when they are unused to spare the
 *  garbage collector some work.
 * @author Sebastian Hjelm
 */
public class Message implements Poolable
{
  private static volatile Pool<Message> pool_;
  
  /**
   * The socket that sent the message.
   */
  public volatile AbstractConnector receiver;
  /**
   * The text message itself.
   */
  public volatile String message;
  
  private volatile boolean isDisposed_;
  
  
  static
  {
    pool_ = new Pool<Message>(Message.class);
    pool_.allocate(10);
  }
  
  
  /**
   * Do <i>not</i> use this constructor! To create a message, use
   *  {@link #createMessage(AbstractConnector, String)}!
   */
  public Message()
  {
  }
  
  
  /**
   * Creates and returns a message containing the specified text message.
   * @param message The text message
   * @return A message connected with {@code sender}, containing {@code message}
   */
  public static Message createMessage(String message)
  {
    return createMessage(null, message);
  }
  /**
   * Creates and returns a message connected to the specified receiver and containing
   *  the specified text message.
   * @param sender The connection that received the message
   * @param message The text message
   * @return A message connected with {@code sender}, containing {@code message}
   */
  public static Message createMessage(AbstractConnector sender, String message)
  {
    Message msg = pool_.acquire();
    msg.receiver    = sender;
    msg.message     = message;
    msg.isDisposed_ = false;
    return msg;
  }
  
  
  /**
   * Returns the message text without the leading command code.
   * @return The message text without the command code
   */
  public String extract()
  {
    return message.substring(1);
  }
  
  
  /**
   * Disposes this message and adds it to the message pool. This method should
   *  always be invoked when the message isn't needed anymore.
   */
  public void dispose()
  {
    if (!isDisposed_)
    {
      receiver = null;
      message = null;
      pool_.store(this);
      isDisposed_ = true;
    }
  }
}
