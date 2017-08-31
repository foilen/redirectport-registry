/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.foilen.smalltools.tools.AbstractBasics;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RedirectPortRegistryEntries extends AbstractBasics {

    private List<RedirectPortRegistryEntry> entries = new ArrayList<>();

    public List<RedirectPortRegistryEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<RedirectPortRegistryEntry> entries) {
        this.entries = entries;
    }

}
