package io.miraclecat.fbs.core.config;



import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * packageName    : io.miraclecat.fbs.core.config
 * fileName       : RequiredTx
 * author         : MiracleCat 
 * date           : 2025-04-17
 * description    :
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Transactional(
	propagation = Propagation.REQUIRED,
	rollbackFor = Exception.class
)
public @interface RequiredTx {
}
