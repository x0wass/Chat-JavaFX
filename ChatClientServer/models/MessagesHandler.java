package models;

/**
 * Common Interface to all classes responsible for:
 * 	- Display messages
 * 	- Display users sending messages
 * @author x0wass
 */
public interface MessagesHandler
{
	/**
	 * Adds new message
	 * @param m the message to add
	 */
	public abstract void addMessage(Message m);

	/**
	 * Adds new message (from string)
	 * @param s the text of the message to add
	 */
	public abstract void addMessage(String s);

	/**
	 * Adds a new user
	 * @param user the new user to add
	 */
	public abstract void addUserName(String user);

	/**
	 * Update all messages according to internal filtering policies
	 */
	public abstract void updateMessages();
}
