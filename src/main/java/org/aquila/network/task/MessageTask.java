package org.aquila.network.task;

import org.aquila.network.Network;
import org.aquila.network.Peer;
import org.aquila.network.message.Message;
import org.aquila.utils.ExecuteProduceConsume.Task;

public class MessageTask implements Task {
    private final Peer peer;
    private final Message nextMessage;
    private final String name;

    public MessageTask(Peer peer, Message nextMessage) {
        this.peer = peer;
        this.nextMessage = nextMessage;
        this.name = "MessageTask::" + peer + "::" + nextMessage.getType();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void perform() throws InterruptedException {
        Network.getInstance().onMessage(peer, nextMessage);
    }
}
