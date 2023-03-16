package org.aquila.data.arbitrary;

import org.aquila.network.Peer;

public class ArbitraryFileListResponseInfo extends  ArbitraryRelayInfo {

    public ArbitraryFileListResponseInfo(String hash58, String signature58, Peer peer, Long timestamp, Long requestTime, Integer requestHops) {
        super(hash58, signature58, peer, timestamp, requestTime, requestHops);
    }

}
