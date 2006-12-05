/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
/**
@COPYRIGHT@
 */
package demo.consolechat;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class Chatter implements MessageListener {

    private String username;
    private MessageObject message = new MessageObject();

    public Chatter(String username) {
        this.username = username;
    }

    public void doChat() throws IOException {
         synchronized (message) {
            message.addListener(this);
        }
        while (true) {
            String formingMessage = "";
            char c = (char) System.in.read();
            while (c != '\n') {
               formingMessage += c;
                c = (char) System.in.read();
            }
            synchronized (message) {
                if (formingMessage.equals("quit\r") ||
                    formingMessage.equals("quit")) {
                    message.removeListener(this);
                    System.exit(1);  
                } else {
                    message.setMessage(username, formingMessage);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("\n\nusage: Chatter <username>\n");
            System.exit(1);
        }
        System.out.println("\n\nPLEASE ENTER TEXT...\n");
        Chatter chatter = new Chatter(args[0]);
        chatter.doChat();
    }

    public void processMessage() {
        synchronized (message) {
            System.out.println(message.getMessage());
        }
    }
}

class MessageObject {
    String message = "hi";
    String username = "kalai";

    transient List listeners = new ArrayList();

    public MessageObject() {
    }

    public String getMessage() {
        return username + " : " +message;
    }

    public void setMessage(String username, String message) {
        this.username = username;
        this.message = message;
        fireListeners();
    }

    public void addListener(MessageListener l) {
        listeners.add(l);
    }

    public void removeListener(MessageListener l) {
        listeners.remove(l);
    }

    private void fireListeners() {
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            MessageListener messageListener = (MessageListener) iterator.next();
            messageListener.processMessage();
        }
    }

}

interface MessageListener {
    public void processMessage();
}
