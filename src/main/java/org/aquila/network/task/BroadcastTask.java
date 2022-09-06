package org.aquila.network.task;

import org.aquila.controller.Controller;
import org.aquila.network.Network;
import org.aquila.network.Peer;
import org.aquila.network.message.Message;
import org.aquila.utils.ExecuteProduceConsume.Task;

public class BroadcastTask implements Task {
    public BroadcastTask() {
    }

    @Override
    public String getName() {
        return "BroadcastTask";
    }

    @Override
    public void perform() throws InterruptedException {
        Controller.getInstance().doNetworkBroadcast();
    }
}
