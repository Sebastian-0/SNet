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
 * An interface used in conjunction with {@link Client} to listen for events.
 * @author Sebastian Hjelm
 */
public interface ClientLifecycleListener {
  /**
   * This method is invoked if the client failed to establish a connection to the
   *  server.
   */
  public abstract void failedToEstablishConnection();
  
  /**
   * This method is invoked when the client has connected to a server.
   */
  public abstract void connected();
  
  /**
   * This method is invoked when this computer loses its connection with the
   *  server it is working with.
   * @param reason The reason for the disconnect
   */
  public abstract void disconnected(DisconnectReason reason);
}
