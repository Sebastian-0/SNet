/*
 * Copyright (C) 2016 Sebastian Hjelm
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package network.internal;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A class that works common super class for both server and client networking.
 * @author Sebastian Hjelm
 */
public abstract class AbstractConnection
{
  /**
   * The message that is sent from the server and client before any communication
   *  can start.
   */
  public static final String HELLO_MESSAGE   = "HelloHello";
  /**
   * The message that is sent from the server and client before closing the
   *  connection.
   */
  public static final String EXIT_MESSAGE   = "ByeBye";
  /**
   * The message that is sent from the server/client to calculate the connection
   *  latency.
   */
  public static final String PING_REQUEST_MESSAGE = "\u2504Ping";
  /**
   * The message that is sent from the server/client after they have received a
   *  ping request.
   */
  public static final String PING_ANSWER_MESSAGE = "\u2504Pong";
  /**
   * The character that separates all the messages in the data stream.
   */
  public static final char   SEPARATOR_CHAR = '\u2502';

  /**
   * The delay between server/client read/write refresh, in milliseconds.
   */
  public static final int    REFRESH_DELAY = 10;

  /**
   * The maximum ping latency that is allowed, in milliseconds.
   */
  public static final int    MAXIMUM_LATENCY = 2000;
  /**
   * The maximum amount of ping retries that are carried out before the connection
   *  is deemed as timed out.
   */
  public static final int    MAXIMUM_RETRIES = 10000; // TODO AbstractConnector; Återställ konstanten till 10
  
  
  protected volatile ConnectionManagerInterface manager_;

  protected volatile Socket socket_;
  
  protected volatile Reader reader_;
  protected volatile Writer writer_;

  protected volatile boolean isClosing_;
  protected volatile boolean isConnected_;
  
  
  protected volatile Queue<String> messages_;
  
  protected volatile Lock lock;
  protected volatile Condition condition;
  
  private StringBuilder builder_;
  private char[]        chars_;
  
  protected boolean hasBeenGreeted_;
  
  private int     pingInterval_;
  private int     currentLatency_;
  
  private long    lastPing_;
  private int     lastPingNumber_;
  private String  pingMessage_;
  
  private boolean hasRecievedAnswer_;
  private int     retries_;
  
  
  /**
   * Initializes the data of the {@code AbstractConnector} and connects it with
   *  the specified connection manager.
   * @param manager The connection manager to connect this connection with
   */
  public AbstractConnection(ConnectionManagerInterface manager)
  {
    manager_ = manager;
    
    messages_ = new SynchronizedQueue<String>(new LinkedList<String>());
    
    builder_ = new StringBuilder();
    chars_   = new char[512];
    
    hasRecievedAnswer_ = true;
    pingInterval_ = 500;
    
    lock = new ReentrantLock();
    condition = lock.newCondition();
  }
  
  
  /**
   * Returns the current ping latency of this connector. This value will be zero
   *  before the first ping has been sent.
   * @return The current ping latency of this connector
   */
  public int getCurrentLatency()
  {
    return currentLatency_;
  }
  
  
  /**
   * Returns true if the connection to the server was established.
   * @return True if the connection to the server was established
   */
  public boolean isConnected()
  {
    return isConnected_;
  }
  
  
  /**
   * Initiates the disconnect-sequence. This connection will send the exit
   *  message to the server and wait for the response. When the response has
   *  arrived the connection closes.
   * </br>
   * </br><b>Note:</b> {@link ConnectionManagerInterface#disconnected(byte, String) disconnected(byte, String)}
   *  of the connection manager connected with this connection will be invoked
   *  when the response arrives.
   */
  public void stop()
  {
    send(EXIT_MESSAGE);
    isClosing_ = true;
  }
  
  
  /**
   * Sends the specified message to the server
   * @param message The message to send
   */
  public void send(String message)
  {
    if (!isClosing_)
    {
      messages_.offer(SEPARATOR_CHAR + message + SEPARATOR_CHAR);
      lock.lock();
      condition.signalAll();
      lock.unlock();
    }
  }
  
  
  protected void sendHello()
  {
    writeString(SEPARATOR_CHAR + HELLO_MESSAGE + SEPARATOR_CHAR);
    try
    {
      writer_.flush();
    } catch (IOException e) { }
  }
  
  
  /**
   * A method that reads all available messages and dispatches them.
   */
  protected void read()
  {
    // Read
    int amount = readChars(chars_);
    if (amount > 0)
    {
      builder_.append(chars_, 0, amount);
      
      while (readReady())
      {
        amount = readChars(chars_);
        builder_.append(chars_, 0, amount);
      }
      
      String receivedString = builder_.toString();
      builder_.setLength(0);
      
      int startIndex = receivedString.indexOf(SEPARATOR_CHAR);
      int endIndex = receivedString.indexOf(SEPARATOR_CHAR, startIndex+1);
      while (startIndex != -1 && endIndex != -1)
      {
        String message = receivedString.substring(startIndex+1, endIndex);
        if (message.length() > 0)
          dispatchMessage(message);

        startIndex = receivedString.indexOf(SEPARATOR_CHAR, endIndex+1);
        endIndex = receivedString.indexOf(SEPARATOR_CHAR, startIndex+1);
      }
      
      if (startIndex != -1) // We have a message that didn't fully arrive
      {
        builder_.append(receivedString.substring(startIndex, receivedString.length()));
      }
    }
  }

  private String dispatchMessage(String msg)
  {
    msg = msg.trim();
    
    if (msg.equals(HELLO_MESSAGE))
    {
      hasBeenGreeted_ = true;
      lock.lock();
      condition.signalAll();
      lock.unlock();
      manager_.connected(this);
    }
    else if (msg.equals(EXIT_MESSAGE))
    {
      if (isClosing_)
      {
        manager_.disconnected(this, ConnectionManagerInterface.REASON_CLOSED, null);
        isConnected_ = false;
      }
      else
      {
        send(EXIT_MESSAGE);
        manager_.disconnected(this, ConnectionManagerInterface.REASON_CLOSED, null);
        isConnected_ = false;
      }
    }
    else if (msg.startsWith(PING_REQUEST_MESSAGE))
    {
      send(PING_ANSWER_MESSAGE + msg.substring(PING_REQUEST_MESSAGE.length()));
    }
    else if (msg.startsWith(PING_ANSWER_MESSAGE))
    {
      if (msg.substring(PING_ANSWER_MESSAGE.length()).equals(pingMessage_)) // ping arrived
      {
        currentLatency_ = (int)(System.currentTimeMillis() - lastPing_);
        retries_ = 0;
        hasRecievedAnswer_ = true;
      }
    }
    else if (!msg.equals("")) // Dispatch
    {
      manager_.receivedMessage(Message.createMessage(this, msg));
    }
    return msg;
  }
  
  
  /**
   * A method that writes all available messages and dispatches them.
   */
  protected void write()
  {
    int s = messages_.size();
    int c = 0;
    while (!messages_.isEmpty() && c++ < s)
      writeString(messages_.poll());
    
    try {
      writer_.flush();
    } catch (IOException e) { }
  }
  
  
  protected void attemptPing()
  {
    if (hasRecievedAnswer_)
    {
      if (System.currentTimeMillis() - lastPing_ > pingInterval_)
      {
        sendPing();
      }
    }
    else if (System.currentTimeMillis() - lastPing_ > MAXIMUM_LATENCY)
    {
      if (retries_++ >= MAXIMUM_RETRIES)
      {
        // Lost connection
        manager_.disconnected(this, ConnectionManagerInterface.REASON_TIMEOUT, null);
        isConnected_ = false;
      }
      else
      {
        sendPing();
      }
    }
  }

  private void sendPing()
  {
    lastPingNumber_ += 1;
    if (lastPingNumber_ > 99)
      lastPingNumber_ = 0;

    lastPing_ = System.currentTimeMillis();
    pingMessage_ = "" + lastPingNumber_;
    send(PING_REQUEST_MESSAGE + pingMessage_);
    hasRecievedAnswer_ = false;
  }


  private boolean readReady()
  {
    try {
      return reader_.ready();
    } catch (IOException e) { }
    
    return false;
  }
  
  private int readChars(char[] chars)
  {
    try {
      int amountRead = reader_.read(chars);
      if (amountRead == -1)
        lostConnection();
      return amountRead;
    } catch (IOException e) {
      lostConnection();
    }
    
    return 0;
  }
  
  private void writeString(String text)
  {
    try {
      writer_.write(text);
    } catch (IOException e) { }
  }
  
  protected void lostConnection()
  {
    if (isConnected_)
    {
      manager_.disconnected(this, ConnectionManagerInterface.REASON_LOST_CONNECTION, null);
      isClosing_ = true;
      isConnected_ = false;
    }
  }
}
