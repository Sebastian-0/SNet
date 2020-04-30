/*
 * Copyright (C) 2016 Sebastian Hjelm
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package snet;

import snet.internal.ConnectionManagerInterface.DisconnectReason;

/**
 * An interface used in conjunction with {@link Server} to listen for events.
 * @author Sebastian Hjelm
 */
public interface ServerLifecycleListener {
  /**
   * This method is invoked if the server failed to start.
   */
  public abstract void failedToStart();
  
  /**
   * This method is invoked when the server has started. The port may not be the
   *  one you specified depending on whether or not it was already occupied.
   * @param port The port the server uses
   */
  public abstract void serverStarted(int port);
  
  /**
   * This method is invoked when the server acquires a connection with a client.
   *  The id is the id of the server connection that was created to
   *  manage the connection with the client.
   * @param id The id of the server connection that was created to handle the client
   */
  public abstract void connected(String id);
  
  /**
   * This method is called when the server loses its connection with one of its
   *  clients. The id parameter is the id of the server connection that lost its
   *  connection with its client.
   * @param id The id of the server connection that lost its connection
   * @param reason The reason for the disconnect
   */
  public abstract void disconnected(String id, DisconnectReason reason);
}
