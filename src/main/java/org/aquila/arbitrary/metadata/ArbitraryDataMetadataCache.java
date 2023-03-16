package org.aquila.arbitrary.metadata;

import org.aquila.repository.DataException;
import org.aquila.utils.Base58;
import org.json.JSONObject;

import java.nio.file.Path;

public class ArbitraryDataMetadataCache extends ArbitraryDataAquilaMetadata {

    private byte[] signature;
    private long timestamp;

    public ArbitraryDataMetadataCache(Path filePath) {
        super(filePath);

    }

    @Override
    protected String fileName() {
        return "cache";
    }

    @Override
    protected void readJson() throws DataException {
        if (this.jsonString == null) {
            throw new DataException("Patch JSON string is null");
        }

        JSONObject cache = new JSONObject(this.jsonString);
        if (cache.has("signature")) {
            String sig = cache.getString("signature");
            if (sig != null) {
                this.signature = Base58.decode(sig);
            }
        }
        if (cache.has("timestamp")) {
            this.timestamp = cache.getLong("timestamp");
        }
    }

    @Override
    protected void buildJson() {
        JSONObject patch = new JSONObject();
        patch.put("signature", Base58.encode(this.signature));
        patch.put("timestamp", this.timestamp);

        this.jsonString = patch.toString(2);
        LOGGER.trace("Cache metadata: {}", this.jsonString);
    }


    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public byte[] getSignature() {
        return this.signature;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

}
