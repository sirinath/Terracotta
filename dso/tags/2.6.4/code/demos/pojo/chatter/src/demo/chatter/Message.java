/*
 @COPYRIGHT@
 */
package demo.chatter;

class Message {
    private final String text;
    private final User user;
    private final boolean alreadyDisplayedLocally;

    public Message(User user, String text, boolean displayed) {
        this.user = user;
        this.text = text;
        this.alreadyDisplayedLocally = displayed;
    }

    public String getText() {
        return text;
    }

    public User getUser() {
        return user;
    }

    public boolean wasAlreadyDisplayedLocally() {
      return alreadyDisplayedLocally;
    }
}
