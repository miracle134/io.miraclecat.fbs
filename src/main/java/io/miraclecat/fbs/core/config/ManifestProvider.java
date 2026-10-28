package io.miraclecat.fbs.core.config;

import java.util.Map;

/**
 * packageName    : io.miraclecat.fbs.core.config
 * fileName       : ManifestProvider
 * author         : MiracleCat 
 * date           : 2025-08-12
 * description    :
 */
public interface ManifestProvider {
	Map<String, Object> loadManifest();
}
