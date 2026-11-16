package io.gymcrm.web.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller parameter of type {@link io.gymcrm.dto.Credentials} that is filled from the
 * request's {@code Authorization: Basic ...} header by {@link BasicAuthArgumentResolver}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface BasicAuth {
}
