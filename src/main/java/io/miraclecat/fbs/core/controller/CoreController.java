package io.miraclecat.fbs.core.controller;

import io.miraclecat.fbs.core.response.ApiEntity;
import io.miraclecat.fbs.core.response.HttpsStatusCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.TemplateEngine;

/**
 * packageName    : io.miraclecat.fbs.core.controller
 * fileName       : CoreController
 * author         : MiracleCat 
 * date           : 2024-09-27
 * description    :
 */
@RestController
@RequiredArgsConstructor
public class CoreController {

    private final ApplicationContext applicationContext;
	
	/**
	 * 메인
	 */
	@GetMapping("/")
	public ModelAndView main() {
		return new ModelAndView("/page/member/main");
	}

	/**
	 * 템플릿 캐시 초기화 (개발사 전용, 상태 변경이라 POST)
	 */
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @PostMapping("/clearCache")
    public ApiEntity<?> clearCache() {
		GlobalModelAdvice.updateBuildStamp();
        applicationContext.getBeansOfType(TemplateEngine.class)
                .values()
                .forEach(TemplateEngine::clearTemplateCache);

        return ApiEntity.of(HttpsStatusCode.CLEAR_CACHE_SUCCESS);
    }

}

