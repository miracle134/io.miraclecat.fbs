package io.miraclecat.fbs.core.config;

import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * packageName    : io.miraclecat.fbs.core.config
 * fileName       : ReadTx
 * author         : MiracleCat
 * date           : 2025-04-17
 * description    :
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Transactional(readOnly = true)
public @interface ReadTx {
}
