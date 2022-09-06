package org.aquila.controller.arbitrary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aquila.controller.Controller;
import org.aquila.data.arbitrary.ArbitraryFileListResponseInfo;
import org.aquila.data.transaction.ArbitraryTransactionData;
import org.aquila.network.Peer;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.utils.ArbitraryTransactionUtils;
import org.aquila.utils.Base58;
import org.aquila.utils.NTP;

import java.util.*;
import java.util.stream.Collectors;

public class ArbitraryDataFileRequestThread implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(ArbitraryDataFileRequestThread.class);

    public ArbitraryDataFileRequestThread() {

    }

    @Override
    public void run() {
        Thread.currentThread().setName("Arbitrary Data File Request Thread");

        try {
            while (!Controller.isStopping()) {
                Long now = NTP.getTime();
                this.processFileHashes(now);
            }
        } catch (InterruptedException e) {
            // Fall-through to exit thread...
        }
    }

    private void processFileHashes(Long now) throws InterruptedException {
		if (Controller.isStopping()) {
            return;
        }

        ArbitraryDataFileManager arbitraryDataFileManager = ArbitraryDataFileManager.getInstance();
        String signature58 = null;
        String hash58 = null;
        Peer peer = null;
        boolean shouldProcess = false;

        synchronized (arbitraryDataFileManager.arbitraryDataFileHashResponses) {
            if (!arbitraryDataFileManager.arbitraryDataFileHashResponses.isEmpty()) {

                // Sort by lowest number of node hops first
                Comparator<ArbitraryFileListResponseInfo> lowestHopsFirstComparator =
                        Comparator.comparingInt(ArbitraryFileListResponseInfo::getRequestHops);
                arbitraryDataFileManager.arbitraryDataFileHashResponses.sort(lowestHopsFirstComparator);

                Iterator iterator = arbitraryDataFileManager.arbitraryDataFileHashResponses.iterator();
                while (iterator.hasNext()) {
                    if (Controller.isStopping()) {
                        return;
                    }

                    ArbitraryFileListResponseInfo responseInfo = (ArbitraryFileListResponseInfo) iterator.next();
                    if (responseInfo == null) {
                        iterator.remove();
                        continue;
                    }

                    hash58 = responseInfo.getHash58();
                    peer = responseInfo.getPeer();
                    signature58 = responseInfo.getSignature58();
                    Long timestamp = responseInfo.getTimestamp();

                    if (now - timestamp >= ArbitraryDataManager.ARBITRARY_RELAY_TIMEOUT || signature58 == null || peer == null) {
                        // Ignore - to be deleted
                        iterator.remove();
                        continue;
                    }

                    // Skip if already requesting, but don't remove, as we might want to retry later
                    if (arbitraryDataFileManager.arbitraryDataFileRequests.containsKey(hash58)) {
                        // Already requesting - leave this attempt for later
                        continue;
                    }

                    // We want to process this file
                    shouldProcess = true;
                    iterator.remove();
                    break;
                }
            }
        }

        if (!shouldProcess) {
            // Nothing to do
            Thread.sleep(1000L);
            return;
        }

        byte[] hash = Base58.decode(hash58);
        byte[] signature = Base58.decode(signature58);

        // Fetch the transaction data
        try (final Repository repository = RepositoryManager.getRepository()) {
            ArbitraryTransactionData arbitraryTransactionData = ArbitraryTransactionUtils.fetchTransactionData(repository, signature);
            if (arbitraryTransactionData == null) {
                return;
            }

            if (signature == null || hash == null || peer == null || arbitraryTransactionData == null) {
                return;
            }

            LOGGER.debug("Fetching file {} from peer {} via request thread...", hash58, peer);
            arbitraryDataFileManager.fetchArbitraryDataFiles(repository, peer, signature, arbitraryTransactionData, Arrays.asList(hash));

        } catch (DataException e) {
            LOGGER.debug("Unable to process file hashes: {}", e.getMessage());
        }
    }
}
