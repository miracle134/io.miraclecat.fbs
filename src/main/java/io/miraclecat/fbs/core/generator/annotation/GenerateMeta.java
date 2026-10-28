package io.miraclecat.fbs.core.generator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * packageName    : io.miraclecat.fbs.core.generator.annotation
 * fileName       : GenerateMeta
 * author         : MiracleCat
 * date           : 2025-06-11
 * description    :
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GenerateMeta {
	String domain() default "";
	boolean controller() default true;
	boolean service() default true;
	boolean dto() default true; // dto가 false면 controller, service, js 모두 불가
	boolean js() default true;
}