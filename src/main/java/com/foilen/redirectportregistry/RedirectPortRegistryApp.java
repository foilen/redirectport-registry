/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.foilen.redirectportregistry.config.RedirectPortRegistryConfig;
import com.foilen.redirectportregistry.config.RedirectPortRegistryEntryConfig;
import com.foilen.redirectportregistry.config.RedirectPortRegistryExitConfig;
import com.foilen.redirectportregistry.model.RedirectPortRegistryEntries;
import com.foilen.redirectportregistry.model.RedirectPortRegistryEntry;
import com.foilen.redirectportregistry.model.RedirectPortRegistryExit;
import com.foilen.redirectportregistry.model.RedirectPortRegistryExits;
import com.foilen.smalltools.crypt.asymmetric.AsymmetricKeys;
import com.foilen.smalltools.crypt.asymmetric.RSACrypt;
import com.foilen.smalltools.crypt.cert.CertificateDetails;
import com.foilen.smalltools.crypt.cert.RSACertificate;
import com.foilen.smalltools.tools.CollectionsTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.LogbackTools;
import com.google.common.base.Strings;

public class RedirectPortRegistryApp {

    private final static Logger logger = LoggerFactory.getLogger(RedirectPortRegistryApp.class);

    public static void main(String[] args) {

        // Get the parameters
        RedirectPortRegistryOptions redirectPortRegistryOptions = new RedirectPortRegistryOptions();
        CmdLineParser cmdLineParser = new CmdLineParser(redirectPortRegistryOptions);
        try {
            cmdLineParser.parseArgument(args);
        } catch (CmdLineException e) {
            e.printStackTrace();
            showUsage();
            return;
        }

        // Launch the app
        new RedirectPortRegistryApp(redirectPortRegistryOptions).start();

    }

    private static void showUsage() {
        System.out.println("Usage:");
        CmdLineParser cmdLineParser = new CmdLineParser(new RedirectPortRegistryOptions());
        cmdLineParser.printUsage(System.out);
    }

    private RedirectPortRegistryOptions redirectPortRegistryOptions;
    private AnnotationConfigApplicationContext context;

    public RedirectPortRegistryApp(RedirectPortRegistryOptions redirectPortRegistryOptions) {
        this.redirectPortRegistryOptions = redirectPortRegistryOptions;
    }

    public void start() {
        // Check if debug
        if (redirectPortRegistryOptions.debug) {
            LogbackTools.changeConfig("/logback-debug.xml");
        } else {
            LogbackTools.changeConfig("/logback-normal.xml");
        }

        // Check if want sample
        if (redirectPortRegistryOptions.createSample) {
            logger.info("Creating sample files");
            createSample();
            return;
        }

        // Check if using cert
        List<String> certOptions = new ArrayList<>();
        certOptions.add(redirectPortRegistryOptions.caCertsFile);
        certOptions.add(redirectPortRegistryOptions.bridgeCertFile);
        certOptions.add(redirectPortRegistryOptions.bridgePrivateKeyFile);
        if (CollectionsTools.isAnyItemNotNullOrEmpty(certOptions) && !CollectionsTools.isAllItemNotNullOrEmpty(certOptions)) {
            System.out.println("ERROR: If you want to use the encrypted connection, you need to set the 3 properties: caCertFile, bridgeCertFile and bridgePrivateKeyFile");
            showUsage();
            return;
        }

        // Prepare Spring
        context = new AnnotationConfigApplicationContext();
        context.getBeanFactory().registerSingleton("redirectPortRegistryOptions", redirectPortRegistryOptions);
        context.register(RedirectPortRegistryConfig.class);

        // Check if is entry or exit bridge
        if (redirectPortRegistryOptions.entryBridgeRegistryFile != null) {

            logger.info("ENTRY - Will use the file {}", redirectPortRegistryOptions.entryBridgeRegistryFile);
            if (!redirectPortRegistryOptions.entryBridgeRegistryFile.exists()) {
                logger.error("The file {} does not exists", redirectPortRegistryOptions.entryBridgeRegistryFile);
                return;
            }

            context.register(RedirectPortRegistryEntryConfig.class);
        } else if (redirectPortRegistryOptions.exitBridgeRegistryFile != null) {

            logger.info("EXIT - Will use the file {} and listen on port {}", redirectPortRegistryOptions.exitBridgeRegistryFile, redirectPortRegistryOptions.bridgePort);

            context.register(RedirectPortRegistryExitConfig.class);
        } else {
            System.out.println("You need to set all the arguments for the entry or exit bridge");
            showUsage();
            return;
        }

        context.refresh();

    }

    private void createSample() {

        logger.info("Preparing certificates");

        // Encryption

        // Root
        AsymmetricKeys rootKeys = RSACrypt.RSA_CRYPT.generateKeyPair(4096);
        RSACertificate rootCertificate = new RSACertificate(rootKeys);
        rootCertificate.selfSign(new CertificateDetails().setCommonName("CA root"));
        List<String> rootCertificatePems = new ArrayList<>();
        rootCertificatePems.add(rootCertificate.saveCertificatePemAsString());
        if (!Strings.isNullOrEmpty(redirectPortRegistryOptions.caCertsFile)) {
            logger.info("Saving CA Certificates in file: {}", redirectPortRegistryOptions.caCertsFile);
            JsonTools.writeToFile(redirectPortRegistryOptions.caCertsFile, rootCertificatePems);
        }

        // Entry
        AsymmetricKeys entryKeys = RSACrypt.RSA_CRYPT.generateKeyPair(4096);
        RSACertificate entryCertificate = rootCertificate.signPublicKey(entryKeys, new CertificateDetails().setCommonName("entry 1"));
        if (!Strings.isNullOrEmpty(redirectPortRegistryOptions.bridgeCertFile)) {
            String fileName = redirectPortRegistryOptions.bridgeCertFile + "-entry";
            logger.info("Saving Entry Certificate in file: {}", fileName);
            entryCertificate.saveCertificatePem(fileName);
        }
        if (!Strings.isNullOrEmpty(redirectPortRegistryOptions.bridgePrivateKeyFile)) {
            String fileName = redirectPortRegistryOptions.bridgePrivateKeyFile + "-entry";
            logger.info("Saving Entry Private Keys in file: {}", fileName);
            RSACrypt.RSA_CRYPT.savePrivateKeyPem(entryKeys, fileName);
        }
        // Exit
        AsymmetricKeys exitKeys = RSACrypt.RSA_CRYPT.generateKeyPair(4096);
        RSACertificate exitCertificate = rootCertificate.signPublicKey(exitKeys, new CertificateDetails().setCommonName("exit 1"));
        if (!Strings.isNullOrEmpty(redirectPortRegistryOptions.bridgeCertFile)) {
            String fileName = redirectPortRegistryOptions.bridgeCertFile + "-exit";
            logger.info("Saving Exit Certificate in file: {}", fileName);
            exitCertificate.saveCertificatePem(fileName);
        }
        if (!Strings.isNullOrEmpty(redirectPortRegistryOptions.bridgePrivateKeyFile)) {
            String fileName = redirectPortRegistryOptions.bridgePrivateKeyFile + "-exit";
            logger.info("Saving Exit Private Keys in file: {}", fileName);
            RSACrypt.RSA_CRYPT.savePrivateKeyPem(exitKeys, fileName);
        }

        // Entry Registry
        if (redirectPortRegistryOptions.entryBridgeRegistryFile != null) {
            String fileName = redirectPortRegistryOptions.entryBridgeRegistryFile.getPath();
            logger.info("Saving Entry Registry in file: {}", fileName);
            RedirectPortRegistryEntries entries = new RedirectPortRegistryEntries();
            entries.getEntries().add(new RedirectPortRegistryEntry(10800, "127.0.0.1", redirectPortRegistryOptions.bridgePort, "GOOGLE", "HTTP"));
            JsonTools.writeToFile(fileName, entries);
        }

        // Exit Registry
        if (redirectPortRegistryOptions.exitBridgeRegistryFile != null) {
            String fileName = redirectPortRegistryOptions.exitBridgeRegistryFile.getPath();
            logger.info("Saving Exit Registry in file: {}", fileName);
            RedirectPortRegistryExits exits = new RedirectPortRegistryExits();
            exits.getExits().add(new RedirectPortRegistryExit("GOOGLE", "HTTP", "www.google.com", 80));
            JsonTools.writeToFile(fileName, exits);
        }
    }

    public void stop() {
        context.close();
    }

}
