package fazirul.fyp.dragon.utils;

import fazirul.fyp.dragon.dragonDevice.GlobalData;
import fazirul.fyp.elements.MessageInterface;
import java.time.LocalTime;

public class Message implements MessageInterface {
    private final GlobalData data;
    private final int senderID;
    private final LocalTime timestamp;

    public Message(GlobalData data, int senderID, LocalTime timestamp) {
        this.data = data;
        this.senderID = senderID;
        this.timestamp = timestamp;
    }

    @Override
    public Message clone() {
        return new Message(data.clone(), senderID, timestamp);
    }

    public int getSenderID() {
        return senderID;
    }

    public LocalTime getTimestamp() {
        return timestamp;
    }

    public GlobalData getData() {
        return data;
    }
}
