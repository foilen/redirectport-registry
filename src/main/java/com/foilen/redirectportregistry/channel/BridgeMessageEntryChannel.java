/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.foilen.redirectportregistry.RedirectionPortRegistryException;
import com.foilen.redirectportregistry.common.service.MessageSenderService;
import com.foilen.smalltools.tools.AssertTools;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

/**
 * A channel that takes a {@link BridgeMessage} from the exit side.
 */
public class BridgeMessageEntryChannel extends ChannelHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(BridgeMessageEntryChannel.class);

    private MessageSenderService messageSenderService;

    public BridgeMessageEntryChannel(MessageSenderService messageSenderService) {
        this.messageSenderService = messageSenderService;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        BridgeMessage bridgeMessage = (BridgeMessage) msg;
        String connectionId;
        String halfConnectionId;

        switch (bridgeMessage.getAction()) {
        case CONNECT_ACK:
            connectionId = bridgeMessage.getConnectionId();
            halfConnectionId = bridgeMessage.getPayloadAsString();
            logger.info("Got an ACK for half connection {}. New connection id is {}", halfConnectionId, connectionId);

            // Verify that the client_uniqueId is the right one in the new connection id
            AssertTools.assertTrue(connectionId.startsWith(halfConnectionId), "The new id does not contain our part. " + halfConnectionId + " -> " + connectionId);
            AssertTools.assertTrue(connectionId.length() == halfConnectionId.length() * 2, "The new id must be twice the initial one. " + halfConnectionId + " -> " + connectionId);
            messageSenderService.receivedConnectAck(halfConnectionId, connectionId);
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
