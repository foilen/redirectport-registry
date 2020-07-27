/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.common.service;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.foilen.redirectportregistry.channel.BridgeAction;
import com.foilen.redirectportregistry.channel.BridgeMessage;
import com.foilen.redirectportregistry.events.ConnectionUsedEvent;
import com.foilen.redirectportregistry.events.DataSentEvent;
import com.foilen.smalltools.net.netty.NettyClientMessagingQueue;
import com.google.common.eventbus.AsyncEventBus;

@Service
public class MessageSenderService {

    private AsyncEventBus asyncEventBus;
    private RoutingTableService routingTableService;

    @Autowired
    public MessageSenderService(AsyncEventBus asyncEventBus, RoutingTableService routingTableService) {
        this.asyncEventBus = asyncEventBus;
        this.routingTableService = routingTableService;
    }

    /**
     * Received on the entry side.
     *
     * @param previousConnectionId
     *            the half connection id
     * @param newConnectionId
     *            the new connection id
     */
    public void receivedConnectAck(String previousConnectionId, String newConnectionId) {
        routingTableService.renameConnection(previousConnectionId, newConnectionId);
        routingTableService.getTransportConnectionOrFail(newConnectionId).init(newConnectionId);
    }

    /**
     * Data received from the bridge to send to the raw socket.
     *
     * @param connectionId
     *            the connection id
     * @param data
     *            the data
     */
    public void receivedData(String connectionId, byte[] data) {
        asyncEventBus.post(new ConnectionUsedEvent(connectionId));
        asyncEventBus.post(new DataSentEvent(connectionId, data.length));
        routingTableService.getTransportConnectionOrFail(connectionId).writeData(data);
    }

    public void receivedDisconnect(String connectionId) {
        routingTableService.getTransportConnectionOrFail(connectionId).gotRemoteDisconnection();
    }

    /**
     * When a local connection gets in, send it over.
     *
     * @param connectionId
     *            the connection id
     * @param serviceName
     *            the remote service name
     * @param serviceEndpoint
     *            the remote service endpoint
     */
    public void sendConnect(String connectionId, String serviceName, String serviceEndpoint) {
        String requestedService = serviceName + "/" + serviceEndpoint;
        routingTableService.getNettyClientMessagingQueueOrFail(connectionId).send(new BridgeMessage(BridgeAction.CONNECT, connectionId, requestedService));
    }

    /**
     * When a connection request comes from the bridge (on the exit side), send an ACK with the complete connection id.
     *
     * @param previousConnectionId
     *            the half connection id
     * @param newConnectionId
     *            the new connection id
     */
    public void sendConnectAck(String previousConnectionId, String newConnectionId) {
        routingTableService.getNettyClientMessagingQueueOrFail(newConnectionId).send(new BridgeMessage(BridgeAction.CONNECT_ACK, newConnectionId, previousConnectionId));
    }

    /**
     * Send data through the bridge.
     *
     * @param connectionId
     *            the connection id
     * @param buffer
     *            the data
     * @param length
     *            the length of the data to take
     */
    public void sendData(String connectionId, byte[] buffer, int length) {
        asyncEventBus.post(new ConnectionUsedEvent(connectionId));
        asyncEventBus.post(new DataSentEvent(connectionId, length));
        routingTableService.getNettyClientMessagingQueueOrFail(connectionId).send(new BridgeMessage(BridgeAction.SEND_DATA, connectionId, Arrays.copyOf(buffer, length)));
    }

    /**
     * Send a disconnect on the messaging queue. Used mostly when not yet in the routing table.
     *
     * @param nettyClientMessagingQueue
     *            the messaging queue
     * @param connectionId
     *            the connection id
     */
    public void sendDisconnect(NettyClientMessagingQueue nettyClientMessagingQueue, String connectionId) {
        nettyClientMessagingQueue.send(new BridgeMessage(BridgeAction.DISCONNECT, connectionId));
    }

    /**
     * Tells that the remote socket closed the connection.
     *
     * @param connectionId
     *            the connection id
     */
    public void sendDisconnect(String connectionId) {
        routingTableService.getNettyClientMessagingQueueOrFail(connectionId).send(new BridgeMessage(BridgeAction.DISCONNECT, connectionId));
    }

}
