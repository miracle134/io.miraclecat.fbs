package io.miraclecat.fbs.core.controller;

import io.miraclecat.fbs.core.config.ManifestProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * packageName    : io.miraclecat.fbs.core.controller
 * fileName       : AssetModelAdvice
 * author         : MiracleCat 
 * date           : 2025-08-12
 * description    :
 */
@Slf4j
@ControllerAdvice
public class AssetModelAdvice {
    private final ManifestProvider manifestProvider;
    private final String assetHost;

    private static final AtomicReference<String> buildStamp = new AtomicReference<>(String.valueOf(System.currentTimeMillis()));

    public AssetModelAdvice(ManifestProvider manifestProvider,
                            @Value("${frontend.asset-host:}") String assetHost) {
        this.manifestProvider = manifestProvider;
        this.assetHost = assetHost; // dev= http://localhost:8081 , prod=""
    }

    @ModelAttribute("assetHost")
    public String assetHost() {
        return assetHost;
    }

    @ModelAttribute("manifest")
    public Map<String, Object> manifest() {
        return manifestProvider.loadManifest();
    }

    @ModelAttribute("buildStamp")
    public String buildStamp() {
        return buildStamp.get();
    }
}
