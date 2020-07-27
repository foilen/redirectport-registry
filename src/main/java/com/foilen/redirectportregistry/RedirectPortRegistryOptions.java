/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry;

import java.io.File;

import org.kohsuke.args4j.Option;

/**
 * The arguments to pass to a port redirect process.
 */
public class RedirectPortRegistryOptions {

    @Option(name = "--createSample", usage = "To create sample files")
    public boolean createSample;

    // For both
    @Option(name = "--debug", usage = "To log everything")
    public boolean debug;

    @Option(name = "--caCertsFile", metaVar = "file", usage = "The trusted certificates in JSON List<String> format")
    public String caCertsFile;
    @Option(name = "--bridgeCertFile", metaVar = "file", usage = "This side's certificate")
    public String bridgeCertFile;
    @Option(name = "--bridgePrivateKeyFile", metaVar = "file", usage = "This side's certificate's private key")
    public String bridgePrivateKeyFile;

    // For entry bridge
    @Option(name = "--entryBridgeRegistryFile", metaVar = "file", usage = "For entry bridge: tell the file that contains the list of ports to redirect")
    public File entryBridgeRegistryFile;

    // For exit bridge
    @Option(name = "--bridgePort", metaVar = "port", usage = "Choose the port for the bridge")
    public int bridgePort;
    @Option(name = "--exitBridgeRegistryFile", metaVar = "file", usage = "For exit bridge: tell the file that contains the list of endpoints to redirect to")
    public File exitBridgeRegistryFile;
}
