package org.aquila.utils;

import java.util.List;

import org.aquila.data.at.ATStateData;
import org.aquila.data.block.BlockData;
import org.aquila.data.transaction.TransactionData;
import org.aquila.repository.BlockArchiveReader;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.transform.block.BlockTransformation;

public class BlockArchiveUtils {

    /**
     * importFromArchive
     * <p>
     * Reads the requested block range from the archive
     * and imports the BlockData and AT state data hashes
     * This can be used to convert a block archive back
     * into the HSQLDB, in order to make it SQL-compatible
     * again.
     * <p>
     * This is only fully compatible with archives that use
     * serialization version 1. For version 2 (or above),
     * we are unable to import individual AT states as we
     * only have a single combined hash, so the use cases
     * for this are greatly limited.
     * <p>
     * A version 1 archive should ultimately be rebuildable
     * via a resync or reindex from genesis, allowing
     * access to this feature once again.
     * <p>
     * Note: calls discardChanges() and saveChanges(), so
     * make sure that you commit any existing repository
     * changes before calling this method.
     *
     * @param startHeight The earliest block to import
     * @param endHeight The latest block to import
     * @param repository A clean repository session
     * @throws DataException
     */
    public static void importFromArchive(int startHeight, int endHeight, Repository repository) throws DataException {
        repository.discardChanges();
        final int requestedRange = endHeight+1-startHeight;

        List<BlockTransformation> blockInfoList = BlockArchiveReader.getInstance().fetchBlocksFromRange(startHeight, endHeight);

        // Ensure that we have received all of the requested blocks
        if (blockInfoList == null || blockInfoList.isEmpty()) {
            throw new IllegalStateException("No blocks found when importing from archive");
        }
        if (blockInfoList.size() != requestedRange) {
            throw new IllegalStateException("Non matching block count when importing from archive");
        }
        BlockTransformation firstBlock = blockInfoList.get(0);
        if (firstBlock == null || firstBlock.getBlockData().getHeight() != startHeight) {
            throw new IllegalStateException("Non matching first block when importing from archive");
        }
        if (blockInfoList.size() > 0) {
            BlockTransformation lastBlock = blockInfoList.get(blockInfoList.size() - 1);
            if (lastBlock == null || lastBlock.getBlockData().getHeight() != endHeight) {
                throw new IllegalStateException("Non matching last block when importing from archive");
            }
        }

        // Everything seems okay, so go ahead with the import
        for (BlockTransformation blockInfo : blockInfoList) {
            try {
                // Save block
                repository.getBlockRepository().save(blockInfo.getBlockData());

                // Save AT state data hashes
                if (blockInfo.getAtStates() != null) {
                    for (ATStateData atStateData : blockInfo.getAtStates()) {
                        atStateData.setHeight(blockInfo.getBlockData().getHeight());
                        repository.getATRepository().save(atStateData);
                    }
                }
                else {
                    // We don't have AT state hashes, so we are only importing a partial state.
                    // This can still be useful to allow orphaning to very old blocks, when we
                    // need to access other chainstate info (such as balances) at an earlier block.
                    // In order to do this, the orphan process must be temporarily adjusted to avoid
                    // orphaning AT states, as it will otherwise fail due to having no previous state.
                }

            } catch (DataException e) {
                repository.discardChanges();
                throw new IllegalStateException("Unable to import blocks from archive");
            }
        }
        repository.saveChanges();
    }

}
