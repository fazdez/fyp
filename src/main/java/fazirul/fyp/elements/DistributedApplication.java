package fazirul.fyp.elements;

import fazirul.fyp.dragon.utils.Message;

import java.util.ArrayList;
import java.util.List;

public abstract class DistributedApplication extends Thread {
    protected List<DistributedApplication> neighbours = new ArrayList<>();
    protected MessageQueue incomingMessages = new MessageQueue();
    protected boolean ended = false;

    @Override
    public void run() {
        initialize();
        while (!ended) {
            orchestrate();
        }
        postProcessing();
    }

    public void addToQueue(MessageInterface message) {
        new Thread(() -> incomingMessages.addMessage(message)).start();
    }

    public void addNeighbour(DistributedApplication neighbour) {
        this.neighbours.add(neighbour);
    }

    protected void broadcast(MessageInterface message) {
        for (DistributedApplication n: this.neighbours) {
            n.addToQueue(message.clone());
        }
    }

    // abstract methods
    protected abstract void orchestrate();

    //this is to initialize the app
    protected abstract void initialize();

    protected abstract void postProcessing();
}