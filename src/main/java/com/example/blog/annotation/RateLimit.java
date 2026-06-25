package com.example.blog.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    int seconds() default 60;

    int maxRequests() default 1;

    String keyPrefix() default "rate_limit";
}
