package fazirul.fyp.dragon.utils;

import fazirul.fyp.dragon.election.GlobalData;
import fazirul.fyp.elements.MessageInterface;
import fazirul.fyp.elements.Node;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;

// message to be sent
public class Message implements MessageInterface {
    private GlobalData data;
    private int senderID;
    private LocalTime timestamp;
    private HashMap<Node, HashSet<Integer>> winnersPerNode;

    public Message(GlobalData data, int senderID, LocalTime timestamp, HashMap<Node, HashSet<Integer>> winnersPerNode) {
        this.data = data;
        this.senderID = senderID;
        this.timestamp = timestamp;
        this.winnersPerNode = winnersPerNode;
    }

    @Override
    public Message clone() {
        return new Message(this.data.clone(), this.senderID, this.timestamp, new HashMap<>(winnersPerNode));
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

    public HashSet<Integer> getWinnersForNode(Node n) {
        return winnersPerNode.get(n);
    }
}
