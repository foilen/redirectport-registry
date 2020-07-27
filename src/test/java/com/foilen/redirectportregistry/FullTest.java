/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.foilen.redirectportregistry.model.RedirectPortRegistryEntries;
import com.foilen.redirectportregistry.model.RedirectPortRegistryEntry;
import com.foilen.redirectportregistry.model.RedirectPortRegistryExit;
import com.foilen.redirectportregistry.model.RedirectPortRegistryExits;
import com.foilen.smalltools.crypt.spongycastle.asymmetric.AsymmetricKeys;
import com.foilen.smalltools.crypt.spongycastle.asymmetric.RSACrypt;
import com.foilen.smalltools.crypt.spongycastle.cert.CertificateDetails;
import com.foilen.smalltools.crypt.spongycastle.cert.RSACertificate;
import com.foilen.smalltools.net.services.TCPServerService;
import com.foilen.smalltools.tools.FileTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.ThreadTools;

public class FullTest {

    private static final String LOCALHOST = "localhost";
    static private final int CONCURRENT_CONNECTIONS = 50;

    private void checkPortOpen(int port, boolean open) {

        Assert.assertEquals(open, isPortOpen(port));
    }

    private boolean isPortOpen(int port) {
        boolean actual = false;

        try {
            Socket socket = new Socket(LOCALHOST, port);
            socket.close();
            actual = true;
        } catch (Exception e) {
        }

        return actual;
    }

    private void sendMessagesPorts(Deque<String> actualDeque, int... ports) throws Exception {

        actualDeque.clear();
        Deque<String> expectedDeque = new ConcurrentLinkedDeque<>();

        List<Thread> threads = new ArrayList<>(ports.length * CONCURRENT_CONNECTIONS);

        // Connect and send the messages
        for (int i = 0; i < CONCURRENT_CONNECTIONS; ++i) {
            int connectionIndex = i;
            for (int portIdx = 0; portIdx < ports.length; ++portIdx) {
                int portIndex = portIdx;
                int port = ports[portIndex];

                Thread thread = new Thread(() -> {

                    try {
                        Socket socket = new Socket(LOCALHOST, port);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        PrintWriter writer = new PrintWriter(socket.getOutputStream());

                        for (int msgId = 0; msgId < 10; ++msgId) {
                            // Send a message
                            String msg = "Message from port " + port + " connection " + connectionIndex;
                            expectedDeque.add(portIndex + " > " + msg);
                            writer.println(msg);
                            writer.flush();

                            // Wait for it to come back
                            Assert.assertEquals("Got > " + msg, reader.readLine());
                        }

                        socket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Assert.fail("Got an exception");
                    }

                }, "sendMessagesPorts-" + i);

                thread.start();
                threads.add(thread);
            }
        }

        // Wait for completion
        for (Thread thread : threads) {
            thread.join();
        }

        // Assert all messages in the deque
        List<String> expectedList = expectedDeque.stream().sorted().collect(Collectors.toList());
        List<String> actualList = actualDeque.stream().sorted().collect(Collectors.toList());

        Assert.assertEquals(expectedList, actualList);
    }

    @Test(timeout = 120000)
    public void test() throws Exception {

        Deque<String> commonDeque = new ConcurrentLinkedDeque<>();

        // Create 2 fake servers
        TCPServerService[] rawService = new TCPServerService[2];
        for (int i = 0; i < 2; ++i) {
            int index = i;
            rawService[i] = new TCPServerService(socket -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter writer = new PrintWriter(socket.getOutputStream());
                    String line;
                    while ((line = reader.readLine()) != null) {
                        commonDeque.add(index + " > " + line);
                        writer.println("Got > " + line);
                        writer.flush();
                    }
                    socket.close();
                } catch (Exception e) {
                }
            });
        }
        int rawServicePort1 = rawService[0].getPort();
        int rawServicePort2 = rawService[1].getPort();

        int rawEntryPort1 = (int) (Math.random() * 2000 + 3000);
        int rawEntryPort2 = (int) (Math.random() * 2000 + 3000);
        int differentRawEntryPort1 = (int) (Math.random() * 2000 + 3000);
        int differentRawEntryPort2 = (int) (Math.random() * 2000 + 3000);

        // Encryption
        RSACrypt rsaCrypt = new RSACrypt();
        String rootCertsFile = File.createTempFile("certs", null).getAbsolutePath();
        String entryCertFile = File.createTempFile("cert", null).getAbsolutePath();
        String exitCertFile = File.createTempFile("cert", null).getAbsolutePath();
        String entryKeysFile = File.createTempFile("keys", null).getAbsolutePath();
        String exitKeysFile = File.createTempFile("keys", null).getAbsolutePath();

        // Root
        AsymmetricKeys root1Keys = rsaCrypt.generateKeyPair(2048);
        RSACertificate root1Certificate = new RSACertificate(root1Keys);
        root1Certificate.selfSign(new CertificateDetails().setCommonName("CA root"));
        AsymmetricKeys root2Keys = rsaCrypt.generateKeyPair(2048);
        RSACertificate root2Certificate = new RSACertificate(root2Keys);
        root2Certificate.selfSign(new CertificateDetails().setCommonName("CA root"));
        List<String> rootCertificatePems = new ArrayList<>();
        rootCertificatePems.add(root1Certificate.saveCertificatePemAsString());
        rootCertificatePems.add(root2Certificate.saveCertificatePemAsString());
        String rootCertificatesPem = JsonTools.compactPrint(rootCertificatePems);
        FileTools.writeFile(rootCertificatesPem, rootCertsFile);

        // Entry
        AsymmetricKeys entry1Keys = rsaCrypt.generateKeyPair(2048);
        RSACertificate entry1Certificate = root1Certificate.signPublicKey(entry1Keys, new CertificateDetails().setCommonName("entry 1"));
        entry1Certificate.saveCertificatePem(entryCertFile);
        rsaCrypt.saveKeysPem(entry1Keys, entryKeysFile);
        // Exit
        AsymmetricKeys exit1Keys = rsaCrypt.generateKeyPair(2048);
        RSACertificate exit1Certificate = root1Certificate.signPublicKey(exit1Keys, new CertificateDetails().setCommonName("exit 1"));
        exit1Certificate.saveCertificatePem(exitCertFile);
        rsaCrypt.saveKeysPem(exit1Keys, exitKeysFile);

        // Create the entry part
        int bridgePort = (int) (Math.random() * 2000 + 3000);
        RedirectPortRegistryOptions entryOptions = new RedirectPortRegistryOptions();
        entryOptions.caCertsFile = rootCertsFile;
        entryOptions.bridgeCertFile = entryCertFile;
        entryOptions.bridgePrivateKeyFile = entryKeysFile;
        entryOptions.entryBridgeRegistryFile = File.createTempFile("entry", null);
        RedirectPortRegistryApp entryApp = new RedirectPortRegistryApp(entryOptions);

        // Create the exit part
        RedirectPortRegistryOptions exitOptions = new RedirectPortRegistryOptions();
        exitOptions.caCertsFile = rootCertsFile;
        exitOptions.bridgeCertFile = exitCertFile;
        exitOptions.bridgePrivateKeyFile = exitKeysFile;
        exitOptions.bridgePort = bridgePort;
        exitOptions.exitBridgeRegistryFile = File.createTempFile("exit", null);
        RedirectPortRegistryApp exitApp = new RedirectPortRegistryApp(exitOptions);

        // Start the app
        entryApp.start();
        exitApp.start();

        // Check entries closed
        checkPortOpen(rawEntryPort1, false);
        checkPortOpen(rawEntryPort2, false);
        checkPortOpen(differentRawEntryPort1, false);
        checkPortOpen(differentRawEntryPort2, false);

        // Create initial registries (entry and exit) with only 1 server
        RedirectPortRegistryExits redirectPortRegistryExits = new RedirectPortRegistryExits();
        redirectPortRegistryExits.getExits().add(new RedirectPortRegistryExit("service_1", "endpoint_1", LOCALHOST, rawServicePort1));
        JsonTools.writeToFile(exitOptions.exitBridgeRegistryFile, redirectPortRegistryExits);

        RedirectPortRegistryEntries redirectPortRegistryEntries = new RedirectPortRegistryEntries();
        redirectPortRegistryEntries.getEntries().add(new RedirectPortRegistryEntry(rawEntryPort1, LOCALHOST, bridgePort, "service_1", "endpoint_1"));
        JsonTools.writeToFile(entryOptions.entryBridgeRegistryFile, redirectPortRegistryEntries);

        // Check only one entry raw port is open
        while (!isPortOpen(rawEntryPort1)) {
            ThreadTools.sleep(500);
        }
        checkPortOpen(rawEntryPort1, true);
        checkPortOpen(rawEntryPort2, false);
        checkPortOpen(differentRawEntryPort1, false);
        checkPortOpen(differentRawEntryPort2, false);

        // Use the raw entry port
        sendMessagesPorts(commonDeque, rawEntryPort1);

        // Modify the registries (entry and exit) with both servers
        redirectPortRegistryExits = new RedirectPortRegistryExits();
        redirectPortRegistryExits.getExits().add(new RedirectPortRegistryExit("service_1", "endpoint_1", LOCALHOST, rawServicePort1));
        redirectPortRegistryExits.getExits().add(new RedirectPortRegistryExit("service_2", "endpoint_2", LOCALHOST, rawServicePort2));
        JsonTools.writeToFile(exitOptions.exitBridgeRegistryFile, redirectPortRegistryExits);

        redirectPortRegistryEntries = new RedirectPortRegistryEntries();
        redirectPortRegistryEntries.getEntries().add(new RedirectPortRegistryEntry(rawEntryPort1, LOCALHOST, bridgePort, "service_1", "endpoint_1"));
        redirectPortRegistryEntries.getEntries().add(new RedirectPortRegistryEntry(rawEntryPort2, LOCALHOST, bridgePort, "service_2", "endpoint_2"));
        JsonTools.writeToFile(entryOptions.entryBridgeRegistryFile, redirectPortRegistryEntries);

        // Check both entry raw port are open
        while (!isPortOpen(rawEntryPort2)) {
            ThreadTools.sleep(500);
        }
        checkPortOpen(rawEntryPort1, true);
        checkPortOpen(rawEntryPort2, true);
        checkPortOpen(differentRawEntryPort1, false);
        checkPortOpen(differentRawEntryPort2, false);

        // Use the raw entry ports
        sendMessagesPorts(commonDeque, rawEntryPort1, rawEntryPort2);

        // Modify the entry registry with both servers on different raw entry port
        redirectPortRegistryEntries = new RedirectPortRegistryEntries();
        redirectPortRegistryEntries.getEntries().add(new RedirectPortRegistryEntry(differentRawEntryPort1, LOCALHOST, bridgePort, "service_1", "endpoint_1"));
        redirectPortRegistryEntries.getEntries().add(new RedirectPortRegistryEntry(differentRawEntryPort2, LOCALHOST, bridgePort, "service_2", "endpoint_2"));
        JsonTools.writeToFile(entryOptions.entryBridgeRegistryFile, redirectPortRegistryEntries);

        // Check that the old raw entry ports are closed and the new ones are open
        while (isPortOpen(rawEntryPort1) || isPortOpen(rawEntryPort2) || !isPortOpen(differentRawEntryPort1) || !isPortOpen(differentRawEntryPort2)) {
            ThreadTools.sleep(500);
        }
        checkPortOpen(rawEntryPort1, false);
        checkPortOpen(rawEntryPort2, false);
        checkPortOpen(differentRawEntryPort1, true);
        checkPortOpen(differentRawEntryPort2, true);

        // Use the raw entry ports
        sendMessagesPorts(commonDeque, differentRawEntryPort1, differentRawEntryPort2);

        // Stop the app
        entryApp.stop();
        exitApp.stop();

    }

}
