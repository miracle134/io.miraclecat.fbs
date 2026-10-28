package io.miraclecat.fbs.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * packageName    : io.miraclecat.fbs.core.config
 * fileName       : DevManifestProvider
 * author         : MiracleCat
 * date           : 2025-08-12
 * description    :
 */
@Component
@Profile("local")
public class LocalManifestProvider implements ManifestProvider {

    private final String manifestUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    // 간단 캐시(필요시 TTL 조정)
    private volatile Map<String, Object> cache;
    private final AtomicLong lastLoaded = new AtomicLong(0);
    private static final long CACHE_MS = 1000; // 1초 캐시

    public LocalManifestProvider(@Value("${frontend.manifest-url}") String manifestUrl) {
        this.manifestUrl = manifestUrl;
    }

    @Override
    public Map<String, Object> loadManifest() {
        long now = System.currentTimeMillis();
        if (cache == null || now - lastLoaded.get() > CACHE_MS) {
            ResponseEntity<Map> resp = restTemplate.getForEntity(manifestUrl, Map.class);
            cache = resp.getBody();
            lastLoaded.set(now);
        }
        return cache;
    }
}