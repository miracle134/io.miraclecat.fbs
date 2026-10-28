package io.miraclecat.fbs.core.generator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * packageName    : io.miraclecat.fbs.core.generator.annotation
 * fileName       : FieldMeta
 * author         : MiracleCat
 * date           : 2025-06-11
 * description    :
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldMeta {
	boolean use() default true;
	boolean select() default true;
	boolean insert() default true;
	boolean update() default false;
	boolean delete() default false;
	boolean where() default false;
	
}