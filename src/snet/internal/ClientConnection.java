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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;

import sutilities.BasicUtils;

public class ClientConnection extends AbstractConnection {
  private volatile String host;
  private volatile int    port;
  
  private volatile boolean couldNotConnect_;
  
  
  public ClientConnection(ConnectionManagerInterface manager, String host, int port) {
    super (manager);
    
    this.host = host;
    this.port = port;
    
    connectionThread.start();
  }
  
  /**
   * Returns true if this client failed to connect to the server.
   * @return True if this client failed to connect to the server
   */
  public boolean couldNotConnect() {
    return couldNotConnect_;
  }
  
  @Override
  public void stop() {
    super.stop();
    if (!isConnected)
      connectionThread.interrupt();
  }
  
  
  private Thread connectionThread = new Thread() {
    
    {
      setName("Client-Server main connection");
    }
    
    @Override
    public void run() {
      // Attempt connection
      // TODO Client; Dela in detta i flera try-statements för att lättare hitta fel?
      try {
        socket = new Socket(host, port);
        socket.setTcpNoDelay(true);
        reader = new InputStreamReader (socket.getInputStream (), Charset.forName("UTF-8"));
        writer = new OutputStreamWriter(socket.getOutputStream(), Charset.forName("UTF-8"));
        isConnected = true;
      } catch (IOException e) {
        System.out.println("Client: <init>(): Unable to create socket! Host unreachable!");
        couldNotConnect_ = true;
        connectionManager.failedToStart(null, ClientConnection.this);
        return;
      }
      
      sendHello();
      
      readThread.start();
      writeThread.start();
      
      while (isConnected) {
        // We lost the connection unexpectedly
        if (socket.isClosed()) {
          lostConnection();
          break;
        }
        
        attemptPing();
        try { Thread.sleep(10); } catch (InterruptedException e) { }
      }
      
      writeThread.interrupt();
      
      while (readThread.isAlive() || writeThread.isAlive()) {
        try { Thread.sleep(10); } catch (InterruptedException e) { }
      }
      
      BasicUtils.closeSilently(reader);
      BasicUtils.closeSilently(writer);
      BasicUtils.closeSilently(socket);
    }
  };
  
  
  private Thread readThread = new Thread() {
    
    {
      setName("Client-Server read connection");
    }
    
    @Override
    public void run() {
      while (isConnected) {
        read();
      }
    }
  };
  
  
  private Thread writeThread = new Thread() {
    
    {
      setName("Client-Server write connection");
    }
    
    @Override
    public void run() {
      while (isConnected) {
        if (!hasBeenGreeted || messages.isEmpty()) {
          lock.lock();
          try {
            condition.await();
          }
          catch (InterruptedException e1) { }
          lock.unlock();
        }
        
        if (hasBeenGreeted)
          write();
      }
    }
  };
}
