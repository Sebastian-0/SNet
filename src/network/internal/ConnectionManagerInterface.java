/*
 * Copyright (C) 2016 Sebastian Hjelm
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package network.internal;


/**
 * An interface used in the {@link ClientConnection} and {@link ServerConnectionListener} classes to dispatch
 *  the messages that have been received.
 * @author Sebastian Hjelm
 */
public interface ConnectionManagerInterface
{
  /**
   * The connection closed normally, either the server or the client sent a
   *  message saying the connection was to be terminated.
   */
  public static final byte REASON_CLOSED  = 0;
  /**
   * The server/client connection timed out, either due to a crash or due to a
   *  loss of Internet connection.
   */
  public static final byte REASON_TIMEOUT = 1;
  /**
   * The server/client connection closed unexpectedly, the reason is unknown.
   */
  public static final byte REASON_LOST_CONNECTION = 2;
  
  /**
   * This method is invoked by the client/server when a message has been
   *  received. The message contains information of the source as well as the
   *  text message.
   * @param message The message that was received
   */
  public abstract void receivedMessage(Message message);
  
  /**
   * This method is invoked if the client failed to establish a connection to the
   *  server (if this is a client), or if the server failed to start (if this
   *  is a server).
   * @param server The server that failed to connect (if this is on the server), or <code>null</code>
   * @param client The client that failed to connect (if this is on a client), or <code>null</code>
   */
  public abstract void failedToStart(ServerConnectionListener server, ClientConnection client);

  /**
   * This method is invoked when the server has started.
   * @param port The port the server uses
   */
  public abstract void serverStarted(int port);
  
  /**
   * This method is invoked when a client connects to this server (if this is the
   *  server), or when this client connects to a server (if this is a client)
   * @param connector The connector that resulted from the connection, a {@link ClientConnection}
   *  if this is a client or a {@link ServerConnection} if this is a server
   */
  public abstract void connected(AbstractConnection connector);
  
  /**
   * This method is invoked when the server/client is disconnected. The
   *  {@code reason} specified the cause for the disconnect, and the
   *  {@code message} may contain additional information.
   * </br>
   * </br>The reason should be one of the following:
   * <ul>
   *  <li>{@link #REASON_CLOSED}</li>
   *  <li>{@link #REASON_TIMEOUT}</li>
   * </ul>
   * @param connector The connector that was disconnected
   * @param reason The reason why the serer/client was disconnected
   * @param message Additional information regarding the disconnection, may be
   *  <code>null</code>
   */
  public abstract void disconnected(AbstractConnection connector, byte reason, String message);
}
