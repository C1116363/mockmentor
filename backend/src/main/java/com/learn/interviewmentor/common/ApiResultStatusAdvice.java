package com.learn.interviewmentor.common;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Keeps the HTTP status line and {@link ApiResult#status()} in agreement.
 *
 * <h2>The bug this exists to prevent</h2>
 * A facade returning {@code ApiResult.created(...)} puts 201 in the body, but a
 * controller that just returns that object gets Spring's default 200 on the wire.
 * The response then says two different things at once - and a body field that
 * contradicts the status line is worse than not having the field, because
 * whichever one a client trusts, it is sometimes wrong.
 *
 * The alternative was every controller method returning
 * {@code ResponseEntity<ApiResult<T>>} and restating the status by hand, which
 * is the same number twice in two places and exactly how they drift apart.
 *
 * <h2>Why it doesn't clash with the error path</h2>
 * {@code GlobalExceptionHandler} returns {@code ResponseEntity<ApiResult<Void>>}
 * and sets its own status. {@link #supports} only matches handlers whose declared
 * return type is {@code ApiResult} itself, so a ResponseEntity is left alone -
 * its status was set deliberately and must not be second-guessed here.
 *
 * Runs before the body is written, so the response is not yet committed and the
 * status can still be changed.
 */
@ControllerAdvice
public class ApiResultStatusAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return ApiResult.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        if (body instanceof ApiResult<?> result
                && response instanceof ServletServerHttpResponse servletResponse) {
            servletResponse.getServletResponse().setStatus(result.status());
        }
        return body;
    }
}
