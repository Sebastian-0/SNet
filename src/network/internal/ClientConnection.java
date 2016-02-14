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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;

import sbasicgui.util.BasicUtils;

public class ClientConnection extends AbstractConnection {
  private volatile String host_;
  private volatile int    port_;
  
  private volatile boolean couldNotConnect_;
  
  
  public ClientConnection(ConnectionManagerInterface manager, String host, int port) {
    super (manager);
    
    host_ = host;
    port_ = port;
    
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
    if (!isConnected_)
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
        socket_ = new Socket(host_, port_);
        socket_.setTcpNoDelay(true);
        reader_ = new InputStreamReader (socket_.getInputStream (), Charset.forName("UTF-8"));
        writer_ = new OutputStreamWriter(socket_.getOutputStream(), Charset.forName("UTF-8"));
        isConnected_ = true;
      } catch (IOException e) {
        System.out.println("Client: <init>(): Unable to create socket! Host unreachable!");
        couldNotConnect_ = true;
        manager_.failedToStart(null, ClientConnection.this);
        return;
      }
      
      sendHello();
      
      readThread.start();
      writeThread.start();
      
      while (isConnected_) {
        // We lost the connection unexpectedly
        if (socket_.isClosed()) {
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
      
      BasicUtils.closeSilently(reader_);
      BasicUtils.closeSilently(writer_);
      BasicUtils.closeSilently(socket_);
    }
  };
  
  
  private Thread readThread = new Thread() {
    
    {
      setName("Client-Server read connection");
    }
    
    @Override
    public void run() {
      while (isConnected_) {
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
      while (isConnected_) {
        if (!hasBeenGreeted_ || messages_.isEmpty()) {
          lock.lock();
          try {
            condition.await();
          }
          catch (InterruptedException e1) { }
          lock.unlock();
        }
        
        if (hasBeenGreeted_)
          write();
      }
    }
  };
}
