/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.entry.service;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.foilen.redirectportregistry.common.service.RemoteBridgeConnectionsEntry;
import com.foilen.smalltools.net.netty.NettyBuilder;
import com.foilen.smalltools.net.netty.NettyClientMessagingQueue;

@Service
public class RemoteBridgeConnectionsService {

    @Autowired
    private NettyBuilder nettyBuilder;

    private ConcurrentMap<String, RemoteBridgeConnectionsEntry> entryByRemoteHost = new ConcurrentHashMap<>();

    /**
     * Close all the hosts that are sure to be no more used (those not in the given list).
     *
     * @param usedRemoteHosts
     *            all the remote hosts that could be used
     */
    public synchronized void cleanupUnused(List<String> usedRemoteHosts) {
        Iterator<Entry<String, RemoteBridgeConnectionsEntry>> it = entryByRemoteHost.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, RemoteBridgeConnectionsEntry> next = it.next();
            String host = next.getKey();
            if (!usedRemoteHosts.contains(host)) {
                next.getValue().close();
                it.remove();
            }
        }
    }

    public synchronized NettyClientMessagingQueue getOneOrConnect(String remoteBridgeHost, int bridgePort) {
        RemoteBridgeConnectionsEntry entry = entryByRemoteHost.get(remoteBridgeHost);
        if (entry == null) {
            entry = new RemoteBridgeConnectionsEntry(nettyBuilder, remoteBridgeHost, bridgePort);
            entryByRemoteHost.put(remoteBridgeHost, entry);
        }
        return entry.getNext();
    }

}
