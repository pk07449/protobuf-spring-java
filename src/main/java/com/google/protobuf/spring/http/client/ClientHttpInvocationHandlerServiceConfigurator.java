package com.google.protobuf.spring.http.client;

/**
 *
 */
public interface ClientHttpInvocationHandlerServiceConfigurator {
    public String getServiceBaseUrl(String serviceName);
    public int getServiceTimeoutInSeconds(String serviceName);
}
