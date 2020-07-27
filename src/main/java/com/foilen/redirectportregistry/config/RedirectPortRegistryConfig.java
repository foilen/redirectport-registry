/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.foilen.smalltools.tools.SpringTools;
import com.google.common.eventbus.AsyncEventBus;

/**
 * Configure the common services.
 */
@Configuration
@ComponentScan("com.foilen.redirectportregistry.common")
@EnableScheduling
public class RedirectPortRegistryConfig {

    @Bean
    public AsyncEventBus asyncEventBus() {
        return new AsyncEventBus(executor());
    }

    @Bean
    public Executor executor() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public RegisterAllEventsubscriberApplicationListener registerAllEventsubscriberApplicationListener() {
        return new RegisterAllEventsubscriberApplicationListener();
    }

    @Bean
    public SpringTools springTools() {
        return new SpringTools();
    }

}
