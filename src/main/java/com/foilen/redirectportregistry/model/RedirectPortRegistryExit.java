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
public class RedirectPortRegistryExit extends AbstractBasics implements Comparable<RedirectPortRegistryExit> {

    private String serviceName;
    private String serviceEndpoint;
    private String exitRawHost;
    private int exitRawPort;

    public RedirectPortRegistryExit() {
    }

    public RedirectPortRegistryExit(String serviceName, String serviceEndpoint, String exitRawHost, int exitRawPort) {
        this.serviceName = serviceName;
        this.serviceEndpoint = serviceEndpoint;
        this.exitRawHost = exitRawHost;
        this.exitRawPort = exitRawPort;
    }

    @Override
    public int compareTo(RedirectPortRegistryExit o) {
        ComparisonChain cc = ComparisonChain.start();
        cc = cc.compare(serviceName, o.serviceName);
        cc = cc.compare(serviceEndpoint, o.serviceEndpoint);
        cc = cc.compare(exitRawHost, o.exitRawHost);
        cc = cc.compare(exitRawPort, o.exitRawPort);
        return cc.result();
    }

    public String getExitRawHost() {
        return exitRawHost;
    }

    public int getExitRawPort() {
        return exitRawPort;
    }

    public String getServiceEndpoint() {
        return serviceEndpoint;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setExitRawHost(String exitRawHost) {
        this.exitRawHost = exitRawHost;
    }

    public void setExitRawPort(int exitRawPort) {
        this.exitRawPort = exitRawPort;
    }

    public void setServiceEndpoint(String serviceEndpoint) {
        this.serviceEndpoint = serviceEndpoint;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

}
