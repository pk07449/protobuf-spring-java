package com.google.protobuf.spring.http.client;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.spring.http.ProtobufHttpMessageConverter;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public abstract class ClientHttpResponseObjectFuture<T> implements Future<ClientHttpResponse> {
    private static final Logger log = Logger.getLogger(AbstractClientHttpInvocationHandler.class.getName());
    private final Class<? extends T> type;
    private ResponseCallback responseCallback;
    private final Future responseFuture;
    private int timeout;

    private ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();

    private static final ConcurrentHashMap<Class, Method> parseFromMethodCache = new ConcurrentHashMap<Class, Method>();
    private static final ConcurrentHashMap<Class, Method> defaultInstanceMethodCache = new ConcurrentHashMap<Class, Method>();

    private Object convertedObject;

    private static boolean isProtobuf23;
    private static Class extensionRegistryClass;

    static {
        try {
            extensionRegistryClass = Class.forName("com.google.protobuf.ExtensionRegistryLite");
            isProtobuf23 = true;
        } catch (ClassNotFoundException e) {
            extensionRegistryClass = com.google.protobuf.ExtensionRegistry.class;
            isProtobuf23 = false;
        }
    }

    public ClientHttpResponseObjectFuture(Class<? extends T> type,
                                    Future responseFuture,
                                    ClientHttpRequestContext requestContext) {
        this.type = type;
        this.responseCallback = requestContext.getResponseCallback();
        this.responseFuture = responseFuture;
        this.timeout = requestContext.getTimeout();
        if (requestContext.getExtensionRegistry() != null) {
            this.extensionRegistry = requestContext.getExtensionRegistry();
        }
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return responseFuture.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
        return responseFuture.isCancelled();
    }

    public boolean isDone() {
        return responseFuture.isDone();
    }

    public ResponseCallback getResponseCallback() {
        return responseCallback;
    }

    public int getTimeout() {
        return timeout;
    }

    public abstract ClientHttpResponse get() throws InterruptedException, ExecutionException;

    public abstract ClientHttpResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

    @SuppressWarnings({"unchecked"})
    public T getObject() throws ExecutionException, TimeoutException, InterruptedException, IOException {
        return getObject(getTimeout(), AbstractClientHttpInvocationHandler.DEFAULT_TIMEOUT_UNIT);
    }

    @SuppressWarnings({"unchecked"})
    public T getObject(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException, InterruptedException, IOException {
        ClientHttpResponse response = get(timeout, unit);
        if (responseCallback != null)
            responseCallback.doWithResponse(response);

        if (response.getStatusCode().compareTo(HttpStatus.MULTIPLE_CHOICES) >= 0) {
            throw new HttpClientInvocationResponseException(response);
        } else {
            return (T) convertResponseToObject(type, response);
        }
    }

    protected Future getInnerFuture() {
        return responseFuture;
    }

    private Object convertResponseToObject(Class returnType, ClientHttpResponse response) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Converting response to " + returnType);
        }
        if (convertedObject != null) {
            return convertedObject;
        }
        if (Void.TYPE == returnType) {
            convertedObject = null;
            return convertedObject;
        }
        try {
            if (String.class == returnType) {
                convertedObject = ProtobufHttpMessageConverter.convertInputStreamToString(response.getBody());
                return convertedObject;
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to convert response to String object", e);
        }

        try {
            InputStream body = response.getBody();
            if (body != null && body.available() > 0) {
                Method m = parseFromMethodCache.get(returnType);
                if (m == null) {
                    m = returnType.getMethod("parseFrom", InputStream.class, extensionRegistryClass);
                    parseFromMethodCache.put(returnType, m);
                }
                convertedObject = m.invoke(returnType, body, extensionRegistry);
            } else {
                Method m = defaultInstanceMethodCache.get(returnType);
                if (m == null) {
                    m = returnType.getMethod("getDefaultInstance");
                    defaultInstanceMethodCache.put(returnType, m);
                }
                convertedObject = m.invoke(returnType);
            }
            return convertedObject;
        } catch (Exception e) {
            throw new RuntimeException("Unable to convert response to Proto object", e);
        }
    }
}
