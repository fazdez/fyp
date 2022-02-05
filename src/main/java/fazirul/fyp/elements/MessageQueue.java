package fazirul.fyp.elements;

import java.util.ArrayList;
import java.util.List;

public class MessageQueue {
    private final List<MessageInterface> messages = new ArrayList<>();

    public synchronized void addMessage(MessageInterface message) {
        messages.add(message);
    }

    public synchronized List<MessageInterface> flush() {
        //create a (shallow) copy
        List<MessageInterface> result = new ArrayList<>(messages);
        messages.clear();
        return result;
    }
}