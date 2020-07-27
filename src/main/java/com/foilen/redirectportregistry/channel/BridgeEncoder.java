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

import com.foilen.smalltools.tools.CharsetTools;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Encode the {@link BridgeMessage}.
 */
public class BridgeEncoder extends MessageToByteEncoder<BridgeMessage> {

    private static final Logger logger = LoggerFactory.getLogger(BridgeEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, BridgeMessage msg, ByteBuf out) throws Exception {

        try {

            // Action Enum
            writeString(out, msg.getAction().name());

            // Connection ID
            writeString(out, msg.getConnectionId());

            // Payload
            writeBytes(out, msg.getPayload());
        } catch (Exception e) {
            logger.warn("Problem encoding the message", e);
            throw e;
        }

    }

    private void writeBytes(ByteBuf out, byte[] message) {
        if (message == null) {
            out.writeInt(0);
        } else {
            out.writeInt(message.length);
            out.writeBytes(message);
        }
    }

    private void writeString(ByteBuf out, String text) {
        byte[] message = text.getBytes(CharsetTools.UTF_8);
        out.writeInt(message.length);
        out.writeBytes(message);
    }

}
