package org.aquila.test.minting;

import org.aquila.block.Block;
import org.aquila.data.block.BlockData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.transform.Transformer;
import org.aquila.utils.NTP;
import org.junit.Before;
import org.junit.Test;
import org.aquila.test.common.BlockUtils;
import org.aquila.test.common.Common;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlockTimestampTests extends Common {

    private static class BlockTimestampDataPoint {
        public byte[] minterPublicKey;
        public int minterAccountLevel;
        public long blockTimestamp;
    }

    private static final Random RANDOM = new Random();

    @Before
    public void beforeTest() throws DataException {
        Common.useSettings("test-settings-v2-block-timestamps.json");
        NTP.setFixedOffset(0L);
    }

    @Test
    public void testTimestamps() throws DataException {
        try (final Repository repository = RepositoryManager.getRepository()) {
            Block parentBlock = BlockUtils.mintBlock(repository);
            BlockData parentBlockData = parentBlock.getBlockData();

            // Generate lots of test minters
            List<BlockTimestampDataPoint> dataPoints = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                BlockTimestampDataPoint dataPoint = new BlockTimestampDataPoint();

                dataPoint.minterPublicKey = new byte[Transformer.PUBLIC_KEY_LENGTH];
                RANDOM.nextBytes(dataPoint.minterPublicKey);

                dataPoint.minterAccountLevel = RANDOM.nextInt(5) + 5;

                dataPoint.blockTimestamp = Block.calcTimestamp(parentBlockData, dataPoint.minterPublicKey, dataPoint.minterAccountLevel);

                System.out.printf("[%d] level %d, blockTimestamp %d - parentTimestamp %d = %d%n",
                        i,
                        dataPoint.minterAccountLevel,
                        dataPoint.blockTimestamp,
                        parentBlockData.getTimestamp(),
                        dataPoint.blockTimestamp - parentBlockData.getTimestamp()
                );
            }
        }
    }
}
