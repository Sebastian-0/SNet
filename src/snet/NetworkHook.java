/*
 * Copyright (C) 2016 Sebastian Hjelm
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package snet;

import java.util.HashMap;
import java.util.Map;

import snet.internal.AbstractConnection;
import snet.internal.Message;

/**
 * This class describes an abstract network hook. Override it and add it to
 *  the list of hooks in {@code Network}. The hook will be invoked when
 *  messages of the corresponding type are received.
 * @author Sebastian Hjelm
 */
public abstract class NetworkHook<E> {
  /**
   * This character is the recommended character to use when separating parts
   *  of the messages sent through the network hooks. By using this character
   *  you will now for certain that the networking system won't run into trouble.
   *  Keep in mind that this character might cause problems if sent as a
   *  part of the message text when not used as a separator!
   */
  public static final String SEPARATOR = "\u65535";
  
  private char commandCode;
  private Map<Integer, E> subscribedObjects;
  
  /**
   * Creates a network hook with the specified command code, this code must be
   *  unique. The command code is used to identify this network hook.
   * @param commandCode The command code character for this hook
   * @throws IllegalArgumentException If the message separator
   *  ({@link AbstractConnection#SEPARATOR_CHAR}) is passed as the command code
   */
  public NetworkHook(char commandCode) {
    if (commandCode == AbstractConnection.SEPARATOR_CHAR)
      throw new IllegalArgumentException("Can't use the message seeparator as the command code");
    this.commandCode = commandCode;
    
    subscribedObjects = new HashMap<Integer, E>();
  }
  
  /**
   * This method will be invoked when the server receives a message with a type
   *  that corresponds to this network hook. Note that the first character in
   *  the message text is the command code character.
   * </br>
   * </br>The message passed to this method will be disposed directly after
   *  this method has finished, they are not intended to be stored for further
   *  use.
   * @param server The network used to manage the client/server connection
   * @param message The message that was received
   */
  public abstract void server(Server server, Message message);
  
  /**
   * This method will be invoked when the clients receives a message with a type
   *  that corresponds to this network hook. Note that the first character in
   *  the message text is the command code character.
   * </br>
   * </br>The message passed to this method will be disposed directly after
   *  this method has finished, they are not intended to be stored for further
   *  use.
   * @param client The network used to manage the client/server connection
   * @param message The message that was received
   */
  public abstract void client(Client client, Message message);
  
  /**
   * Returns the command code character used to identify this network hook,
   *  this character must be unique.
   * @return The command code character used to identify this network hook
   */
  public char getCommandCode() {
    return commandCode;
  }
  
  /**
   * Attaches the specified object as a subscriber for messages received by this
   *  network hook. The id is used to target this specific subscriber when 
   *  sending messages. Any existing subscriber with the same id will be overwritten.
   * @param id The id for this subscriber to use
   * @param subscriber The object that wants to subscribe
   */
  public void subscribe(int id, E subscriber) {
  	subscribedObjects.put(id, subscriber);
  }
  /**
   * Removes any subscriber that uses the specified id.
   * @param id The id of the subscriber to remove
   * @return The subscriber that was removed, or <code>null</code>
   */
  public E unsubscribe(int id) {
  	return subscribedObjects.remove(id);
  }
  /**
   * Returns the subscriber associated with the specified id.
   * @param id The id of the subscriber to return
   * @return The subscriber associated with the specified id
   */
  protected E getSubscriber(int id) {
  	return subscribedObjects.get(id);
  }
  
  /**
   * Creates a message that can be sent through the network. The text that
   *  is to be sent should be passed through this method before being sent to
   *  the network. The targeted subscriber id defaults to 0. 
   * @param data The data that should be parsed and returned
   * @return The message to send, parsed and ready to be sent through the network
   */
  public final Message createMessage(String data) {
    return createMessage(data, 0);
  }
  
  /**
   * Creates a message that can be sent through the network. The text that
   *  is to be sent should be passed through this method before being sent to
   *  the network.
   * @param data The data that should be parsed and returned
   * @param targetedSubscriberId The id of subscriber this message is intended for
   * @return The message to send, parsed and ready to be sent through the network
   */
  public final Message createMessage(String data, int targetedSubscriberId) {
    return Message.createMessage(Character.toString(getCommandCode()) + (char)targetedSubscriberId + data);
  }
}