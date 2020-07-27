/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.channel;

import com.foilen.smalltools.tools.CharsetTools;

import io.netty.util.CharsetUtil;

public class BridgeMessage {

    private BridgeAction action;
    private String connectionId;
    private byte[] payload;

    public BridgeMessage(BridgeAction action, String connectionId) {
        this.action = action;
        this.connectionId = connectionId;
    }

    public BridgeMessage(BridgeAction action, String connectionId, byte[] payload) {
        this.action = action;
        this.connectionId = connectionId;
        this.payload = payload;
    }

    public BridgeMessage(BridgeAction action, String connectionId, String payload) {
        this.action = action;
        this.connectionId = connectionId;
        this.payload = payload.getBytes(CharsetTools.UTF_8);
    }

    public BridgeAction getAction() {
        return action;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getPayloadAsString() {
        return new String(payload, CharsetUtil.UTF_8);
    }

    public void setAction(BridgeAction action) {
        this.action = action;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

}
