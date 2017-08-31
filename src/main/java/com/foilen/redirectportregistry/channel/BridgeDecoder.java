/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.channel;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.CharsetUtil;

/**
 * Decode the {@link BridgeMessage}.
 */
public class BridgeDecoder extends ReplayingDecoder<Void> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        // Action Enum
        BridgeAction bridgeAction = BridgeAction.valueOf(readString(in));

        // Connection ID
        String connectionId = readString(in);

        // Payload
        byte[] payload = readBytes(in);

        BridgeMessage bridgeMessage = new BridgeMessage(bridgeAction, connectionId, payload);
        out.add(bridgeMessage);
    }

    private byte[] readBytes(ByteBuf in) {
        int len = in.readInt();
        if (len == 0) {
            return null;
        } else {
            return in.readBytes(len).array();
        }
    }

    private String readString(ByteBuf in) {
        int len = in.readInt();
        String text = in.readBytes(len).toString(CharsetUtil.UTF_8);
        return text;
    }

}
