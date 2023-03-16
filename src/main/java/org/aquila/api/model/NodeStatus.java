package org.aquila.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.aquila.controller.Controller;
import org.aquila.controller.OnlineAccountsManager;
import org.aquila.controller.Synchronizer;
import org.aquila.network.Network;

@XmlAccessorType(XmlAccessType.FIELD)
public class NodeStatus {

	public final boolean isMintingPossible;
	public final boolean isSynchronizing;

	// Not always present
	public final Integer syncPercent;

	public final int numberOfConnections;

	public final int height;

	public NodeStatus() {
		this.isMintingPossible = OnlineAccountsManager.getInstance().hasActiveOnlineAccountSignatures();

		this.syncPercent = Synchronizer.getInstance().getSyncPercent();
		this.isSynchronizing = Synchronizer.getInstance().isSynchronizing();

		this.numberOfConnections = Network.getInstance().getImmutableHandshakedPeers().size();

		this.height = Controller.getInstance().getChainHeight();
	}

}
