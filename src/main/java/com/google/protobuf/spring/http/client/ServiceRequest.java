package com.google.protobuf.spring.http.client;

import org.springframework.http.HttpMethod;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ServiceRequest {
    /**
	 * The service name to bind to.
	 */
    public abstract String name() default "";

    public String pattern() default "";

    /**
     * Valid options:
     * <ul>
     * <li>protobuf</li>
     * <li>json</li>
     * <li>text</li>
     * <li>xml</li>
     * </ul>
     * @return
     */
    public String contentType() default "application/x-protobuf";

    public HttpMethod method();
}