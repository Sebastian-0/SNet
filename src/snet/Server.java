/*
 * Copyright (C) 2016 Sebastian Hjelm
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package snet;

import snet.internal.AbstractConnection;
import snet.internal.ClientConnection;
import snet.internal.ConnectionManagerInterface;
import snet.internal.ConnectionManagerInterface.DisconnectReason;
import snet.internal.Message;
import snet.internal.ServerConnection;
import snet.internal.ServerConnectionListener;

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
  
  private ServerLifecycleListener serverLifecycleListener;
  
  
  /**
   * Creates a network interface for <i>servers</i>. The connection listener 
   *  receives information on when a client-server connection is established or lost.
   *  Call {@link #start(int)} to start the server.
   * @param lifecycleListener The lifecycle listener to use
   */
  public Server(ServerLifecycleListener lifecycleListener) {
    this.serverLifecycleListener = lifecycleListener;
  }

  /**
   * Starts a server at the specified port.
   * @param port The port to use
   */
	public void start(int port) {
		connectionListener = new ServerConnectionListener(connectionManager, port, useTcpNoDelay);
	}
  
  @Override
  protected void dispatchMessage(Message message, NetworkHook<?> hook) {
  	hook.server(this, message);
  }
  
  
  @Override
  public void send(Message message) {
    throw new UnsupportedOperationException("This operation is only supported for clients");
  }
 
  @Override
	public void send(String id, Message message) {
    if (connectionListener != null) {
      ServerConnection connection = connectionListener.getConnections().get(id);
      if (connection != null)
        connection.send(message.message);
      else
        throw new IllegalArgumentException("No client with id: " + id);
    }
  }
  
  @Override
	public void sendToAll(Message message) {
    if (connectionListener != null)
      connectionListener.sendToAll(message.message);
  }
  
  @Override
	public void forward(Message message) {
    if (connectionListener != null)
      connectionListener.forward(message);
  }
  
  /**
   * {@inheritDoc}
   * <br />
   * <br /> <b>Note:</b> This method flushes the sockets of all the clients, it might be
   *  more efficient to call {@link Server#flush(String)} if you only want to flush a
   *  single client.
   */
  @Override
  public void flush() {
    if (connectionListener != null) {
      for (ServerConnection connection : connectionListener.getConnections().values()) {
        connection.flush();
      }
    }
  }
  
  @Override
  public void flush(String id) {
    if (connectionListener != null) {
      ServerConnection connection = connectionListener.getConnections().get(id);
      if (connection != null)
        connection.flush();
      else
        throw new IllegalArgumentException("No client with id: " + id);
    }
  }
  
  
  @Override
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
      connectionListener.getConnections().get(id).stop(DisconnectReason.Kicked);
  }
  
  
  @Override
	public boolean isConnected() {
    if (connectionListener != null)
      return connectionListener.isConnected();
    
    return false;
  }
  
  @Override
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
    public void disconnected(AbstractConnection connector, DisconnectReason reason) {
      serverLifecycleListener.disconnected(((ServerConnection)connector).getId(), reason);
    }
  };
}
