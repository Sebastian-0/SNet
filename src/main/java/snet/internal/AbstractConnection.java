/*
 * Copyright (C) 2016 Sebastian Hjelm
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package snet.internal;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import snet.internal.ConnectionManagerInterface.DisconnectReason;
import sutilities.Debugger;

/**
 * A class that works common super class for both server and client networking.
 * @author Sebastian Hjelm
 */
public abstract class AbstractConnection {
  /**
   * The message that is sent from the server and client before any communication
   *  can start.
   */
  public static final String HELLO_MESSAGE = "HelloHello";
  /**
   * The message that is sent from the server and client before closing the
   *  connection.
   */
  public static final String EXIT_MESSAGE = "ByeBye";
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
  public static final char SEPARATOR_CHAR = '\u2502';

  /**
   * The maximum ping latency that is allowed, in milliseconds.
   */
  public static int maximumLatency = 2000;
  /**
   * The maximum amount of ping retries that are carried out before the connection
   *  is deemed as timed out.
   */
  public static int maximumRetries = 10;
  
  /**
   * Defines whether or not the system should print information that might be useful when debugging.
   **/
  public static boolean isInVerboseMode = false;
  
  
  protected volatile ConnectionManagerInterface connectionManager;

  protected volatile Socket socket;
  
  protected volatile Reader reader;
  protected volatile Writer writer;

  protected volatile boolean isClosing;
  protected volatile boolean isConnected;
  
  protected volatile boolean useTcpNoDelay;
  private volatile boolean shouldFlush;
  
  
  protected volatile Queue<String> messages;
  
  protected volatile Lock lock;
  protected volatile Condition condition;
  
  private StringBuilder builder;
  private char[]        chars;
  
  protected boolean hasBeenGreeted;
  
  private int     pingInterval;
  private int     currentLatency;
  
  private long    lastPing;
  private int     lastPingNumber;
  private String  pingMessage;
  
  private boolean hasRecievedAnswer;
  private int     retries;
  
  
  /**
   * Initializes the data of the {@code AbstractConnector} and connects it with
   *  the specified connection manager.
   * @param manager The connection manager to connect this connection with
   * @param useTcpNoDelay Whether or not Nagle's algorithm is disabled for the socket
   */
  public AbstractConnection(ConnectionManagerInterface manager, boolean useTcpNoDelay) {
    this.connectionManager = manager;
    this.useTcpNoDelay = useTcpNoDelay;
    
    messages = new SynchronizedQueue<String>(new LinkedList<String>());
    
    builder = new StringBuilder();
    chars   = new char[512];
    
    hasRecievedAnswer = true;
    pingInterval = 500;
    
    lock = new ReentrantLock();
    condition = lock.newCondition();
  }
  
  
  /**
   * Returns the current ping latency of this connector. This value will be zero
   *  before the first ping has been sent.
   * @return The current ping latency of this connector
   */
  public int getCurrentLatency() {
    return currentLatency;
  }
  
  
  /**
   * Returns true if the connection to the server was established.
   * @return True if the connection to the server was established
   */
  public boolean isConnected() {
    return isConnected;
  }
  
  
  /**
   * Initiates the disconnect-sequence. This connection will send the exit
   *  message to the server and wait for the response. When the response has
   *  arrived the connection closes.
   * </br>
   * </br><b>Note:</b> {@link ConnectionManagerInterface#disconnected(AbstractConnection, DisconnectReason)
   * disconnected(AbstractConnection, DisconnectReason)}
   *  of the connection manager connected with this connection will be invoked
   *  when the response arrives.
   * @param reason The reason for stopping closing connection
   */
  public void stop(DisconnectReason reason) {
    send(EXIT_MESSAGE + reason.code);
    isClosing = true;
  }
  
  
  /**
   * Sends the specified message to the server
   * @param message The message to send
   */
  public void send(String message) {
    if (!isClosing) {
      messages.offer(SEPARATOR_CHAR + message + SEPARATOR_CHAR);
      lock.lock();
      condition.signalAll();
      lock.unlock();
    }
  }
  
  /**
   * Flushes the output buffer and sends all lingering messages over the network.
   *  If the connection was made with TCP no delay activated this method has no effect.
   */
  public void flush() {
    if (!isClosing) {
      shouldFlush = true;
      lock.lock();
      condition.signalAll();
      lock.unlock();
    }
  }
  
  
  protected void sendHello() {
    writeString(SEPARATOR_CHAR + HELLO_MESSAGE + SEPARATOR_CHAR);
    try {
      writer.flush();
    } catch (IOException e) { }
  }
  
  
  /**
   * A method that reads all available messages and dispatches them.
   */
  protected void read() {
    // Read
    int amount = readChars(chars);
    if (amount > 0) {
      builder.append(chars, 0, amount);
      
      while (readReady()) {
        amount = readChars(chars);
        builder.append(chars, 0, amount);
      }
      
      String receivedString = builder.toString();
      builder.setLength(0);
      
      int startIndex = receivedString.indexOf(SEPARATOR_CHAR);
      int endIndex = receivedString.indexOf(SEPARATOR_CHAR, startIndex+1);
      while (startIndex != -1 && endIndex != -1) {
        String message = receivedString.substring(startIndex+1, endIndex);
        if (message.length() > 0)
          dispatchMessage(message);

        startIndex = receivedString.indexOf(SEPARATOR_CHAR, endIndex+1);
        endIndex = receivedString.indexOf(SEPARATOR_CHAR, startIndex+1);
      }
      
      if (startIndex != -1) { // We have a message that didn't fully arrive
        builder.append(receivedString.substring(startIndex, receivedString.length()));
      }
    }
  }

  private String dispatchMessage(String msg) {
    msg = msg.trim();
    
    if (msg.equals(HELLO_MESSAGE)) {
      hasBeenGreeted = true;
      lock.lock();
      condition.signalAll();
      lock.unlock();
      connectionManager.connected(this);
    }
    else if (msg.startsWith(EXIT_MESSAGE) && msg.length() == EXIT_MESSAGE.length() + 1) {
    	int code = msg.charAt(msg.length() - 1) - '0';
    	DisconnectReason reason = DisconnectReason.reasonFromCode(code);
      if (isClosing) {
      	isConnected = false;
        connectionManager.disconnected(this, reason);
      } else {
        send(msg);
        isConnected = false;
        connectionManager.disconnected(this, reason);
      }
    }
    else if (msg.startsWith(PING_REQUEST_MESSAGE)) {
      send(PING_ANSWER_MESSAGE + msg.substring(PING_REQUEST_MESSAGE.length()));
    }
    else if (msg.startsWith(PING_ANSWER_MESSAGE)) {
      if (msg.substring(PING_ANSWER_MESSAGE.length()).equals(pingMessage)) { // ping arrived in
        currentLatency = (int)(System.currentTimeMillis() - lastPing);
        retries = 0;
        hasRecievedAnswer = true;
      }
    }
    else if (!msg.equals("")) { // Dispatch
      connectionManager.receivedMessage(Message.createMessage(this, msg));
    }
    return msg;
  }
  
  
  /**
   * A method that writes all available messages and dispatches them.
   */
  protected void write() {
    int s = messages.size();
    int c = 0;
    while (!messages.isEmpty() && c++ < s)
      writeString(messages.poll());
    
    if (shouldFlush || useTcpNoDelay) {
      try {
        writer.flush();
      } catch (IOException e) { }
      shouldFlush = false;
    }
  }
  
  
  protected void attemptPing() {
    if (hasRecievedAnswer) {
      if (System.currentTimeMillis() - lastPing > pingInterval) {
        sendPing();
      }
    }
    else if (System.currentTimeMillis() - lastPing > maximumLatency) {
      if (retries++ >= maximumRetries) {
        // Lost connection
      	isConnected = false;
        connectionManager.disconnected(this, DisconnectReason.Timeout);
      } else {
      	if (isInVerboseMode) {
      		Debugger.debug(AbstractConnection.class.getSimpleName() + ": attemptPing()", "Lost ping, retrying (" + retries + "/" + maximumRetries + ")", false);
      	}
        sendPing();
      }
    }
  }

  private void sendPing() {
    lastPingNumber += 1;
    if (lastPingNumber > 99)
      lastPingNumber = 0;

    lastPing = System.currentTimeMillis();
    pingMessage = "" + lastPingNumber;
    send(PING_REQUEST_MESSAGE + pingMessage);
    hasRecievedAnswer = false;
  }


  private boolean readReady() {
    try {
      return reader.ready();
    } catch (IOException e) { }
    
    return false;
  }
  
  private int readChars(char[] chars) {
    try {
      int amountRead = reader.read(chars);
      if (amountRead == -1)
        lostConnection();
      return amountRead;
    } catch (IOException e) {
      lostConnection();
    }
    
    return 0;
  }
  
  private void writeString(String text) {
    try {
      writer.write(text);
    } catch (IOException e) { }
  }
  
  protected void lostConnection() {
    if (isConnected) {
    	isClosing = true;
    	isConnected = false;
      connectionManager.disconnected(this, DisconnectReason.Unknown);
    }
  }
}
