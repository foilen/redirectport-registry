/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.config;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.foilen.smalltools.net.commander.connectionpool.SimpleConnectionPool;
import com.google.common.eventbus.AsyncEventBus;

/**
 * To pass all the beans in asyncEventBus.register.
 */
public class RegisterAllEventsubscriberApplicationListener {

    private static final Logger logger = LoggerFactory.getLogger(SimpleConnectionPool.class);

    @EventListener
    public void registerEventSubscribers(ContextRefreshedEvent event) {
        AsyncEventBus asyncEventBus = event.getApplicationContext().getBean(AsyncEventBus.class);
        Map<String, Object> beansByName = event.getApplicationContext().getBeansOfType(Object.class, false, false);

        for (Entry<String, Object> entry : beansByName.entrySet()) {
            String name = entry.getKey();
            Object bean = entry.getValue();

            if (bean instanceof AsyncEventBus) {
                continue;
            }

            logger.info("Registering bean with name [{}] and type [{}]", name, bean.getClass().getName());

            asyncEventBus.register(bean);
        }
    }

}
