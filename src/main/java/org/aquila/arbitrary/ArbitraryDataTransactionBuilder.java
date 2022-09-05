package org.aquila.arbitrary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aquila.arbitrary.exception.MissingDataException;
import org.aquila.arbitrary.ArbitraryDataFile.ResourceIdType;
import org.aquila.arbitrary.ArbitraryDataDiff.*;
import org.aquila.arbitrary.metadata.ArbitraryDataMetadataPatch;
import org.aquila.arbitrary.metadata.ArbitraryDataTransactionMetadata;
import org.aquila.arbitrary.misc.Category;
import org.aquila.arbitrary.misc.Service;
import org.aquila.crypto.Crypto;
import org.aquila.data.PaymentData;
import org.aquila.data.transaction.ArbitraryTransactionData;
import org.aquila.data.transaction.ArbitraryTransactionData.*;
import org.aquila.data.transaction.BaseTransactionData;
import org.aquila.group.Group;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.transaction.ArbitraryTransaction;
import org.aquila.transaction.Transaction;
import org.aquila.transform.Transformer;
import org.aquila.utils.Base58;
import org.aquila.utils.FilesystemUtils;
import org.aquila.utils.NTP;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class ArbitraryDataTransactionBuilder {

    private static final Logger LOGGER = LogManager.getLogger(ArbitraryDataTransactionBuilder.class);

    // Min transaction version required
    private static final int MIN_TRANSACTION_VERSION = 5;

    // Maximum number of PATCH layers allowed
    private static final int MAX_LAYERS = 10;
    // Maximum size difference (out of 1) allowed for PATCH transactions
    private static final double MAX_SIZE_DIFF = 0.2f;
    // Maximum proportion of files modified relative to total
    private static final double MAX_FILE_DIFF = 0.5f;

    private final String publicKey58;
    private final Path path;
    private final String name;
    private Method method;
    private final Service service;
    private final String identifier;
    private final Repository repository;

    // Metadata
    private final String title;
    private final String description;
    private final List<String> tags;
    private final Category category;

    private int chunkSize = ArbitraryDataFile.CHUNK_SIZE;

    private ArbitraryTransactionData arbitraryTransactionData;
    private ArbitraryDataFile arbitraryDataFile;

    public ArbitraryDataTransactionBuilder(Repository repository, String publicKey58, Path path, String name,
                                           Method method, Service service, String identifier,
                                           String title, String description, List<String> tags, Category category) {
        this.repository = repository;
        this.publicKey58 = publicKey58;
        this.path = path;
        this.name = name;
        this.method = method;
        this.service = service;

        // If identifier is a blank string, or reserved keyword "default", treat it as null
        if (identifier == null || identifier.equals("") || identifier.equals("default")) {
            identifier = null;
        }
        this.identifier = identifier;

        // Metadata (optional)
        this.title = ArbitraryDataTransactionMetadata.limitTitle(title);
        this.description = ArbitraryDataTransactionMetadata.limitDescription(description);
        this.tags = ArbitraryDataTransactionMetadata.limitTags(tags);
        this.category = category;
    }

    public void build() throws DataException {
        try {
            this.preExecute();
            this.checkMethod();
            this.createTransaction();
        }
        finally {
            this.postExecute();
        }
    }

    private void preExecute() {

    }

    private void postExecute() {

    }

    private void checkMethod() throws DataException {
        if (this.method == null) {
            // We need to automatically determine the method
            this.method = this.determineMethodAutomatically();
        }
    }

    private Method determineMethodAutomatically() throws DataException {
        ArbitraryDataReader reader = new ArbitraryDataReader(this.name, ResourceIdType.NAME, this.service, this.identifier);
        try {
            reader.loadSynchronously(true);
        } catch (Exception e) {
            // Catch all exceptions if the existing resource cannot be loaded first time
            // In these cases it's simplest to just use a PUT transaction
            return Method.PUT;
        }

        // Get existing metadata and see if it matches the new metadata
        ArbitraryDataResource resource = new ArbitraryDataResource(this.name, ResourceIdType.NAME, this.service, this.identifier);
        ArbitraryDataTransactionMetadata existingMetadata = resource.getLatestTransactionMetadata();

        try {
            // Check layer count
            int layerCount = reader.getLayerCount();
            if (layerCount >= MAX_LAYERS) {
                LOGGER.info("Reached maximum layer count ({} / {}) - using PUT", layerCount, MAX_LAYERS);
                return Method.PUT;
            }

            // Check size of differences between this layer and previous layer
            ArbitraryDataCreatePatch patch = new ArbitraryDataCreatePatch(reader.getFilePath(), this.path, reader.getLatestSignature());
            try {
                patch.create();
            }
            catch (DataException | IOException e) {
                // Handle matching states separately, as it's best to block transactions with duplicate states
                if (e.getMessage().equals("Current state matches previous state. Nothing to do.")) {
                    // Only throw an exception if the metadata is also identical, as well as the data
                    if (this.isMetadataEqual(existingMetadata)) {
                        throw new DataException(e.getMessage());
                    }
                }

                LOGGER.info("Caught exception when creating patch: {}", e.getMessage());
                LOGGER.info("Unable to load existing resource - using PUT to overwrite it.");
                return Method.PUT;
            }

            long diffSize = FilesystemUtils.getDirectorySize(patch.getFinalPath());
            long existingStateSize = FilesystemUtils.getDirectorySize(reader.getFilePath());
            double difference = (double) diffSize / (double) existingStateSize;
            if (difference > MAX_SIZE_DIFF) {
                LOGGER.info("Reached maximum difference ({} / {}) - using PUT", difference, MAX_SIZE_DIFF);
                return Method.PUT;
            }

            // Check number of modified files
            ArbitraryDataMetadataPatch metadata = patch.getMetadata();
            int totalFileCount = patch.getTotalFileCount();
            int differencesCount = metadata.getFileDifferencesCount();
            difference = (double) differencesCount / (double) totalFileCount;
            if (difference > MAX_FILE_DIFF) {
                LOGGER.info("Reached maximum file differences ({} / {}) - using PUT", difference, MAX_FILE_DIFF);
                return Method.PUT;
            }

            // Check the patch types
            // Limit this check to single file resources only for now
            boolean atLeastOnePatch = false;
            if (totalFileCount == 1) {
                for (ModifiedPath path : metadata.getModifiedPaths()) {
                    if (path.getDiffType() != DiffType.COMPLETE_FILE) {
                        atLeastOnePatch = true;
                    }
                }
            }
            if (!atLeastOnePatch) {
                LOGGER.info("Patch consists of complete files only - using PUT");
                return Method.PUT;
            }

            // State is appropriate for a PATCH transaction
            return Method.PATCH;
        }
        catch (IOException e) {
            // IMPORTANT: Don't catch DataException here, as they must be passed to the caller
            LOGGER.info("Caught exception: {}", e.getMessage());
            LOGGER.info("Unable to load existing resource - using PUT to overwrite it.");
            return Method.PUT;
        }
    }

    private void createTransaction() throws DataException {
        arbitraryDataFile = null;
        try {
            Long now = NTP.getTime();
            if (now == null) {
                throw new DataException("NTP time not synced yet");
            }

            // Ensure that this chain supports transactions necessary for complex arbitrary data
            int transactionVersion = Transaction.getVersionByTimestamp(now);
            if (transactionVersion < MIN_TRANSACTION_VERSION) {
                throw new DataException("Transaction version unsupported on this blockchain.");
            }

            if (publicKey58 == null || path == null) {
                throw new DataException("Missing public key or path");
            }
            byte[] creatorPublicKey = Base58.decode(publicKey58);
            final String creatorAddress = Crypto.toAddress(creatorPublicKey);
            byte[] lastReference = repository.getAccountRepository().getLastReference(creatorAddress);
            if (lastReference == null) {
                // Use a random last reference on the very first transaction for an account
                // Code copied from CrossChainResource.buildAtMessage()
                // We already require PoW on all arbitrary transactions, so no additional logic is needed
                Random random = new Random();
                lastReference = new byte[Transformer.SIGNATURE_LENGTH];
                random.nextBytes(lastReference);
            }

            Compression compression = Compression.ZIP;

            // FUTURE? Use zip compression for directories, or no compression for single files
            // Compression compression = (path.toFile().isDirectory()) ? Compression.ZIP : Compression.NONE;

            ArbitraryDataWriter arbitraryDataWriter = new ArbitraryDataWriter(path, name, service, identifier, method,
                    compression, title, description, tags, category);
            try {
                arbitraryDataWriter.setChunkSize(this.chunkSize);
                arbitraryDataWriter.save();
            } catch (IOException | DataException | InterruptedException | RuntimeException | MissingDataException e) {
                LOGGER.info("Unable to create arbitrary data file: {}", e.getMessage());
                throw new DataException(e.getMessage());
            }

            // Get main file
            arbitraryDataFile = arbitraryDataWriter.getArbitraryDataFile();
            if (arbitraryDataFile == null) {
                throw new DataException("Arbitrary data file is null");
            }

            // Get chunks metadata file
            ArbitraryDataFile metadataFile = arbitraryDataFile.getMetadataFile();
            if (metadataFile == null && arbitraryDataFile.chunkCount() > 1) {
                throw new DataException(String.format("Chunks metadata data file is null but there are %d chunks", arbitraryDataFile.chunkCount()));
            }

            String digest58 = arbitraryDataFile.digest58();
            if (digest58 == null) {
                LOGGER.error("Unable to calculate file digest");
                throw new DataException("Unable to calculate file digest");
            }

            final BaseTransactionData baseTransactionData = new BaseTransactionData(now, Group.NO_GROUP,
                    lastReference, creatorPublicKey, 0L, null);
            final int size = (int) arbitraryDataFile.size();
            final int version = 5;
            final int nonce = 0;
            byte[] secret = arbitraryDataFile.getSecret();
            final ArbitraryTransactionData.DataType dataType = ArbitraryTransactionData.DataType.DATA_HASH;
            final byte[] digest = arbitraryDataFile.digest();
            final byte[] metadataHash = (metadataFile != null) ? metadataFile.getHash() : null;
            final List<PaymentData> payments = new ArrayList<>();

            ArbitraryTransactionData transactionData = new ArbitraryTransactionData(baseTransactionData,
                    version, service, nonce, size, name, identifier, method,
                    secret, compression, digest, dataType, metadataHash, payments);

            this.arbitraryTransactionData = transactionData;

        } catch (DataException e) {
            if (arbitraryDataFile != null) {
                arbitraryDataFile.deleteAll();
            }
            throw(e);
        }

    }

    private boolean isMetadataEqual(ArbitraryDataTransactionMetadata existingMetadata) {
        if (!Objects.equals(existingMetadata.getTitle(), this.title)) {
            return false;
        }
        if (!Objects.equals(existingMetadata.getDescription(), this.description)) {
            return false;
        }
        if (!Objects.equals(existingMetadata.getCategory(), this.category)) {
            return false;
        }
        if (!Objects.equals(existingMetadata.getTags(), this.tags)) {
            return false;
        }
        return true;
    }

    public void computeNonce() throws DataException {
        if (this.arbitraryTransactionData == null) {
            throw new DataException("Arbitrary transaction data is required to compute nonce");
        }

        ArbitraryTransaction transaction = (ArbitraryTransaction) Transaction.fromData(repository, this.arbitraryTransactionData);
        LOGGER.info("Computing nonce...");
        transaction.computeNonce();

        Transaction.ValidationResult result = transaction.isValidUnconfirmed();
        if (result != Transaction.ValidationResult.OK) {
            arbitraryDataFile.deleteAll();
            throw new DataException(String.format("Arbitrary transaction invalid: %s", result));
        }
        LOGGER.info("Transaction is valid");
    }

    public ArbitraryTransactionData getArbitraryTransactionData() {
        return this.arbitraryTransactionData;
    }

    public ArbitraryDataFile getArbitraryDataFile() {
        return this.arbitraryDataFile;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

}
