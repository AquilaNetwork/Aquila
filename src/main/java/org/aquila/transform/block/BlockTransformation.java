package org.aquila.transform.block;

import java.util.List;

import org.aquila.data.at.ATStateData;
import org.aquila.data.block.BlockData;
import org.aquila.data.transaction.TransactionData;

public class BlockTransformation {
    private final BlockData blockData;
    private final List<TransactionData> transactions;
    private final List<ATStateData> atStates;
    private final byte[] atStatesHash;

    /*package*/ BlockTransformation(BlockData blockData, List<TransactionData> transactions, List<ATStateData> atStates) {
        this.blockData = blockData;
        this.transactions = transactions;
        this.atStates = atStates;
        this.atStatesHash = null;
    }

    /*package*/ BlockTransformation(BlockData blockData, List<TransactionData> transactions, byte[] atStatesHash) {
        this.blockData = blockData;
        this.transactions = transactions;
        this.atStates = null;
        this.atStatesHash = atStatesHash;
    }

    public BlockData getBlockData() {
        return blockData;
    }

    public List<TransactionData> getTransactions() {
        return transactions;
    }

    public List<ATStateData> getAtStates() {
        return atStates;
    }

    public byte[] getAtStatesHash() {
        return atStatesHash;
    }
}
