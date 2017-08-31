/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.events;

public class DataSentEvent {

    private String connectionId;
    private int dataLength;

    public DataSentEvent(String connectionId, int dataLength) {
        this.connectionId = connectionId;
        this.dataLength = dataLength;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public int getDataLength() {
        return dataLength;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

}
