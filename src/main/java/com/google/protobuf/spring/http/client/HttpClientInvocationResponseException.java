package com.google.protobuf.spring.http.client;

import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 *
 */
public class HttpClientInvocationResponseException extends RuntimeException {
    public HttpClientInvocationResponseException(ClientHttpResponse response) throws IOException {
        super(buildExceptionText(response));
    }

    public HttpClientInvocationResponseException(ClientHttpResponse response, Throwable cause) throws IOException {
        super(buildExceptionText(response), cause);
    }

    private static String buildExceptionText(ClientHttpResponse response) throws IOException {
        return "Failed Service Invocation. requestURL: " + //response.getRequestUrl() + //TODO get ClientHttpResponse to expose a requestURI
                "\nstatusCode: " + response.getStatusCode() +
                "\nstatusMessage: " + response.getStatusText() +
                "\nserver: " + response.getHeaders().get("Server");
    }
}
