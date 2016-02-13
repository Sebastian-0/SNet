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
 * This class describes an abstract network hook. Override it and add it to
 *  the list of hooks in {@code Network}. The hook will be invoked when
 *  messages of the corresponding type are received.
 * @author Sebastian Hjelm
 */
public abstract class NetworkHook
{
  /**
   * This character is the recommended character to use when separating parts
   *  of the messages sent through the network hooks. By using this character
   *  you will now for certain that the networking system won't run into trouble.
   *  Keep in mind that this character might cause problems if sent as a
   *  part of the message text when not used as a separator!
   */
  public static final String SEPARATOR = "|"; // Same as '\u007C'
  
  private char commandCode_;
  
  /**
   * Creates a network hook with the specified command code, this code must be
   *  unique. The command code is used to identify this network hook.
   * @param commandCode The command code character for this hook
   * @throws IllegalArgumentException If the message separator
   *  ({@link AbstractConnector#SEPARATOR_CHAR}) is passed as the command code
   */
  public NetworkHook(char commandCode)
  {
    if (commandCode == AbstractConnector.SEPARATOR_CHAR)
      throw new IllegalArgumentException("Can't use the message seeparator as the command code");
    commandCode_ = commandCode;
  }
  
  /**
   * This method will be invoked when the server receives a message with a type
   *  that corresponds to this network hook. Note that the first character in
   *  the message text is the command code character.
   * </br>
   * </br>The message passed to this method will be disposed directly after
   *  this method has finished, they are not intended to be stored for further
   *  use.
   * @param network The network used to manage the client/server connection
   * @param message The message that was received
   */
  public abstract void server(Network network, Message message);
  
  /**
   * This method will be invoked when the clients receives a message with a type
   *  that corresponds to this network hook. Note that the first character in
   *  the message text is the command code character.
   * </br>
   * </br>The message passed to this method will be disposed directly after
   *  this method has finished, they are not intended to be stored for further
   *  use.
   * @param network The network used to manage the client/server connection
   * @param message The message that was received
   */
  public abstract void client(Network network, Message message);
  
  /**
   * Returns the command code character used to identify this network hook,
   *  this character must be unique.
   * @return The command code character used to identify this network hook
   */
  public char getCommandCode()
  {
    return commandCode_;
  }
  
  /**
   * Creates a message that can be sent through the network. The text that
   *  is to be sent should be passed through this method before being sent to
   *  the network.
   * @param data The data that should be parsed and returned
   * @return The message to send, parsed and ready to be sent through the network
   */
  public final Message createMessage(String data)
  {
    return Message.createMessage(getCommandCode() + data);
  }
  
  
//  /**
//   * Extracts the message from the specified text. The extraction removes the
//   *  leading command code of the message.
//   * @return The message without the command code
//   */
//  public final String extractMessage(String text)
//  {
//    return text.substring(1);
//  }
}