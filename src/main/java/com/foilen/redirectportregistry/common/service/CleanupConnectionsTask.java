/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.common.service;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.foilen.redirectportregistry.events.ConnectionUsedEvent;
import com.google.common.eventbus.Subscribe;

/**
 * Cleaning up connections idling between 5 and 10 minutes.
 */
@Service
public class CleanupConnectionsTask {

    private static final Logger logger = LoggerFactory.getLogger(CleanupConnectionsTask.class);

    @Autowired
    private RoutingTableService routingTableService;

    private Set<String> usedConnectionIds = new HashSet<>();

    @Scheduled(fixedRate = 5 * 60000)
    public synchronized void cleanupIdlingConnections() {
        logger.info("Cleaning up idling connections");

        routingTableService.closeAndRemoveUnused(usedConnectionIds);
        usedConnectionIds.clear();

        logger.info("Cleanup of idling connections completed");
    }

    @Subscribe
    public synchronized void recordUsage(ConnectionUsedEvent connectionUsedEvent) {
        usedConnectionIds.add(connectionUsedEvent.getConnectionId());
    }

}
