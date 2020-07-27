/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry;

public class RedirectionPortRegistryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RedirectionPortRegistryException(String message) {
        super(message);
    }

    public RedirectionPortRegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RedirectionPortRegistryException(Throwable cause) {
        super(cause);
    }

}
