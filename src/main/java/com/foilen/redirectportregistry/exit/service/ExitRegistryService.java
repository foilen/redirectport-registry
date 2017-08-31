/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.exit.service;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.foilen.redirectportregistry.RedirectionPortRegistryException;
import com.foilen.redirectportregistry.model.RedirectPortRegistryExit;
import com.foilen.redirectportregistry.model.RedirectPortRegistryExits;
import com.foilen.smalltools.filesystemupdatewatcher.handler.OneFileUpdateNotifyer;
import com.foilen.smalltools.filesystemupdatewatcher.handler.OneFileUpdateNotifyerHandler;
import com.foilen.smalltools.tools.JsonTools;

public class ExitRegistryService implements OneFileUpdateNotifyerHandler {

    private static final Logger logger = LoggerFactory.getLogger(ExitRegistryService.class);

    private Map<String, RedirectPortRegistryExit> exitByServiceKey = new HashMap<>();

    // Properties
    private OneFileUpdateNotifyer oneFileUpdateNotifyer;

    public ExitRegistryService(String filePath) {
        oneFileUpdateNotifyer = new OneFileUpdateNotifyer(filePath, this);
    }

    @Override
    public void fileUpdated(String fileName) {
        logger.info("Refreshing the registry {}", fileName);

        // Read the registry file
        RedirectPortRegistryExits exits = null;
        try {
            exits = JsonTools.readFromFile(fileName, RedirectPortRegistryExits.class);
        } catch (Exception e) {
            logger.warn("Could not read the registry {} . Using an empty registry", fileName);
            exits = new RedirectPortRegistryExits();
        }

        // Update the registry
        Map<String, RedirectPortRegistryExit> newExitByServiceKey = new HashMap<>();
        for (RedirectPortRegistryExit next : exits.getExits()) {
            newExitByServiceKey.put(next.getServiceName() + "/" + next.getServiceEndpoint(), next);
        }

        // Switch
        exitByServiceKey = newExitByServiceKey;
    }

    /**
     * Get the exit for the requested service.
     *
     * @param serviceKey
     *            the service key (ex: mysql/tcp)
     * @return the exit details
     * @throws RedirectionPortRegistryException
     *             if the service does not exists
     */
    public RedirectPortRegistryExit getRedirectPortRegistryExit(String serviceKey) {
        RedirectPortRegistryExit redirectPortRegistryExit = exitByServiceKey.get(serviceKey);
        if (redirectPortRegistryExit == null) {
            throw new RedirectionPortRegistryException("The service " + serviceKey + " does not exists");
        }
        return redirectPortRegistryExit;
    }

    @PostConstruct
    public void init() {
        oneFileUpdateNotifyer.initAutoUpdateSystem();
    }

}
