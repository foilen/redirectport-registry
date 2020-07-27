/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.channel;

import java.net.Socket;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.foilen.redirectportregistry.RedirectionPortRegistryException;
import com.foilen.redirectportregistry.common.connection.TransportConnection;
import com.foilen.redirectportregistry.common.service.MessageSenderService;
import com.foilen.redirectportregistry.common.service.RoutingTableService;
import com.foilen.redirectportregistry.common.service.UniqueIdGeneratorService;
import com.foilen.redirectportregistry.exit.service.ExitRegistryService;
import com.foilen.redirectportregistry.model.RedirectPortRegistryExit;
import com.foilen.smalltools.net.netty.NettyClientMessagingQueue;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

/**
 * A channel that takes a {@link BridgeMessage} and relays it.
 */
public class BridgeMessageExitChannel extends ChannelHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(BridgeMessageExitChannel.class);

    private ExecutorService executorService;
    private ExitRegistryService exitRegistryService;
    private MessageSenderService messageSenderService;
    private RoutingTableService routingTableService;
    private UniqueIdGeneratorService uniqueIdGeneratorService;
    private Integer port;

    public BridgeMessageExitChannel(Integer port, ExecutorService executorService, ExitRegistryService exitRegistryService, MessageSenderService messageSenderService,
            RoutingTableService routingTableService, UniqueIdGeneratorService uniqueIdGeneratorService) {
        this.port = port;
        this.executorService = executorService;
        this.exitRegistryService = exitRegistryService;
        this.messageSenderService = messageSenderService;
        this.routingTableService = routingTableService;
        this.uniqueIdGeneratorService = uniqueIdGeneratorService;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        BridgeMessage bridgeMessage = (BridgeMessage) msg;

        String connectionId;
        String halfConnectionId;

        switch (bridgeMessage.getAction()) {
        case CONNECT:
            halfConnectionId = bridgeMessage.getConnectionId();
            connectionId = halfConnectionId + uniqueIdGeneratorService.generate();
            logger.info("Got a new connection with id {} -> {}", halfConnectionId, connectionId);
            executorService.submit(() -> {

                NettyClientMessagingQueue nettyClientMessagingQueue = NettyClientMessagingQueue.getInstance(ctx.channel());
                TransportConnection transportConnection;
                try {
                    // Get the exit details
                    String serviceKey = bridgeMessage.getPayloadAsString();
                    RedirectPortRegistryExit redirectPortRegistryExit = exitRegistryService.getRedirectPortRegistryExit(serviceKey);

                    // Connect to the right machine and port
                    Socket socket = new Socket(redirectPortRegistryExit.getExitRawHost(), redirectPortRegistryExit.getExitRawPort());
                    transportConnection = new TransportConnection(connectionId, socket, messageSenderService);

                } catch (Exception e) {
                    logger.error("Could not connect to local service on port {} for connection {}", port, connectionId);
                    messageSenderService.sendDisconnect(nettyClientMessagingQueue, halfConnectionId);
                    return;
                }

                try {
                    routingTableService.addEntry(connectionId, transportConnection, nettyClientMessagingQueue);
                    messageSenderService.sendConnectAck(halfConnectionId, connectionId);
                    transportConnection.init(connectionId);
                } catch (Exception e) {
                    logger.error("[{}] Problem with new connection", connectionId, e);
                    messageSenderService.sendDisconnect(nettyClientMessagingQueue, halfConnectionId);
                    messageSenderService.sendDisconnect(nettyClientMessagingQueue, connectionId);
                }

            });
            break;

        case DISCONNECT:
            connectionId = bridgeMessage.getConnectionId();
            logger.info("Got a disconnect for connection with id {}", connectionId);
            messageSenderService.receivedDisconnect(connectionId);
            break;

        case SEND_DATA:
            connectionId = bridgeMessage.getConnectionId();
            logger.debug("Got data from connection with id {}", connectionId);
            messageSenderService.receivedData(connectionId, bridgeMessage.getPayload());
            break;

        default:
            throw new RedirectionPortRegistryException("Action not supported: " + bridgeMessage.getAction());
        }

    }

}
