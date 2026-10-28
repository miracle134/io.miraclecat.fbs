package io.miraclecat.fbs.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

/**
 * packageName    : io.miraclecat.fbs.core.config
 * fileName       : WebConfig
 * author         : MiracleCat
 * date           : 2023-07-19
 * description    : 정적 리소스 (webpack 번들은 해시 파일명이라 장기 캐시, manifest는 캐시 안 함)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/images/**")
			.addResourceLocations("classpath:/static/images/");
		registry.addResourceHandler("/css/**")
			.addResourceLocations("classpath:/static/css/");
		registry.addResourceHandler("/js/manifest.json")
			.addResourceLocations("classpath:/static/js/manifest.json")
			.setCacheControl(CacheControl.noStore());
		registry.addResourceHandler("/js/**")
			.addResourceLocations("classpath:/static/js/")
			.setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
		registry.addResourceHandler("/lib/**")
			.addResourceLocations("classpath:/static/lib/");
		registry.addResourceHandler("/fonts/**")
			.addResourceLocations("classpath:/static/fonts/");
		registry.addResourceHandler("/favicon/**")
			.addResourceLocations("classpath:/static/");
	}
}
