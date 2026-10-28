package io.miraclecat.fbs.core.controller;

import io.miraclecat.fbs.core.utils.common.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.concurrent.atomic.AtomicReference;

/**
 * packageName    : io.miraclecat.fbs.core.controller
 * fileName       : GlobalModelAdvice
 * author         : MiracleCat 
 * date           : 2025-07-04
 * description    :
 */
@Slf4j
@ControllerAdvice
public class GlobalModelAdvice {
	private static final AtomicReference<String> buildStamp = new AtomicReference<>();
	
	static {
		buildStamp.set(StringUtil.randomCodeLower(10));
	}
	
	@ModelAttribute("buildStamp")
	public String getBuildStamp() {
		return buildStamp.get();
	}
	
	public static void updateBuildStamp() {
		buildStamp.set(StringUtil.randomCodeLower(10));
	}

}