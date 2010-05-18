package com.google.protobuf.spring.http.client;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

/**
 * This interface provides definition of the common methods for HttpRequest context.
 * It defines information needed by the invocation handler and varioius http clients to transform the
 *  service interface call into an http request
 */
public interface ClientHttpRequestContext {
    Class getReturnObjectType();

    String getRequestUrl();

    MediaType getResponseDesiredContentType();

    HttpMethod getRequestMethod();

    Message getInputMessage();

    String getServiceIdentifier();

    String getServiceIdentifierHeaderName();

    int getTimeout();

    ResponseCallback getResponseCallback();

    void addCallback(ResponseCallback callback);

    /**
     * This is Google Protocol Buffers specific concept.
     * @return .
     */
    ExtensionRegistry getExtensionRegistry();
}
