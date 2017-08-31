/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.foilen.smalltools.tools.AbstractBasics;
import com.google.common.collect.ComparisonChain;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RedirectPortRegistryEntry extends AbstractBasics implements Comparable<RedirectPortRegistryEntry> {

    private int entryRawPort;
    private String remoteBridgeHost;
    private int remoteBridgePort;
    private String remoteServiceName;
    private String remoteServiceEndpoint;

    public RedirectPortRegistryEntry() {
    }

    public RedirectPortRegistryEntry(int entryRawPort, String remoteBridgeHost, int remoteBridgePort, String remoteServiceName, String remoteServiceEndpoint) {
        this.entryRawPort = entryRawPort;
        this.remoteBridgeHost = remoteBridgeHost;
        this.remoteBridgePort = remoteBridgePort;
        this.remoteServiceName = remoteServiceName;
        this.remoteServiceEndpoint = remoteServiceEndpoint;
    }

    @Override
    public int compareTo(RedirectPortRegistryEntry o) {
        ComparisonChain cc = ComparisonChain.start();
        cc = cc.compare(entryRawPort, o.entryRawPort);
        cc = cc.compare(remoteBridgeHost, o.remoteBridgeHost);
        cc = cc.compare(remoteBridgePort, o.remoteBridgePort);
        cc = cc.compare(remoteServiceName, o.remoteServiceName);
        cc = cc.compare(remoteServiceEndpoint, o.remoteServiceEndpoint);
        return cc.result();
    }

    public int getEntryRawPort() {
        return entryRawPort;
    }

    public String getRemoteBridgeHost() {
        return remoteBridgeHost;
    }

    public int getRemoteBridgePort() {
        return remoteBridgePort;
    }

    public String getRemoteServiceEndpoint() {
        return remoteServiceEndpoint;
    }

    public String getRemoteServiceName() {
        return remoteServiceName;
    }

    public void setEntryRawPort(int entryRawPort) {
        this.entryRawPort = entryRawPort;
    }

    public void setRemoteBridgeHost(String remoteBridgeHost) {
        this.remoteBridgeHost = remoteBridgeHost;
    }

    public void setRemoteBridgePort(int remoteBridgePort) {
        this.remoteBridgePort = remoteBridgePort;
    }

    public void setRemoteServiceEndpoint(String remoteServiceEndpoint) {
        this.remoteServiceEndpoint = remoteServiceEndpoint;
    }

    public void setRemoteServiceName(String remoteServiceName) {
        this.remoteServiceName = remoteServiceName;
    }

}
