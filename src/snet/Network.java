/*
 * Copyright (C) 2016 Sebastian Hjelm
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package snet;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import sbasicgui.util.Debugger;
import snet.internal.Message;

/**
 * This class manages the connection between the networking backend and the game
 *  mechanics. The communication is done through {@link NetworkHook NetworkHooks},
 *  which are registered using {@link #registerHook(NetworkHook)}. The network
 *  hooks has methods for managing both server and client connections.
 * </br>
 * </br>To create a connection, just create in instance of this class using the
 *  appropriate constructor. One constructor creates a server, and the other
 *  creates a client. To send messages just use the messaging methods of this
 *  class, but remember to always parse the messages using the appropriate
 *  network hook before sending them! You should use
 *  {@link NetworkHook#createMessage(String)} to parse the messages.
 * @author Sebastian Hjelm
 */
public abstract class Network {
  private HashMap<Character, NetworkHook<?>> networkHooks;
  private HashMap<Class<?>, NetworkHook<?>> networkHooksByClass;
  
  private BlockingQueue<Message> queuedMessages;
  private boolean waitForPoll;
  
  
  public Network() {
    queuedMessages = new LinkedBlockingQueue<Message>();
    networkHooks = new HashMap<Character, NetworkHook<?>>();
    networkHooksByClass = new HashMap<Class<?>, NetworkHook<?>>();
  }
  
  
  
  /**
   * Sets whether or not the messages received should be delivered immediately
   *  (asynchronously) by the networking threads or if all the messages should be 
   *  queued until {@link #pollMessages(boolean)} is called (in that case all the messages 
   *  will be dispatched by the thread invoking the poll-method). The default 
   *  value for this state is <code>false</code>.
   * @param shouldWaitforPoll The new state
   */
  public void setWaitForPoll(boolean shouldWaitforPoll) {
    waitForPoll = shouldWaitforPoll;
  }

  
	protected void receivedMessage(Message message) {
		if (waitForPoll) {
      queuedMessages.offer(message);
    } else {
      processMessage(message, "CM: receivedMessage()");
    }
	}
  
  
  /**
   * If this network is set to wait for polling this method will dispatch all the
   *  messages received since the last call to this method, otherwise it will
   *  do nothing. 
   * @param blockIfNoMessages Specifies whether or not to block until messages are available
   * @throws InterruptedException If interrupted when waiting for messages
   */
  public void pollMessages(boolean blockIfNoMessages) throws InterruptedException {
    if (!waitForPoll && queuedMessages.isEmpty())
      return;
    
    if (!queuedMessages.isEmpty() || blockIfNoMessages) {
      do {
        Message message = queuedMessages.take();
        processMessage(message, "pollMessages()");
      }
      while (!queuedMessages.isEmpty());
    }
  }

  private void processMessage(Message message, String methodName) {
    NetworkHook<?> hook = networkHooks.get(message.message.charAt(0));
    if (hook != null) {
      try {
        dispatchMessage(message, hook);
      } catch (Throwable e) { // TODO Network; Disconnect?
        Debugger.error("Network: " + methodName, "Exception occured when executing command " + hook.getClass().getSimpleName(), e);
      }
      
      message.dispose();
    } else {
      Debugger.error("Network: " + methodName, "No network hook with the specified command code: " + commandCodeString(message.message.charAt(0)));
    }
  }

	protected abstract void dispatchMessage(Message message, NetworkHook<?> hook);
  
	
  /**
   * Sends the specified message to the server. If this is the server the 
   *  behavior is undefined. Remember to parse the message using the appropriate
   *  network hook before sending it! You can parse messages using
   *  {@link NetworkHook#createMessage(String)}.
   * @param message The message to send
   */
  public abstract void send(Message message);
  
  /**
   * Sends the specified message to the server/client connected through this
   *  network. When the id is <code>null</code> this method has the same behavior
   *  as {@link #send(Message)}. Remember to parse the message using the 
   *  appropriate network hook before sending it! You can parse messages using
   *  {@link NetworkHook#createMessage(String)}.
   * @param id The id of the client to send the message to, or
   *  <code>null</code> when sending to the server
   * @param message The message to send
   * @throws IllegalArgumentException If the id is invalid
   */
  public abstract void send(String id, Message message);
  
  
  /**
   * Sends the specified message to everyone. Remember to parse
   *  the message using the appropriate network hook before sending it! You can
   *  parse messages using {@link NetworkHook#createMessage(String)}.
   * @param message The message to send
   * @throws UnsupportedOperationException If this method is called by a client
   */
  public abstract void sendToAll(Message message);
  
  
  /**
   * Forwards the specified message to everyone, except the sender.
   * @param message The message to forward
   */
  public abstract void forward(Message message);
  
  
  /**
   * Disconnects this network from all its clients/the server. This process is
   *  not instantaneous, use {@link #isConnected()} to check when this network
   *  is fully disconnected.
   */
  public abstract void disconnect();
  
  
  /**
   * Returns whether or not this network is connected to a client/server.
   * @return Whether or not this network is connected to a client/server
   */
  public abstract boolean isConnected();
  
  
  /**
   * Returns whether or not this client could connect to the server if this is
   *  a client, or whether or not this server failed to initialize if this is a
   *  server.
   * @return Whether or not this client/server could connect/initialize
   */
  public abstract boolean connectionFailed();
  
  
//  /**
//   * Returns whether or not this network is a server or not.
//   * @return Whether or not this network is a server or not
//   */
//  public boolean isServer()
//  {
//    return server_ != null;
//  }
//  
//  
//  /**
//   * Returns whether or not this network is a client or not.
//   * @return Whether or not this network is a client or not
//   */
//  public boolean isClient()
//  {
//    return client_ != null;
//  }
  
  
  /**
   * Register a network hook to the network system. When a hook is registered
   *  messages of its type will automatically be redirected to that network hook.
   * @param networkHook The network hook to add
   * @throws IllegalArgumentException If there already is a network hook with the
   *  command code used by the new hook.
   */
  public void registerHook(NetworkHook<?> networkHook) {
    if (networkHooks.get(networkHook.getCommandCode()) == null) {
      networkHooks.put(networkHook.getCommandCode(), networkHook);
      networkHooksByClass.put(networkHook.getClass(), networkHook);
    }
    else {
    	throw new IllegalArgumentException("There was already a network hook with the command code: " + commandCodeString(networkHook.getCommandCode()));
    }
  }
  
  private String commandCodeString(char code) {
    return code + " (\\u" + Integer.toHexString(code | 0x10000).substring(1) + ")";
  }

  /**
   * Returns any network hook registered by the specified command code, or 
   *  <code>null</code> if there is none.
   * @param commandCode The command code to search for
   * @return Any network hook registered using the specified command code
   */
  public NetworkHook<?> getNetworkHook(char commandCode) {
  	return networkHooks.get(commandCode);
  }
  /**
   * Returns any network hook that is if the specified class, or 
   *  <code>null</code> if there is none.
   * @param hookClass The class to search for
   * @return Any network hook that is if the specified class
   */
  public NetworkHook<?> getNetworkHook(Class<?> hookClass) {
  	return networkHooksByClass.get(hookClass);
  }
}
