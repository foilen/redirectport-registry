/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.common.service;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.foilen.smalltools.hash.HashSha256;
import com.foilen.smalltools.tools.SecureRandomTools;

@Service
public class UniqueIdGeneratorService {

    private AtomicLong counter = new AtomicLong();

    public String generate() {
        return HashSha256.hashString(counter.incrementAndGet() + SecureRandomTools.randomHexString(5)).substring(0, 5);
    }

}
