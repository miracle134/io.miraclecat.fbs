package io.miraclecat.fbs.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Map;

/**
 * packageName    : io.miraclecat.fbs.core.config
 * fileName       : ClasspathManifestProvider
 * author         : MiracleCat
 * date           : 2025-08-12
 * description    : 빌드에 포함된 manifest.json 사용 (local 외 dev / prod)
 */
@Component
@Profile("!local")
public class ClasspathManifestProvider implements ManifestProvider {

    private final Resource manifestResource;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClasspathManifestProvider(@Value("${frontend.manifest-classpath}") Resource manifestResource) {
        this.manifestResource = manifestResource;
    }

    @Override
    public Map<String, Object> loadManifest() {
        try (InputStream is = manifestResource.getInputStream()) {
            return objectMapper.readValue(is, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load manifest.json", e);
        }
    }
}
