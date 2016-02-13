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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import sbasicgui.util.Debugger;


public class ServerConnectionListener
{
  private ConnectionManagerInterface manager_;
  
  private volatile Map<String, ServerConnection> connections_;

  private volatile int     port_;
  private volatile boolean couldNotConnect_;
  
  private ServerSocket socket_;
  
  private volatile boolean isRunning_;
  
  
  public ServerConnectionListener(ConnectionManagerInterface manager, int port)
  {
    manager_ = manager;
    port_ = port;
    
    connections_ = Collections.synchronizedMap(new HashMap<String, ServerConnection>());
    
    thread.start();
  }
  
  /**
   * Returns all the server listeners of this server. There is one server listener
   *  for each connected client. The listeners are stored with their ids as the
   *  keys.
   * @return All the server listeners of this server
   */
  public Map<String, ServerConnection> getConnections()
  {
    synchronized (this)
    {
      return connections_;
    }
  }
  
  
  void terminated(ServerConnection connection)
  {
    connections_.remove(connection.getId());
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
  public void stop()
  {
    if (isRunning_)
    {
      for (ServerConnection connection : connections_.values())
        connection.stop();
      
      isRunning_ = false;
      try {
        socket_.close();
      } catch (IOException e) { }
    }
    else
    {
      thread.interrupt();
    }
  }
  
  
  /**
   * Returns true if the server hasn't been stopped, or when it has been stopped
   *  and all connections haven't closed yet.
   * @return True if this server is connected
   */
  public boolean isConnected()
  {
    if (isRunning_)
      return true;
    
    boolean has = false;
    for (ServerConnection connection : connections_.values())
    {
      if (connection.isConnected())
      {
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
  public boolean couldNotConnect()
  {
    return couldNotConnect_;
  }
  
  
  /**
   * Sends the specified message to all clients.
   * @param message The message to send
   */
  public void sendToAll(String message)
  {
    for (ServerConnection sl : connections_.values())
      sl.send(message);
  }
  
  
  /**
   * Forwards the specified message to all the clients (except the one it came
   *  from).
   * @param message The message to forward
   */
  public void forward(Message message)
  {
    for (ServerConnection connection : connections_.values())
      if (connection != message.receiver)
        connection.send(message.message);
  }
  
  
  private Thread thread = new Thread() {
    
    {
      setName("Server connection listener");
    }
    
    @Override
    public void run()
    {
      if (!isPortAvailable(port_))
      {
        startFailed();
        return;
      }
      
      try {
        socket_ = new ServerSocket(port_);
      } catch (IOException e1) {
        startFailed();
        return;
      }
      if (Thread.interrupted()) // The server was stopped while starting
        return;
      
      isRunning_ = true;
      manager_.serverStarted(socket_.getLocalPort());
      
      while (isRunning_)
      {
        try {
          Socket connection = socket_.accept();
          connection.setTcpNoDelay(true);
          
          ServerConnection listener = new ServerConnection(manager_, connection, ServerConnectionListener.this);
          
          while (connections_.get(listener.getId()) != null)
            listener.generateId();
          
          connections_.put(listener.getId(), listener);
        } catch (IOException e) {
          if (isRunning_) // If 'false' the server's stop() has been invoked. In that case this exception is expected
            Debugger.error("Server: run()", "An error occured while waiting for a connection");
        }
      }
    }

    private boolean isPortAvailable(int port)
    {
      if (port != 0)
      {
        try
        {
          Socket socket = new Socket("localhost", port);
          socket.close();
          return false;
        }
        catch (UnknownHostException e) { }
        catch (IOException e) { }
      }
      
      return true;
    }

    private void startFailed()
    {
      isRunning_ = false;
      couldNotConnect_ = true;
      Debugger.error("Server: Thread: startFailed()", "Socket already used!");
      manager_.failedToStart(ServerConnectionListener.this, null);
    }
  };
}
