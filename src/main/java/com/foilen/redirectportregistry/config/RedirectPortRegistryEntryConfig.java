/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.config;

import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.foilen.redirectportregistry.RedirectPortRegistryOptions;
import com.foilen.redirectportregistry.channel.BridgeMessageEntryChannel;
import com.foilen.redirectportregistry.common.service.MessageSenderService;
import com.foilen.redirectportregistry.entry.service.EntryBridgeService;
import com.foilen.smalltools.net.netty.NettyBuilder;

/**
 * Configure the entry service: entryBridgeRawPort -> entryBridgeRemoteHost:bridgePort .
 */
@Configuration
@ComponentScan("com.foilen.redirectportregistry.entry")
public class RedirectPortRegistryEntryConfig extends AbstractRedirectPortRegistryCommonConfig {

    @Autowired
    private MessageSenderService messageSenderService;
    @Autowired
    private RedirectPortRegistryOptions redirectPortRegistryOptions;

    @Override
    protected void configureNettyBuilderMessageChannel(NettyBuilder nettyBuilder, ExecutorService executorService) {
        nettyBuilder.addChannelHandler(BridgeMessageEntryChannel.class, messageSenderService);
    }

    @Bean
    public EntryBridgeService entryBridge() {
        return new EntryBridgeService(redirectPortRegistryOptions.entryBridgeRegistryFile);
    }

}
