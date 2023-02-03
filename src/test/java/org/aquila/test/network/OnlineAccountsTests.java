package org.aquila.test.network;

import com.google.common.primitives.Ints;
import io.druid.extendedset.intset.ConciseSet;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.aquila.block.Block;
import org.aquila.block.BlockChain;
import org.aquila.controller.BlockMinter;
import org.aquila.data.network.OnlineAccountData;
import org.aquila.network.message.*;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.settings.Settings;
import org.aquila.test.common.AccountUtils;
import org.aquila.test.common.Common;
import org.aquila.transform.Transformer;
import org.aquila.utils.Base58;
import org.aquila.utils.NTP;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class OnlineAccountsTests extends Common {

    private static final Random RANDOM = new Random();
    static {
        // This must go before any calls to LogManager/Logger
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

        Security.insertProviderAt(new BouncyCastleProvider(), 0);
        Security.insertProviderAt(new BouncyCastleJsseProvider(), 1);
    }

    @Before
    public void beforeTest() throws DataException, IOException {
        Common.useSettingsAndDb("test-settings-v2-no-sig-agg.json", false);
        NTP.setFixedOffset(Settings.getInstance().getTestNtpOffset());
    }


    @Test
    public void testGetOnlineAccountsV2() throws MessageException {
        List<OnlineAccountData> onlineAccountsOut = generateOnlineAccounts(false);

        Message messageOut = new GetOnlineAccountsV2Message(onlineAccountsOut);

        byte[] messageBytes = messageOut.toBytes();
        ByteBuffer byteBuffer = ByteBuffer.wrap(messageBytes);

        GetOnlineAccountsV2Message messageIn = (GetOnlineAccountsV2Message) Message.fromByteBuffer(byteBuffer);

        List<OnlineAccountData> onlineAccountsIn = messageIn.getOnlineAccounts();

        assertEquals("size mismatch", onlineAccountsOut.size(), onlineAccountsIn.size());
        assertTrue("accounts mismatch", onlineAccountsIn.containsAll(onlineAccountsOut));

        Message oldMessageOut = new GetOnlineAccountsMessage(onlineAccountsOut);
        byte[] oldMessageBytes = oldMessageOut.toBytes();

        long numTimestamps = onlineAccountsOut.stream().mapToLong(OnlineAccountData::getTimestamp).sorted().distinct().count();

        System.out.println(String.format("For %d accounts split across %d timestamp%s: old size %d vs new size %d",
                onlineAccountsOut.size(),
                numTimestamps,
                numTimestamps != 1 ? "s" : "",
                oldMessageBytes.length,
                messageBytes.length));
    }

    @Test
    public void testOnlineAccountsV2() throws MessageException {
        List<OnlineAccountData> onlineAccountsOut = generateOnlineAccounts(true);

        Message messageOut = new OnlineAccountsV2Message(onlineAccountsOut);

        byte[] messageBytes = messageOut.toBytes();
        ByteBuffer byteBuffer = ByteBuffer.wrap(messageBytes);

        OnlineAccountsV2Message messageIn = (OnlineAccountsV2Message) Message.fromByteBuffer(byteBuffer);

        List<OnlineAccountData> onlineAccountsIn = messageIn.getOnlineAccounts();

        assertEquals("size mismatch", onlineAccountsOut.size(), onlineAccountsIn.size());
        assertTrue("accounts mismatch", onlineAccountsIn.containsAll(onlineAccountsOut));

        Message oldMessageOut = new OnlineAccountsMessage(onlineAccountsOut);
        byte[] oldMessageBytes = oldMessageOut.toBytes();

        long numTimestamps = onlineAccountsOut.stream().mapToLong(OnlineAccountData::getTimestamp).sorted().distinct().count();

        System.out.println(String.format("For %d accounts split across %d timestamp%s: old size %d vs new size %d",
                onlineAccountsOut.size(),
                numTimestamps,
                numTimestamps != 1 ? "s" : "",
                oldMessageBytes.length,
                messageBytes.length));
    }

    private List<OnlineAccountData> generateOnlineAccounts(boolean withSignatures) {
        List<OnlineAccountData> onlineAccounts = new ArrayList<>();

        int numTimestamps = RANDOM.nextInt(2) + 1; // 1 or 2

        for (int t = 0; t < numTimestamps; ++t) {
            int numAccounts = RANDOM.nextInt(3000);

            for (int a = 0; a < numAccounts; ++a) {
                byte[] sig = null;
                if (withSignatures) {
                    sig = new byte[Transformer.SIGNATURE_LENGTH];
                    RANDOM.nextBytes(sig);
                }

                byte[] pubkey = new byte[Transformer.PUBLIC_KEY_LENGTH];
                RANDOM.nextBytes(pubkey);

                onlineAccounts.add(new OnlineAccountData(t << 32, sig, pubkey));
            }
        }

        return onlineAccounts;
    }

    @Test
    public void testOnlineAccountsModulusV1() throws IllegalAccessException, DataException {
        try (final Repository repository = RepositoryManager.getRepository()) {

            // Set feature trigger timestamp to MAX long so that it is inactive
            FieldUtils.writeField(BlockChain.getInstance(), "onlineAccountsModulusV2Timestamp", Long.MAX_VALUE, true);

            List<String> onlineAccountSignatures = new ArrayList<>();
            long fakeNTPOffset = 0L;

            // Mint a block and store its timestamp
            Block block = BlockMinter.mintTestingBlock(repository, Common.getTestAccount(repository, "alice-reward-share"));
            long lastBlockTimestamp = block.getBlockData().getTimestamp();

            // Mint some blocks and keep track of the different online account signatures
            for (int i = 0; i < 30; i++) {
                block = BlockMinter.mintTestingBlock(repository, Common.getTestAccount(repository, "alice-reward-share"));

                // Increase NTP fixed offset by the block time, to simulate time passing
                long blockTimeDelta = block.getBlockData().getTimestamp() - lastBlockTimestamp;
                lastBlockTimestamp = block.getBlockData().getTimestamp();
                fakeNTPOffset += blockTimeDelta;
                NTP.setFixedOffset(fakeNTPOffset);

                String lastOnlineAccountSignatures58 = Base58.encode(block.getBlockData().getOnlineAccountsSignatures());
                if (!onlineAccountSignatures.contains(lastOnlineAccountSignatures58)) {
                    onlineAccountSignatures.add(lastOnlineAccountSignatures58);
                }
            }

            // We expect at least 6 unique signatures over 30 blocks (generally 6-8, but could be higher due to block time differences)
            System.out.println(String.format("onlineAccountSignatures count: %d", onlineAccountSignatures.size()));
            assertTrue(onlineAccountSignatures.size() >= 6);
        }
    }

    @Test
    public void testOnlineAccountsModulusV2() throws IllegalAccessException, DataException {
        try (final Repository repository = RepositoryManager.getRepository()) {

            // Set feature trigger timestamp to 0 so that it is active
            FieldUtils.writeField(BlockChain.getInstance(), "onlineAccountsModulusV2Timestamp", 0L, true);

            List<String> onlineAccountSignatures = new ArrayList<>();
            long fakeNTPOffset = 0L;

            // Mint a block and store its timestamp
            Block block = BlockMinter.mintTestingBlock(repository, Common.getTestAccount(repository, "alice-reward-share"));
            long lastBlockTimestamp = block.getBlockData().getTimestamp();

            // Mint some blocks and keep track of the different online account signatures
            for (int i = 0; i < 30; i++) {
                block = BlockMinter.mintTestingBlock(repository, Common.getTestAccount(repository, "alice-reward-share"));

                // Increase NTP fixed offset by the block time, to simulate time passing
                long blockTimeDelta = block.getBlockData().getTimestamp() - lastBlockTimestamp;
                lastBlockTimestamp = block.getBlockData().getTimestamp();
                fakeNTPOffset += blockTimeDelta;
                NTP.setFixedOffset(fakeNTPOffset);

                String lastOnlineAccountSignatures58 = Base58.encode(block.getBlockData().getOnlineAccountsSignatures());
                if (!onlineAccountSignatures.contains(lastOnlineAccountSignatures58)) {
                    onlineAccountSignatures.add(lastOnlineAccountSignatures58);
                }
            }

            // We expect 1-3 unique signatures over 30 blocks
            System.out.println(String.format("onlineAccountSignatures count: %d", onlineAccountSignatures.size()));
            assertTrue(onlineAccountSignatures.size() >= 1 && onlineAccountSignatures.size() <= 3);
        }
    }
 @Test
    @Ignore(value = "For informational use")
    public void testOnlineAccountNonceCompression() throws IOException {
        List<OnlineAccountData> onlineAccounts = AccountUtils.generateOnlineAccounts(5000);

        // Build array of nonce values
        List<Integer> accountNonces = new ArrayList<>();
        for (OnlineAccountData onlineAccountData : onlineAccounts) {
            accountNonces.add(onlineAccountData.getNonce());
        }

        // Write nonces into ConciseSet
        ConciseSet nonceSet = new ConciseSet();
        nonceSet = nonceSet.convert(accountNonces);
        byte[] conciseEncodedNonces = nonceSet.toByteBuffer().array();

        // Also write to regular byte array of ints, for comparison
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (Integer nonce : accountNonces) {
            bytes.write(Ints.toByteArray(nonce));
        }
        byte[] standardEncodedNonces = bytes.toByteArray();

        System.out.println(String.format("Standard: %d", standardEncodedNonces.length));
        System.out.println(String.format("Concise: %d", conciseEncodedNonces.length));
    }   
}
