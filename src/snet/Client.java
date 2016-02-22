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
import snet.internal.Message;
import snet.internal.ServerConnectionListener;

/**
 * This class manages the client connection. The communication is done through
 *  {@link NetworkHook NetworkHooks}, which are registered using
 *  {@link #registerHook(NetworkHook)}.
 * </br>
 * </br>To send messages just use the messaging methods of this
 *  class, but remember to always parse the messages using the appropriate
 *  network hook before sending them! You should use
 *  {@link NetworkHook#createMessage(String)} to parse the messages.
 * @author Sebastian Hjelm
 */
public class Client extends Network {
  private ClientConnection connection;
  
  private ClientLifecycleListener clientLifecycleListener;
  
  
  /**
   * Creates a network interface for <i>clients</i>. The lifecycle listener 
   *  receives information on when a client-server connection is established or lost. 
   *  Call {@link #start(String, int)} to connect to a server.
   * @param lifecycleListener The lifecycle listener to use
   */
  public Client(ClientLifecycleListener lifecycleListener) {
    this.clientLifecycleListener = lifecycleListener;
  }

  /**
   * Opens a connection the specified host using the specified port.
   * @param host The host address
   * @param port The host port
   */
	public void start(String host, int port) {
		connection = new ClientConnection(connectionManager, host, port);
	}
  

	@Override
	protected void dispatchMessage(Message message, NetworkHook<?> hook) {
		hook.client(this, message);
	}
  
  @Override
  public void send(Message message) {
  	send(null, message);
  }
  
  public void send(String id, Message message) {
    if (connection != null)
      connection.send(message.message);
  }
  
  public void sendToAll(Message message) {
    if (connection != null)
      throw new UnsupportedOperationException("This operation is only supported for servers");
  }
  
  public void forward(Message message) {
    if (connection != null)
      throw new UnsupportedOperationException("This operation is only supported for servers");
  }
  
  
  public void disconnect() {
    if (connection != null)
      connection.stop();
  }
  
  
  public boolean isConnected() {
    if (connection != null)
      return connection.isConnected();
    
    return false;
  }
  
  public boolean connectionFailed() {
    if (connection != null)
      return connection.couldNotConnect();
    
    return false;
  }
  
  
  /**
   * Returns the latency of this client. The latency returned may be zero if no 
   *  ping messages have been sent yet.
   */
  public int getLatency() {
    if (connection != null)
      return connection.getCurrentLatency();
    
    return 0;
  }
  
  
  
  private ConnectionManagerInterface connectionManager = new ConnectionManagerInterface() {
    
    @Override
    public void receivedMessage(Message message) {
      Client.this.receivedMessage(message);
    }
    
    @Override
    public void failedToStart(ServerConnectionListener server, ClientConnection client) {
      clientLifecycleListener.failedToEstablishConnection();
    }
    
    @Override
    public void serverStarted(int port) { }
    
    @Override
    public void connected(AbstractConnection connector) {
      clientLifecycleListener.connected();
    }
    
    @Override
    public void disconnected(AbstractConnection connector, byte reason, String message) {
      clientLifecycleListener.disconnected(reason, message);
    }
  };
}
