/*
 @COPYRIGHT@
 */
package demo.chatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.swing.DefaultListModel;

/**
 *  Description of the Class
 *
 *@author    Terracotta, Inc.
 */
class ChatManager {
	private transient ChatListener listener;

	private List messages;

	private DefaultListModel listModel;

	private Map map;

	public ChatManager() {
		listModel = new DefaultListModel();
		messages = new Vector();
		map = new HashMap();
	}

	public Message[] getMessages() {
		return (Message[]) messages.toArray(new Message[0]);
	}

	public Object[] getCurrentUsers() {
		synchronized (listModel) {
			return listModel.toArray();
		}
	}

	public void send(User user, String message) {
		Message msg = new Message(user, message);
		synchronized (messages) {
			messages.add(msg);
		}
		sendNewMessageEvent(msg);
	}

	public void registerUser(User user) {
		this.listener = user;
		synchronized (listModel) {
			listModel.addElement(user);
			map.put(user.getNodeId(), user);
		}
		sendNewUserEvent(user.getName());
	}

	public String removeUser(String nodeId) {
		synchronized (listModel) {
			if (map.containsKey(nodeId)) {
				User removedUser = (User) map.get(nodeId);
				listModel.removeElement(removedUser);
				return removedUser.getName();
			} else {
				return "";
			}
		}
	}

	private synchronized void sendNewUserEvent(String username) {
		this.listener.newUser(username);
	}

	private synchronized void sendNewMessageEvent(Message message) {
		this.listener.newMessage(message);
	}
}
