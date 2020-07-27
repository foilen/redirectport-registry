/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RedirectPortRegistryExits {

    private List<RedirectPortRegistryExit> exits = new ArrayList<>();

    public List<RedirectPortRegistryExit> getExits() {
        return exits;
    }

    public void setExits(List<RedirectPortRegistryExit> exits) {
        this.exits = exits;
    }

}
