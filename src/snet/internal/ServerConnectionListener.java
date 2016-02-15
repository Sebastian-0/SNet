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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import sbasicgui.util.Debugger;


public class ServerConnectionListener {
  private ConnectionManagerInterface manager;
  
  private volatile Map<String, ServerConnection> connections;

  private volatile int port;
  private volatile boolean couldNotConnect;
  
  private ServerSocket serverSocket;
  
  private volatile boolean isRunning;
  
  
  public ServerConnectionListener(ConnectionManagerInterface manager, int port) {
    this.manager = manager;
    this.port = port;
    
    connections = Collections.synchronizedMap(new HashMap<String, ServerConnection>());
    
    thread.start();
  }
  
  /**
   * Returns all the server listeners of this server. There is one server listener
   *  for each connected client. The listeners are stored with their ids as the
   *  keys.
   * @return All the server listeners of this server
   */
  public Map<String, ServerConnection> getConnections() {
    return connections;
  }
  
  
  void terminated(ServerConnection connection) {
    connections.remove(connection.getId());
  }
  
  
  /**
   * Initiates the disconnect-sequence for all connected clients. The server will
   *  send the exit message to all clients and wait for the response. When the
   *  response has arrived the connection closes.
   * </br>
   * </br><b>Note:</b> {@link ConnectionManagerInterface#disconnected(byte, String) disconnected(byte, String)}
   *  of the connection manager connected with this connection will be invoked
   *  when the response for each connection arrives.
   */
  public void stop() {
    if (isRunning) {
      for (ServerConnection connection : connections.values())
        connection.stop();
      
      isRunning = false;
      try {
        serverSocket.close();
      } catch (IOException e) { }
    } else {
      thread.interrupt();
    }
  }
  
  
  /**
   * Returns true if the server hasn't been stopped, or when it has been stopped
   *  and all connections haven't closed yet.
   * @return True if this server is connected
   */
  public boolean isConnected() {
    if (isRunning)
      return true;
    
    boolean has = false;
    for (ServerConnection connection : connections.values()) {
      if (connection.isConnected()) {
        has = true;
        break;
      }
    }
    
    return has;
  }
  
  /**
   * Returns true if this server failed to initialize.
   * @return True if this server failed to initialize
   */
  public boolean couldNotConnect() {
    return couldNotConnect;
  }
  
  
  /**
   * Sends the specified message to all clients.
   * @param message The message to send
   */
  public void sendToAll(String message) {
    for (ServerConnection connection : connections.values())
      connection.send(message);
  }
  
  
  /**
   * Forwards the specified message to all the clients (except the one it came
   *  from).
   * @param message The message to forward
   */
  public void forward(Message message) {
    for (ServerConnection connection : connections.values())
      if (connection != message.receiver)
        connection.send(message.message);
  }
  
  
  private Thread thread = new Thread() {
    
    {
      setName("Server connection listener");
    }
    
    @Override
    public void run() {
      if (!isPortAvailable(port)) {
        startFailed();
        return;
      }
      
      try {
        serverSocket = new ServerSocket(port);
      } catch (IOException e1) {
        startFailed();
        return;
      }
      if (Thread.interrupted()) // The server was stopped while starting
        return;
      
      isRunning = true;
      manager.serverStarted(serverSocket.getLocalPort());
      
      while (isRunning) {
        try {
          Socket newSocket = serverSocket.accept();
          newSocket.setTcpNoDelay(true);
          
          ServerConnection connection = new ServerConnection(manager, newSocket, ServerConnectionListener.this);
          
          while (connections.get(connection.getId()) != null)
            connection.generateId();
          
          connections.put(connection.getId(), connection);
        } catch (IOException e) {
          if (isRunning) // If 'false' the server's stop() has been invoked. In that case this exception is expected
            Debugger.error("Server: run()", "An error occured while waiting for a connection");
        }
      }
    }

    private boolean isPortAvailable(int port) {
      if (port != 0) {
        try {
          Socket socket = new Socket("localhost", port);
          socket.close();
          return false;
        }
        catch (UnknownHostException e) { }
        catch (IOException e) { }
      }
      
      return true;
    }

    private void startFailed() {
      isRunning = false;
      couldNotConnect = true;
      Debugger.error("Server: Thread: startFailed()", "Socket already used!");
      manager.failedToStart(ServerConnectionListener.this, null);
    }
  };
}
