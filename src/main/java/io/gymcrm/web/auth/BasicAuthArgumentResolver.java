package io.gymcrm.web.auth;

import io.gymcrm.dto.Credentials;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Fills {@code @BasicAuth Credentials} controller parameters from the Authorization header.
 */
@Component
public class BasicAuthArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(BasicAuth.class)
                && Credentials.class.equals(parameter.getParameterType());
    }

    @Override
    public Credentials resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                       NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return BasicAuthParser.parse(webRequest.getHeader(HttpHeaders.AUTHORIZATION));
    }
}
