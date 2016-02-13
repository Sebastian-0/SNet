/*
 * Copyright (C) 2016 Sebastian Hjelm
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package network;

import network.internal.AbstractConnection;
import network.internal.ClientConnection;
import network.internal.ConnectionManagerInterface;
import network.internal.Message;
import network.internal.ServerConnection;
import network.internal.ServerConnectionListener;

/**
 * This class manages the server connection. The communication is done through 
 *  {@link NetworkHook NetworkHooks}, which are registered using 
 *  {@link #registerHook(NetworkHook)}.
 * </br>
 * </br>To send messages just use the messaging methods of this
 *  class, but remember to always parse the messages using the appropriate
 *  network hook before sending them! You should use
 *  {@link NetworkHook#createMessage(String)} to parse the messages.
 * @author Sebastian Hjelm
 */
public class Server extends Network {
  private ServerConnectionListener connectionListener;
  
  private ConnectionLifecycleListener serverLifecycleListener;
  
  
  /**
   * Creates a network interface for <i>servers</i> using the specified port.
   * The connection listener receives information on when a client-server
   *  connection is established or lost.
   * @param connectionListener The connection listener to use
   * @param port The host server port
   */
  public Server(ConnectionLifecycleListener connectionLifecycleListener, int port) {
    this.serverLifecycleListener = connectionLifecycleListener;
    
    connectionListener = new ServerConnectionListener(connectionManager, port);
  }
  
  @Override
  protected void dispatchMessage(Message message, NetworkHook hook) {
  	hook.server(this, message);
  }
  
  
  @Override
  public void send(Message message) {
    throw new UnsupportedOperationException("This operation is only supported for clients");
  }
 
  public void send(String id, Message message) {
    if (connectionListener != null) {
      ServerConnection connection = connectionListener.getConnections().get(id);
      if (connection != null)
        connection.send(message.message);
      else
        throw new IllegalArgumentException("No client with id: " + id);
    }
  }
  
  public void sendToAll(Message message) {
    if (connectionListener != null)
      connectionListener.sendToAll(message.message);
  }
  
  public void forward(Message message) {
    if (connectionListener != null)
      connectionListener.forward(message);
  }
  
  
  public void disconnect() {
    if (connectionListener != null)
      connectionListener.stop();
  }
  
  /**
   * Drops the client with the specified id from the server.
   * @param id The id of the client to drop
   */
  public void drop(String id) {
    if (connectionListener != null)
      connectionListener.getConnections().get(id).stop();
  }
  
  
  public boolean isConnected() {
    if (connectionListener != null)
      return connectionListener.isConnected();
    
    return false;
  }
  
  public boolean connectionFailed() {
    if (connectionListener != null)
      return connectionListener.couldNotConnect();
    
    return false;
  }
  
  
  /**
   * Returns the latency of the client with the specified id.
   * </br>
   * </br>The latency returned may be zero if no ping messages have been sent yet.
   * @param id The id of the client to get the latency of
   * @throws IllegalArgumentException If the id is invalid
   */
  public int getLatency(String id)
  {
    if (connectionListener != null) {
      ServerConnection connection = connectionListener.getConnections().get(id);
      if (connection != null)
        return connection.getCurrentLatency();
      else
        throw new IllegalArgumentException("No client with id: " + id);
    }
    
    return 0;
  }
  
  
  
  private ConnectionManagerInterface connectionManager = new ConnectionManagerInterface() {
    
    @Override
    public void receivedMessage(Message message) {
      Server.this.receivedMessage(message);
    }
    
    @Override
    public void failedToStart(ServerConnectionListener server, ClientConnection client) {
      serverLifecycleListener.failedToStart();
    }
    
    @Override
    public void serverStarted(int port) {
      serverLifecycleListener.serverStarted(port);
    }
    
    @Override
    public void connected(AbstractConnection connector) {
      serverLifecycleListener.connected(((ServerConnection)connector).getId());
    }
    
    @Override
    public void disconnected(AbstractConnection connector, byte reason, String message) {
      serverLifecycleListener.disconnected(((ServerConnection)connector).getId(), reason, message);
    }
  };
}
