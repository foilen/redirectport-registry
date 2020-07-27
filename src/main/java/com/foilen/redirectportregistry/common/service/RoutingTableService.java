/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.common.service;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.foilen.redirectportregistry.RedirectionPortRegistryException;
import com.foilen.redirectportregistry.common.connection.TransportConnection;
import com.foilen.smalltools.net.netty.NettyClientMessagingQueue;
import com.foilen.smalltools.tools.CloseableTools;

@Service
public class RoutingTableService {

    private static class RoutingTableEntry implements Closeable {
        // This must be static
        private TransportConnection transportConnection;
        // The messaging queue could be changed, but must be certain that the queue is empty
        private NettyClientMessagingQueue nettyClientMessagingQueue;

        public RoutingTableEntry(TransportConnection transportConnection, NettyClientMessagingQueue nettyClientMessagingQueue) {
            this.transportConnection = transportConnection;
            this.nettyClientMessagingQueue = nettyClientMessagingQueue;
        }

        @Override
        public void close() {
            CloseableTools.close(transportConnection);
        }

    }

    private static final Logger logger = LoggerFactory.getLogger(RoutingTableService.class);

    private ConcurrentMap<String, RoutingTableEntry> routingTableEntryByConnectionId = new ConcurrentHashMap<>();

    /**
     * Add a new routing entry.
     *
     * @param connectionId
     *            the connection id
     * @param routingTableEntry
     *            the entry
     * @throws RedirectionPortRegistryException
     *             if the connection id is already used
     */
    protected void addEntry(String connectionId, RoutingTableEntry routingTableEntry) {
        RoutingTableEntry previous = routingTableEntryByConnectionId.putIfAbsent(connectionId, routingTableEntry);
        if (previous != null) {
            throw new RedirectionPortRegistryException("Connection id " + connectionId + " is already registered");
        }
    }

    /**
     * Add a new routing entry.
     *
     * @param connectionId
     *            the connection id
     * @param transportConnection
     *            the raw socket endpoint
     * @param nettyClientMessagingQueue
     *            the bridge endpoint
     * @throws RedirectionPortRegistryException
     *             if the connection id is already used
     */
    public void addEntry(String connectionId, TransportConnection transportConnection, NettyClientMessagingQueue nettyClientMessagingQueue) {
        logger.info("[{}] Adding an entry", connectionId);
        addEntry(connectionId, new RoutingTableEntry(transportConnection, nettyClientMessagingQueue));
    }

    public void closeAndRemove(TransportConnection transportConnection) {
        Iterator<RoutingTableEntry> it = routingTableEntryByConnectionId.values().iterator();
        while (it.hasNext()) {
            RoutingTableEntry next = it.next();
            if (next.transportConnection == transportConnection) {
                next.close();
                it.remove();
                break;
            }
        }
    }

    public void closeAndRemoveUnused(Set<String> usedConnectionIds) {
        Iterator<Entry<String, RoutingTableEntry>> it = routingTableEntryByConnectionId.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, RoutingTableEntry> next = it.next();
            if (!usedConnectionIds.contains(next.getKey())) {
                next.getValue().close();
                it.remove();
            }
        }

    }

    public Set<String> getAllConnectionIds() {
        Set<String> connectionIds = new HashSet<>();
        for (String connectionId : routingTableEntryByConnectionId.keySet()) {
            connectionIds.add(connectionId);
        }
        return connectionIds;
    }

    /**
     * Get the bridge endpoint.
     *
     * @param connectionId
     *            the connection id
     * @return the bridge endpoint
     * @throws RedirectionPortRegistryException
     *             if the connection id does not exists
     */
    public NettyClientMessagingQueue getNettyClientMessagingQueueOrFail(String connectionId) {
        return getOrFail(connectionId).nettyClientMessagingQueue;
    }

    private RoutingTableEntry getOrFail(String connectionId) {
        RoutingTableEntry entry = routingTableEntryByConnectionId.get(connectionId);
        if (entry == null) {
            throw new RedirectionPortRegistryException("Connection id " + connectionId + " does not exists");
        }
        return entry;
    }

    /**
     * Get the raw socket endpoint.
     *
     * @param connectionId
     *            the connection id
     * @return the raw socket endpoint
     * @throws RedirectionPortRegistryException
     *             if the connection id does not exists
     */
    public TransportConnection getTransportConnectionOrFail(String connectionId) {
        return getOrFail(connectionId).transportConnection;
    }

    /**
     * Rename a connection.
     *
     * @param previousConnectionId
     *            the connection id already registered
     * @param newConnectionId
     *            the new connection id for that entry
     * @throws RedirectionPortRegistryException
     *             if the previous connection id does not exists
     * @throws RedirectionPortRegistryException
     *             if the new connection id already exists
     */
    public void renameConnection(String previousConnectionId, String newConnectionId) {
        logger.info("[{}] Changing id to {}", previousConnectionId, newConnectionId);
        RoutingTableEntry previous = routingTableEntryByConnectionId.remove(previousConnectionId);
        if (previous == null) {
            throw new RedirectionPortRegistryException("Connection id " + previousConnectionId + " does not exists");
        }

        addEntry(newConnectionId, previous);

    }

}
