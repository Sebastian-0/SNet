/*
 * Copyright (C) 2016 Sebastian Hjelm
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package network;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import network.internal.AbstractConnection;
import network.internal.ClientConnection;
import network.internal.ConnectionManagerInterface;
import network.internal.Message;
import network.internal.ServerConnection;
import network.internal.ServerConnectionListener;
import sbasicgui.util.Debugger;

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
public class Network
{
  private static HashMap<Character, NetworkHook> hooks_;
  
  private ClientConnection client_;
  private ServerConnectionListener server_;
  
  private ConnectionLifecycleListener connectionListener_;
  
  private BlockingQueue<Message> queuedMessages_;
  private boolean waitForPoll_;
  
  
  static
  {
    hooks_ = new HashMap<Character, NetworkHook>();
  }
  
  
  private Network()
  {
    queuedMessages_ = new LinkedBlockingQueue<Message>();
  }
  
  
  /**
   * Creates a network interface for <i>clients</i> using the specified host
   *  server and port. The connection listener receives information on when a
   *  client-server connection is established or lost. 
   * @param connectionListener The connection listener to use
   * @param host The host server address
   * @param port The host server port
   */
  public Network(ConnectionLifecycleListener connectionListener, String host, int port)
  {
    this ();
    
    connectionListener_ = connectionListener;
    
    client_ = new ClientConnection(connectionManager, host, port);
  }
  
  
  /**
   * Creates a network interface for <i>servers</i> using the specified port.
   * The connection listener receives information on when a client-server
   *  connection is established or lost.
   * @param connectionListener The connection listener to use
   * @param port The host server port
   */
  public Network(ConnectionLifecycleListener connectionListener, int port)
  {
    this ();
    
    connectionListener_ = connectionListener;
    
    server_ = new ServerConnectionListener(connectionManager, port);
  }
  
  
  /**
   * Sets whether or not the messages received should be delivered immediately
   *  (asynchronously) by the networking threads or if all the messages should be 
   *  queued until {@link #pollMessages(boolean)} is called (in that case all the messages 
   *  will be dispatched by the thread invoking the poll-method). The default 
   *  value for this state is <code>false</code>.
   * @param shouldWaitforPoll The new state
   */
  public void setWaitForPoll(boolean shouldWaitforPoll)
  {
    waitForPoll_ = shouldWaitforPoll;
  }
  
  
  /**
   * If this network is set to wait for polling this method will dispatch all the
   *  messages received since the last call to this method, otherwise it will
   *  do nothing. 
   * @param blockIfNoMessages Specifies whether or not to block until messages are available
   * @throws InterruptedException If interrupted when waiting for messages
   */
  public void pollMessages(boolean blockIfNoMessages) throws InterruptedException
  {
    if (!waitForPoll_ && queuedMessages_.isEmpty())
      return;
    
    if (!queuedMessages_.isEmpty() || blockIfNoMessages)
    {
      do
      {
        Message message = queuedMessages_.take();
        processMessage(message, "pollMessages()");
      }
      while (!queuedMessages_.isEmpty());
    }
  }

  private void processMessage(Message message, String methodName)
  {
    NetworkHook hook = hooks_.get(message.message.charAt(0));
    if (hook != null)
    {
      try
      {
        if (client_ != null)
          hook.client(Network.this, message);
        else
          hook.server(Network.this, message);
      } catch (Throwable e) { // TODO Network; Disconnect?
        Debugger.error("Network: " + methodName, "Exception occured when executing command " + hook.getClass().getSimpleName(), e);
      }
      
      message.dispose();
    }
    else
    {
      Debugger.error("Network: " + methodName, "No network hook with the specified command code: " + Network.commandCodeString(message.message.charAt(0)));
    }
  }
  
  
  /**
   * Sends the specified message to the server/client connected through this
   *  network. Remember to parse the message using the appropriate network hook
   *  before sending it! You can parse messages using
   *  {@link NetworkHook#createMessage(String)}.
   * </br>
   * </br>The id is only used if this network is a server. In that case it is
   *  used to identify which server listener to send the message to.
   * @param id The id of the server listener to send the message to, or
   *  <code>null</code> if this network is a client
   * @param message The message to send
   * @throws IllegalArgumentException If the id is invalid (server networks only)
   */
  public void send(String id, Message message)
  {
    if (client_ != null)
      client_.send(message.message);
    else if (server_ != null)
    {
      ServerConnection listener = server_.getServerListeners().get(id);
      if (listener != null)
        listener.send(message.message);
      else
        throw new IllegalArgumentException("No server listener with id: " + id);
    }
  }
  
  
  /**
   * Sends the specified message to all clients. This method only works for the
   *  server, an exception is thrown if called by a client. Remember to parse
   *  the message using the appropriate network hook before sending it! You can
   *  parse messages using {@link NetworkHook#createMessage(String)}.
   * @param message The message to send
   * @throws UnsupportedOperationException If this method is called by a client
   */
  public void sendToAll(Message message)
  {
    if (client_ != null)
      throw new UnsupportedOperationException("This operation is only supported for servers");
    else if (server_ != null)
      server_.sendToAll(message.message);
  }
  
  
  /**
   * Forwards the specified message to all clients, except the one it came from.
   *  This method only works for the server, an exception is thrown if called by
   *  a client. 
   * @param message The message to forward
   * @throws UnsupportedOperationException If this method is called by a client
   */
  public void forward(Message message)
  {
    if (client_ != null)
      throw new UnsupportedOperationException("This operation is only supported for servers");
    else if (server_ != null)
      server_.forward(message);
  }
  
  
  /**
   * Disconnects this network from all its clients/the server. This process is
   *  not instantaneous, use {@link #isConnected()} to check when this network
   *  is fully disconnected.
   */
  public void disconnect()
  {
    if (client_ != null)
      client_.stop();
    else if (server_ != null)
      server_.stop();
  }
  
  
  /**
   * Drops the client with the specified id from the server, if this network is
   *  a client this method does nothing.
   * @param id The id of the user to drop
   */
  public void drop(String id)
  {
    if (server_ != null)
      server_.getServerListeners().get(id).stop();
  }
  
  
  /**
   * Returns whether or not this network is connected to a client/server.
   * @return Whether or not this network is connected to a client/server
   */
  public boolean isConnected()
  {
    if (client_ != null)
      return client_.isConnected();
    else if (server_ != null)
      return server_.isConnected();
    
    return false;
  }
  
  
  /**
   * Returns whether or not this client could connect to the server if this is
   *  a client, or whether or not this server failed to initialize if this is a
   *  server.
   * @return Whether or not this client/server could connect/initialize
   */
  public boolean connectionFailed()
  {
    if (client_ != null)
      return client_.couldNotConnect();
    else if (server_ != null)
      return server_.couldNotConnect();
    
    return false;
  }
  
  
  /**
   * Returns whether or not this network is a server or not.
   * @return Whether or not this network is a server or not
   */
  public boolean isServer()
  {
    return server_ != null;
  }
  
  
  /**
   * Returns whether or not this network is a client or not.
   * @return Whether or not this network is a client or not
   */
  public boolean isClient()
  {
    return client_ != null;
  }
  
  
  /**
   * Returns the latency of this network if this is a client, or the latency of
   *  the server listener with the specified id if this is a server.
   * </br>
   * </br>The latency returned may be zero if no ping messages have been sent yet.
   * @param id The id of the server listener to get the latency of, or
   *  <code>null</code> if this network is a client
   * @throws IllegalArgumentException If the id is invalid (server networks only)
   */
  public int getLatency(String id)
  {
    if (client_ != null)
      return client_.getCurrentLatency();
    else if (server_ != null)
    {
      ServerConnection listener = server_.getServerListeners().get(id);
      if (listener != null)
        return listener.getCurrentLatency();
      else
        throw new IllegalArgumentException("No server listener with id: " + id);
    }
    
    return 0;
  }
  
  
  /**
   * Register a network hook to the network system. When a hook is registered
   *  messages of its type will automatically be redirected to that network hook.
   * @param networkHook The network hook to add
   * @throws IllegalArgumentException If there already is a network hook with the
   *  command code used by the new hook.
   */
  public static void registerHook(NetworkHook networkHook)
  {
    if (hooks_.get(networkHook.getCommandCode()) == null)
    {
      hooks_.put(networkHook.getCommandCode(), networkHook);
      return;
    }
    
//    Debugger.error("Network: registerHook()", "There was already a network hook with the specified command code!");
    
    throw new IllegalArgumentException("There was already a network hook with the command code: " + commandCodeString(networkHook.getCommandCode()));
  }
  
  private static String commandCodeString(char code)
  {
    return code + " (\\u" + Integer.toHexString(code | 0x10000).substring(1) + ")";
  }
  
  
  
  private ConnectionManagerInterface connectionManager = new ConnectionManagerInterface() {
    
    @Override
    public void receivedMessage(Message message)
    {
      if (waitForPoll_)
      {
        queuedMessages_.offer(message);
      }
      else
      {
        processMessage(message, "CM: receivedMessage()");
      }
    }
    
    @Override
    public void failedToStart(ServerConnectionListener server, ClientConnection client) {
      connectionListener_.failedToStart();
    }
    
    @Override
    public void serverStarted(int port)
    {
      if (server_ != null)
        connectionListener_.serverStarted(port);
    }
    
    @Override
    public void connected(AbstractConnection connector)
    {
      if (connector instanceof ServerConnection)
        connectionListener_.connected(((ServerConnection)connector).getId());
      else
        connectionListener_.connected(null);
    }
    
    @Override
    public void disconnected(AbstractConnection connector, byte reason, String message)
    {
      if (connector instanceof ServerConnection)
        connectionListener_.disconnected(((ServerConnection)connector).getId(), reason, message);
      else
        connectionListener_.disconnected(null, reason, message);
    }
  };
}
