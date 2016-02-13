/*
 * Copyright (C) 2016 Sebastian Hjelm
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package network;

/**
 * An interface used in conjunction with {@code Network} to listen for disconnects.
 * @author Sebastian Hjelm
 */
public interface ConnectionListener
{
  /**
   * This method is invoked if the client failed to establish a connection to the
   *  server (if this is a client), or if the server failed to start (if this
   *  is a server).
   */
  public abstract void failedToStart();
  
  /**
   * This method is invoked when the server has started.
   * @param port The port the server uses
   */
  public abstract void serverStarted(int port);
  
  /**
   * This method is invoked when this server acquires a connection with a client,
   *  if this is a server, otherwise it is invoked when this client has connected
   *  to a server. The id is the id of the server listener that was created to
   *  manage the connection with the client, if this is a server, or
   *  <code>null</code> otherwise.
   * @param id The id of the server listener that was created to handle the client,
   *  or <code>null</code> if this is a client
   */
  public abstract void connected(String id);
  
  /**
   * This method is invoked when this computer loses its connection with the
   *  server it is working with, or if this computer is the server it is called
   *  when the server loses the connection with one of its clients. The id
   *  parameter is the id of the server listener that lost its connection with
   *  its client, if this computer is the server, or <code>null</code> otherwise.
   * </br>
   * </br>The reason should be one of the following:
   * <ul>
   *  <li>{@link ConnectionManagerInterface#REASON_CLOSED}</li>
   *  <li>{@link ConnectionManagerInterface#REASON_TIMEOUT}</li>
   * </ul>
   * @param id The id of the server listener that lost its connection, o
   *  <code>null</code> if this is a client
   * @param reason The reason for the disconnect
   * @param message Additional information, or <code>null</code>
   */
  public abstract void disconnected(String id, byte reason, String message);
}
