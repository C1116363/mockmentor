package com.learn.interviewmentor.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Put this on a controller parameter to get the logged-in User entity:
 *
 *     public Xyz doThing(@CurrentUser User user) { ... }
 *
 * It is a shorthand for @AuthenticationPrincipal(expression = "user"), which
 * tells Spring: take the principal (our AppUserDetails) and call getUser() on it.
 *
 * This is how the server knows who you are without ever trusting a userId sent
 * from the browser.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "user")
public @interface CurrentUser {
}
