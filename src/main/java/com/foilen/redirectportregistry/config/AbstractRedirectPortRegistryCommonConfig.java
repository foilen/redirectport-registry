/*
    Redirectport-Registry
    https://github.com/foilen/redirectport-registry
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.config;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;

import com.foilen.redirectportregistry.RedirectPortRegistryOptions;
import com.foilen.redirectportregistry.channel.BridgeDecoder;
import com.foilen.redirectportregistry.channel.BridgeEncoder;
import com.foilen.smalltools.crypt.asymmetric.AsymmetricKeys;
import com.foilen.smalltools.crypt.asymmetric.RSACrypt;
import com.foilen.smalltools.crypt.cert.RSACertificate;
import com.foilen.smalltools.crypt.cert.RSATrustedCertificates;
import com.foilen.smalltools.net.netty.NettyBuilder;
import com.foilen.smalltools.tools.AssertTools;
import com.foilen.smalltools.tools.JsonTools;
import com.google.common.base.Strings;

public abstract class AbstractRedirectPortRegistryCommonConfig {

    protected abstract void configureNettyBuilderMessageChannel(NettyBuilder nettyBuilder, ExecutorService executorService);

    @Bean
    public NettyBuilder nettyBuilder(RedirectPortRegistryOptions redirectPortRegistryOptions) {

        ExecutorService executorService = Executors.newCachedThreadPool();

        // SSL
        RSATrustedCertificates trustedCertificates = null;
        RSACertificate certificate = null;

        if (!Strings.isNullOrEmpty(redirectPortRegistryOptions.caCertsFile)) {
            trustedCertificates = new RSATrustedCertificates();
            List<RSACertificate> caCerts = JsonTools.readFromFileAsList(redirectPortRegistryOptions.caCertsFile, String.class).stream() //
                    .map(it -> {
                        return RSACertificate.loadPemFromString(it);
                    }) //
                    .collect(Collectors.toList());
            for (RSACertificate caCert : caCerts) {
                trustedCertificates.addTrustedRsaCertificate(caCert);
            }

        }

        if (!Strings.isNullOrEmpty(redirectPortRegistryOptions.bridgeCertFile)) {
            certificate = RSACertificate.loadPemFromFile(redirectPortRegistryOptions.bridgeCertFile);
            if (!Strings.isNullOrEmpty(redirectPortRegistryOptions.bridgePrivateKeyFile)) {
                RSACrypt rsaCrypt = new RSACrypt();
                AsymmetricKeys keysForSigning = rsaCrypt.loadKeysPemFromFile(redirectPortRegistryOptions.bridgePrivateKeyFile);
                certificate.setKeysForSigning(keysForSigning);
            }

            AssertTools.assertNotNull(certificate.getCertificate(), "No certificate found");
            AssertTools.assertNotNull(certificate.getKeysForSigning(), "No keys for signing");
        }

        // Builder
        NettyBuilder nettyBuilder = new NettyBuilder();
        nettyBuilder.setCertificate(certificate);
        nettyBuilder.setTrustedCertificates(trustedCertificates);
        nettyBuilder.addChannelHandler(BridgeDecoder.class);
        configureNettyBuilderMessageChannel(nettyBuilder, executorService);
        nettyBuilder.addChannelHandler(BridgeEncoder.class);
        return nettyBuilder;
    }

}
