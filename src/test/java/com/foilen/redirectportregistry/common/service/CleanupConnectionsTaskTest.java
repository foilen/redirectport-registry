/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.common.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.foilen.redirectportregistry.events.ConnectionUsedEvent;

public class CleanupConnectionsTaskTest {

    private CleanupConnectionsTask task;
    private RoutingTableService routingTableService;

    private List<String> connectionIds;

    private void assertConnections() {
        List<String> actuals = routingTableService.getAllConnectionIds().stream().sorted().collect(Collectors.toList());
        Assert.assertEquals(connectionIds, actuals);
    }

    @Before
    public void before() {

        connectionIds = Arrays.asList("a", "b", "c", "d", "e");

        routingTableService = new RoutingTableService();

        for (String connectionId : connectionIds) {
            routingTableService.addEntry(connectionId, null, null);
        }

        // Prepare
        task = new CleanupConnectionsTask();
        ReflectionTestUtils.setField(task, "routingTableService", routingTableService);
    }

    @Test
    public void testCleanupIdlingConnections_All() {

        assertConnections();

        // No usage ; all gone
        task.cleanupIdlingConnections();

        connectionIds = Arrays.asList();
        assertConnections();
    }

    @Test
    public void testCleanupIdlingConnections_InStage() {

        // All
        assertConnections();

        // Remove d, e
        task.recordUsage(new ConnectionUsedEvent("a"));
        task.recordUsage(new ConnectionUsedEvent("b"));
        task.recordUsage(new ConnectionUsedEvent("c"));
        connectionIds = Arrays.asList("a", "b", "c");
        task.cleanupIdlingConnections();
        assertConnections();

        // Remove a, c
        task.recordUsage(new ConnectionUsedEvent("b"));
        connectionIds = Arrays.asList("b");
        task.cleanupIdlingConnections();
        assertConnections();

        // None
        connectionIds = Arrays.asList();
        task.cleanupIdlingConnections();
        assertConnections();
    }

    @Test
    public void testCleanupIdlingConnections_None() {

        assertConnections();

        for (String connectionId : connectionIds) {
            task.recordUsage(new ConnectionUsedEvent(connectionId));
        }

        // All used ; all present
        task.cleanupIdlingConnections();

        assertConnections();
    }

}
