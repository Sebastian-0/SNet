/*
 * Copyright (C) 2016 Sebastian Hjelm
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package network;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;

import sbasicgui.util.BasicUtils;


/**
 * A class that represents and handles a link between the server and a client.
 * @author Sebastian Hjelm
 */
public class ServerListener extends AbstractConnector
{
  private String id_;
  
  private Server server_;
  
  
  /**
   * Creates a {@code ServerListener} and links it with the specified socket and
   *  connection manager.
   * @param manager The connection manager to use
   * @param socket The socket to link this listener to
   * @param server The server this listener belongs to
   */
  public ServerListener(ConnectionManagerInterface manager, Socket socket, Server server)
  {
    super (manager);
    
    generateId();
    
    server_ = server;
    
    socket_ = socket;
    
    isConnected_ = true;
    connectionThread.start();
  }
  
  
  /**
   * Returns the id of this server listener. The id can be used to identify this
   *  client among all other clients. The id is randomly generated when the
   *  listener is created.
   * @return The id of this server listener
   */
  public String getId()
  {
    return id_;
  }
  
  
  void generateId()
  {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < 20; i++)
      b.append(generateChar());
    
    id_ = b.toString();
  }
  
  
  private char generateChar()
  {
    return (char)((('z' - '0') * Math.random()) + '0');
  }
  

  private Thread connectionThread = new Thread() {
    
    {
      setName("Server-Client main connection");
    }
    
    @Override
    public void run()
    {
      // TODO ServerListener; Dela in detta i flera try-statements för att lättare hitta fel?
      try {
        reader_ = new InputStreamReader (socket_.getInputStream (), Charset.forName("UTF-8"));
        writer_ = new OutputStreamWriter(socket_.getOutputStream(), Charset.forName("UTF-8"));
      } catch (IOException e) {
        System.out.println("ServerListener: run(): failed to create streams! Canceling connection...");
        isConnected_ = false;
      }
      
      sendHello();
      
      readThread.start();
      writeThread.start();
      
      while (isConnected_)
      {
        // We lost the connection unexpectedly
        if (socket_.isClosed())
        {
          lostConnection();
          break;
        }
        
        attemptPing();
        try { Thread.sleep(10); } catch (InterruptedException e) { }
      }
      
      writeThread.interrupt();
      
      while (readThread.isAlive() || writeThread.isAlive())
      {
        try { Thread.sleep(10); } catch (InterruptedException e) { }
      }
      
      BasicUtils.closeSilently(reader_);
      BasicUtils.closeSilently(writer_);
      BasicUtils.closeSilently(socket_);
      
      server_.terminated(ServerListener.this);
    }
  };
  
  
  private Thread readThread = new Thread() {
    
    {
      setName("Server-Client read connection");
    }
    
    @Override
    public void run()
    {
      while (isConnected_)
      {
        read();
      }
    }
  };
  
  
  private Thread writeThread = new Thread() {
    
    {
      setName("Server-Client write connection");
    }
    
    @Override
    public void run()
    {
      while (isConnected_)
      {
        if (!hasBeenGreeted_ || messages_.isEmpty())
        {
          lock.lock();
          try
          {
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
