/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.common.service;

import java.util.ArrayList;
import java.util.List;

import com.foilen.smalltools.net.netty.NettyBuilder;
import com.foilen.smalltools.net.netty.NettyClient;
import com.foilen.smalltools.net.netty.NettyClientMessagingQueue;

public class RemoteBridgeConnectionsEntry {

    private static final int MAX_CONNECTIONS = 5;

    private NettyBuilder nettyBuilder;
    private String remoteBridgeHost;
    private int bridgePort;
    private int next;
    private List<NettyClientMessagingQueue> nettyClientMessagingQueues = new ArrayList<>();

    public RemoteBridgeConnectionsEntry(NettyBuilder nettyBuilder, String remoteBridgeHost, int bridgePort) {
        this.nettyBuilder = nettyBuilder;
        this.remoteBridgeHost = remoteBridgeHost;
        this.bridgePort = bridgePort;
    }

    public void close() {
        nettyClientMessagingQueues.forEach(it -> {
            it.close();
        });
    }

    public void closeAndRemove(NettyClientMessagingQueue nettyClientMessagingQueue) {
        if (nettyClientMessagingQueues.remove(nettyClientMessagingQueue)) {
            nettyClientMessagingQueue.close();
        }
    }

    public synchronized List<NettyClientMessagingQueue> getAllNettyClientMessagingQueues() {
        return nettyClientMessagingQueues;
    }

    public synchronized NettyClientMessagingQueue getNext() {

        NettyClientMessagingQueue nettyClientMessagingQueue = null;

        while (nettyClientMessagingQueue == null) {
            if (next >= nettyClientMessagingQueues.size()) {
                if (next < MAX_CONNECTIONS) {
                    // Create new
                    NettyClient nettyClient = nettyBuilder.buildClient(remoteBridgeHost, bridgePort);
                    nettyClientMessagingQueue = NettyClientMessagingQueue.getInstance(nettyClient);
                    nettyClientMessagingQueues.add(nettyClientMessagingQueue);
                    ++next;
                    return nettyClientMessagingQueue;
                } else {
                    // Rewind
                    next = 1;

                    // Get or create first
                    if (nettyClientMessagingQueues.isEmpty()) {
                        // Create new
                        NettyClient nettyClient = nettyBuilder.buildClient(remoteBridgeHost, bridgePort);
                        nettyClientMessagingQueue = NettyClientMessagingQueue.getInstance(nettyClient);
                        nettyClientMessagingQueues.add(nettyClientMessagingQueue);
                        return nettyClientMessagingQueue;
                    } else {
                        nettyClientMessagingQueue = nettyClientMessagingQueues.get(0);
                    }
                }
            } else {
                nettyClientMessagingQueue = nettyClientMessagingQueues.get(next++);
            }

            // Check if connected
            if (!nettyClientMessagingQueue.isConnected()) {
                nettyClientMessagingQueues.remove(next - 1);
                nettyClientMessagingQueue.close();
                nettyClientMessagingQueue = null;
            }
        }
        return nettyClientMessagingQueue;
    }

}
