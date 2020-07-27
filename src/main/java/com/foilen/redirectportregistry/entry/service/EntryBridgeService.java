/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.entry.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.foilen.redirectportregistry.common.connection.TransportConnection;
import com.foilen.redirectportregistry.common.service.MessageSenderService;
import com.foilen.redirectportregistry.common.service.RoutingTableService;
import com.foilen.redirectportregistry.common.service.UniqueIdGeneratorService;
import com.foilen.redirectportregistry.model.RedirectPortRegistryEntries;
import com.foilen.redirectportregistry.model.RedirectPortRegistryEntry;
import com.foilen.smalltools.filesystemupdatewatcher.handler.OneFileUpdateNotifyer;
import com.foilen.smalltools.filesystemupdatewatcher.handler.OneFileUpdateNotifyerHandler;
import com.foilen.smalltools.listscomparator.ListComparatorHandler;
import com.foilen.smalltools.listscomparator.ListsComparator;
import com.foilen.smalltools.net.netty.NettyClientMessagingQueue;
import com.foilen.smalltools.net.services.TCPServerService;
import com.foilen.smalltools.tools.CloseableTools;
import com.foilen.smalltools.tools.JsonTools;

/**
 * The listening server that initiates a remote connection.
 */
public class EntryBridgeService implements OneFileUpdateNotifyerHandler {

    private static final Logger logger = LoggerFactory.getLogger(EntryBridgeService.class);

    // Services
    @Autowired
    private MessageSenderService messageSenderService;
    @Autowired
    private RemoteBridgeConnectionsService remoteBridgeConnectionsService;
    @Autowired
    private RoutingTableService routingTableService;
    @Autowired
    private UniqueIdGeneratorService uniqueIdGeneratorService;

    // Properties
    private OneFileUpdateNotifyer oneFileUpdateNotifyer;

    // Execution state
    private ConcurrentMap<Integer, RedirectPortRegistryEntry> redirectPortRegistryEntryByPort = new ConcurrentHashMap<>();
    private ConcurrentMap<Integer, TCPServerService> tcpServerByPort = new ConcurrentHashMap<>();

    public EntryBridgeService(File entryRegistryFile) {
        oneFileUpdateNotifyer = new OneFileUpdateNotifyer(entryRegistryFile.getAbsolutePath(), this);
    }

    @Override
    public void fileUpdated(String fileName) {
        logger.info("Refreshing the registry {}", fileName);

        // Read the registry file
        RedirectPortRegistryEntries entries = null;
        try {
            entries = JsonTools.readFromFile(fileName, RedirectPortRegistryEntries.class);
        } catch (Exception e) {
            logger.warn("Could not read the registry {} . Using an empty registry", fileName);
            entries = new RedirectPortRegistryEntries();
        }

        // Update
        List<RedirectPortRegistryEntry> registryEntries = entries.getEntries();
        Collections.sort(registryEntries);
        List<RedirectPortRegistryEntry> currentEntries = redirectPortRegistryEntryByPort.values().stream().sorted().collect(Collectors.toList());
        List<RedirectPortRegistryEntry> toRemove = new ArrayList<>();
        List<RedirectPortRegistryEntry> toAdd = new ArrayList<>();

        ListsComparator.compareLists(currentEntries, registryEntries, new ListComparatorHandler<RedirectPortRegistryEntry, RedirectPortRegistryEntry>() {

            @Override
            public void both(RedirectPortRegistryEntry left, RedirectPortRegistryEntry right) {
                // Stay the same
            }

            @Override
            public void leftOnly(RedirectPortRegistryEntry left) {
                toRemove.add(left);
            }

            @Override
            public void rightOnly(RedirectPortRegistryEntry right) {
                toAdd.add(right);
            }
        });

        // Remove first
        toRemove.forEach(remove -> {
            // Disconnect and remove
            logger.info("Removing service {}/{} on port {}", remove.getRemoteServiceName(), remove.getRemoteServiceEndpoint(), remove.getEntryRawPort());
            int port = remove.getEntryRawPort();
            CloseableTools.close(tcpServerByPort.remove(port));
            redirectPortRegistryEntryByPort.remove(port);
        });

        // Then add
        toAdd.forEach(right -> { // Start a server
            logger.info("Adding service {}/{} on port {}", right.getRemoteServiceName(), right.getRemoteServiceEndpoint(), right.getEntryRawPort());
            int port = right.getEntryRawPort();
            redirectPortRegistryEntryByPort.put(port, right);

            TCPServerService serverService = new TCPServerService(port, socket -> {

                // Get or create a bridge connection to the remote host
                NettyClientMessagingQueue nettyClientMessagingQueue = remoteBridgeConnectionsService.getOneOrConnect(right.getRemoteBridgeHost(), right.getRemoteBridgePort());

                // Generate a unique id
                String connectionId = uniqueIdGeneratorService.generate();

                // Wrap the socket
                TransportConnection transportConnection = new TransportConnection(connectionId, socket, messageSenderService);

                // Register on the routing table
                routingTableService.addEntry(connectionId, transportConnection, nettyClientMessagingQueue);

                // Create a new connection
                messageSenderService.sendConnect(connectionId, right.getRemoteServiceName(), right.getRemoteServiceEndpoint());
            });

            tcpServerByPort.put(port, serverService);
        });

        // Cleanup connectionsPoolByRemotHost
        List<String> usedRemoteHosts = redirectPortRegistryEntryByPort.values().stream() //
                .map(RedirectPortRegistryEntry::getRemoteBridgeHost) //
                .sorted().distinct() //
                .collect(Collectors.toList());
        remoteBridgeConnectionsService.cleanupUnused(usedRemoteHosts);
    }

    @PostConstruct
    public void init() {
        oneFileUpdateNotifyer.initAutoUpdateSystem();
    }

}
