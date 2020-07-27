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
import com.foilen.redirectportregistry.channel.BridgeMessageExitChannel;
import com.foilen.redirectportregistry.common.service.MessageSenderService;
import com.foilen.redirectportregistry.common.service.RoutingTableService;
import com.foilen.redirectportregistry.common.service.UniqueIdGeneratorService;
import com.foilen.redirectportregistry.exit.service.ExitRegistryService;
import com.foilen.smalltools.net.netty.NettyBuilder;
import com.foilen.smalltools.net.netty.NettyServer;

/**
 * Configure the exit service: localhost:bridgePort -> exitBridgeRawPort .
 */
@Configuration
@ComponentScan("com.foilen.redirectportregistry.exit")
public class RedirectPortRegistryExitConfig extends AbstractRedirectPortRegistryCommonConfig {

    @Autowired
    private ExitRegistryService exitRegistryService;
    @Autowired
    private MessageSenderService messageSenderService;
    @Autowired
    private RedirectPortRegistryOptions redirectPortRegistryOptions;
    @Autowired
    private RoutingTableService routingTableService;
    @Autowired
    private UniqueIdGeneratorService uniqueIdGeneratorService;

    @Override
    protected void configureNettyBuilderMessageChannel(NettyBuilder nettyBuilder, ExecutorService executorService) {
        nettyBuilder.addChannelHandler(BridgeMessageExitChannel.class, //
                redirectPortRegistryOptions.bridgePort, //
                executorService, //
                exitRegistryService, //
                messageSenderService, //
                routingTableService, //
                uniqueIdGeneratorService);
    }

    @Bean
    public ExitRegistryService exitRegistryService(RedirectPortRegistryOptions redirectPortRegistryOptions) {
        return new ExitRegistryService(redirectPortRegistryOptions.exitBridgeRegistryFile.getAbsolutePath());
    }

    @Bean
    public NettyServer nettyServer(NettyBuilder nettyBuilder) {
        return nettyBuilder.buildServer(redirectPortRegistryOptions.bridgePort);
    }

}
