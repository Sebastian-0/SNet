/*
 * Copyright (C) 2016 Sebastian Hjelm
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package snet;

import snet.internal.ConnectionManagerInterface;

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
   * </br>
   * </br>The reason should be one of the following:
   * <ul>
   *  <li>{@link ConnectionManagerInterface#REASON_CLOSED}</li>
   *  <li>{@link ConnectionManagerInterface#REASON_TIMEOUT}</li>
   * </ul>
   * @param reason The reason for the disconnect
   * @param message Additional information, or <code>null</code>
   */
  public abstract void disconnected(byte reason, String message);
}
