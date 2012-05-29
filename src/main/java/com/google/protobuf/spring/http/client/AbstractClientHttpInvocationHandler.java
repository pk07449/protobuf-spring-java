package com.google.protobuf.spring.http.client;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.spring.ExtensionRegistryInitializer;
import com.google.protobuf.spring.http.ProtobufHttpMessageConverter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public abstract class AbstractClientHttpInvocationHandler implements ClientHttpInvocationHandler {
    private static final Logger log = Logger.getLogger(AbstractClientHttpInvocationHandler.class.getName());

    private RestTemplate restTemplate;
    private ClientHttpInvocationHandlerServiceConfigurator config;

    private ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
    private static final ConcurrentHashMap<Method, Class> methodReturnTypeCache = new ConcurrentHashMap<Method, Class>();

    public static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

    static {
        try {
            Class.forName("javax.servlet.http.Cookie");
        } catch (ClassNotFoundException e) {
            log.log(Level.SEVERE, "You need to have javax.servlet.http.Cookie or servlet-api in your classpath or depend on a servlet container", e);
        }
    }

    public AbstractClientHttpInvocationHandler(ClientHttpInvocationHandlerServiceConfigurator config) {
        this(null, config, null);
    }

    public AbstractClientHttpInvocationHandler(ClientHttpRequestFactory factory,
                                               ClientHttpInvocationHandlerServiceConfigurator config) {
        this(factory, config, null);
    }

    public AbstractClientHttpInvocationHandler(ClientHttpInvocationHandlerServiceConfigurator config,
                                               ExtensionRegistryInitializer registryInitializer) {
        this(null, config, registryInitializer);
    }

    public AbstractClientHttpInvocationHandler(ClientHttpRequestFactory factory,
                                               ClientHttpInvocationHandlerServiceConfigurator config,
                                               ExtensionRegistryInitializer registryInitializer) {
        if (factory == null) {
            this.restTemplate = new RestTemplate();
        } else {
            this.restTemplate = new RestTemplate(factory);
        }
        this.restTemplate.getMessageConverters().add(new ProtobufHttpMessageConverter(registryInitializer));
        this.config = config;
        if (registryInitializer != null) {
            registryInitializer.initializeExtensionRegistry(extensionRegistry);
        }
    }

    protected ExtensionRegistry getExtensionRegistry() {
        return extensionRegistry;
    }

    /**
     * This method provides the implementation of the <code>invoke</code> method for the <code>InvocationHandler</code>.<br/>
     * The order of execution is the following:
     * <ol>
     * <li>Retrieve the list of <code>ParamName</code> markers</li>
     * <li>Retrieve the <code>ServiceName</code></li>
     * <li>Retrieve the <code>LapsangServiceCallback</code> if provided</li>
     * <li>Using the name, lookup <i>baseUrl</i>, <i>timeout<i> and <i>pattern</i> from <i>config</i></li>
     * <li>Retrieve <code>RequestMethod</code> from the method name based on convention</li>
     * <li>Generate request url using given <code>ParamName</code> list</li>
     * <li>Make HTTP request</li>
     * <li>
     *  Unmarshal response into <code>Message</code> object determined by the method return type.
     *  If the return type is of type <code>ResponseFuture</code> the response future will be returned.
     *  If the return type is of type <code>HttpResponseMessage</code> the response message will be returned.
     *  In both cases it is then up to the caller to do the response processing and error handling.
     * </li>
     * </ol>
     *
     * @param	proxy the proxy instance that the method was invoked on
     *
     * @param	method the <code>Method</code> instance corresponding to
     * the interface method invoked on the proxy instance.  The declaring
     * class of the <code>Method</code> object will be the interface that
     * the method was declared in, which may be a superinterface of the
     * proxy interface that the proxy class inherits the method through.
     *
     * @param	args an array of objects containing the values of the
     * arguments passed in the method invocation on the proxy instance,
     * or <code>null</code> if interface method takes no arguments.
     * Arguments of primitive types are wrapped in instances of the
     * appropriate primitive wrapper class, such as
     * <code>java.lang.Integer</code> or <code>java.lang.Boolean</code>.
     *
     * If the Argument is annotated with </code>ParamName</code>, it will be used in
     * the request URL construction.
     * If the Argument is of type <code>com.google.protobuf.Message</code>,
     * it will be used as a payload for the request.
     * If the Argument is of type <code>LapsangServiceCallback</code>, it will be called
     * after the HTTP request completes.
     *
     * @return <code>Message</code> object representing the response of the service call.
     *
     * @throws	Throwable the exception to throw from the method
     * invocation on the proxy instance.  The exception's type must be
     * assignable either to any of the exception types declared in the
     * <code>throws</code> clause of the interface method or to the
     * unchecked exception types <code>java.lang.RuntimeException</code>
     * or <code>java.lang.Error</code>.  If a checked exception is
     * thrown by this method that is not assignable to any of the
     * exception types declared in the <code>throws</code> clause of
     * the interface method, then an
     * {@link java.lang.reflect.UndeclaredThrowableException} containing the
     * exception that was thrown by this method will be thrown by the
     * method invocation on the proxy instance.
     *
     * @see com.google.protobuf.Message
     * @see RequestParam
     * @see ServiceRequest
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        ClientHttpRequestContext requestContext = createRequestContext(method, args);

        boolean returnResponseFuture = (Future.class.isAssignableFrom(method.getReturnType()));
        boolean returnHttpResponseMessage = (ClientHttpResponse.class.isAssignableFrom(method.getReturnType()));
        boolean returnHttpResponseObjectFuture = (ClientHttpResponseObjectFuture.class.isAssignableFrom(method.getReturnType()));

        // Special check, must be done before the result is available, to provide async behavior
        if (returnHttpResponseObjectFuture) {
            //return this.doHttpResponseObjectFuture(requestContext);
        }

        if (returnResponseFuture) {
            //return this.doResponseFeature(requestContext);
        }

        // Special check, must be done before callback is invoked, to provide native response processing behavior
        if (returnHttpResponseMessage) {
            return this.doHttpResponse(requestContext);
        }

        return this.doObject(requestContext);
    }

    public ClientHttpInvocationHandlerServiceConfigurator getHttpInvocationHandlerServiceConfigurator() {
        return config;
    }

    /**
     * Scans the provided <i>method</i> for <code>RequestParam</code> annotations.<br/>
     * The returned <code>RequestParam</code> array will be the size of the number of method arguments.<br/>
     * If any of the method arguments does not have <code>RequestParam</code> annotation, that array bucket will be <code>null</code><br/>
     * @param method which will be scanned for <code>RequestParam</code> annotations.
     * @return <code>RequestParam</code> array with the correpsonding number of elements as the number of method arguments.
     */
    public static RequestParam[] retrieveRequestParams(Method method) {
        Annotation[][] annotations = method.getParameterAnnotations();
        RequestParam[] paramNames = new RequestParam[annotations.length];
        for (int i = 0; i < annotations.length; i++) {
            Annotation[] anns = annotations[i];
            paramNames[i] = null;
            for (Annotation ann : anns) {
                if (RequestParam.class.isInstance(ann)) {
                    paramNames[i] = (RequestParam) ann;
                    break;
                }
            }
        }
        return paramNames;
    }

    /**
     * Scans the provided <i>method</i> for <code>RequestContent</code> annotations.<br/>
     * If <i>args</i> array has only one object and it is of type <code>Message</code>, it will be returned by default.<br/>
     * @param method which will be scanned for <code>RequestContent</code> annotations.
     * @param args array with method invocation arguments.
     * @return <code>RequestContent</code> object or null.
     */
    public static Message retrieveRequestBody(Method method, Object[] args) {
        if (args != null && args.length == 1 && args[0] != null && Message.class.isAssignableFrom(args[0].getClass()))
            return (Message) args[0];

        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            Annotation[] anns = annotations[i];
            for (Annotation ann : anns) {
                if (RequestBody.class.isInstance(ann) && args[i] != null && Message.class.isInstance(args[i])) {
                    return (Message) args[i];
                }
            }
        }
        return null;
    }

    /**
     * Converts the methodName to an HTTP Method based on the following conventions:
     * <ul>
     * <li><b>get</b><i>Name</i>: GET</li>
     * <li><b>delete</b><i>Name</i>: DELETE</li>
     * <li><b>set</b><i>Name</i>: PUT</li>
     * <li><b>update</b><i>Name</i>: PUT</li>
     * <li><b>create</b><i>Name</i>: POST</li>
     * <li><b>default</b><i>Name</i>: POST</li>
     * </ul>
     *
     * @param methodName
     *            <code>String</code> representing the invoked method name
     * @return HttpMethod enum representing the HTTP method
     */
    public static HttpMethod retrieveMethodType(String methodName) {
        if (methodName.startsWith("get")) {
            return HttpMethod.GET;
        } else if (methodName.startsWith("delete")) {
            return HttpMethod.DELETE;
        } else if (methodName.startsWith("set") || methodName.startsWith("update")) {
            return HttpMethod.PUT;
        } else if (methodName.startsWith("create")) {
            return HttpMethod.POST;
        } else {
            return HttpMethod.POST;
        }
    }

    public static Class retrieveHttpResponseObjectFutureGenericsType(Method method) {
        if (ClientHttpResponseObjectFuture.class != method.getReturnType()) {
            throw new IllegalArgumentException("Method return type MUST be of " +
                    ClientHttpResponseObjectFuture.class.getName() + ", instead it is " + method.getReturnType().getName());
        }
        Class returnTypeClass = methodReturnTypeCache.get(method);
        if (returnTypeClass == null) {
            Assert.isInstanceOf(ParameterizedType.class, method.getGenericReturnType());
            ParameterizedType returnType = ((ParameterizedType) method.getGenericReturnType());
            Assert.state(returnType.getActualTypeArguments().length == 1);

            returnTypeClass = (Class) returnType.getActualTypeArguments()[0];
            methodReturnTypeCache.put(method, returnTypeClass);
        }
        return returnTypeClass;
    }

    public Object doObject(ClientHttpRequestContext requestContext) throws URISyntaxException {
    	/*
        switch (requestContext.getRequestMethod()) {
            case GET:
                return restTemplate.getForObject(new URI(requestContext.getRequestUrl()),
                        requestContext.getReturnObjectType());
            case PUT:
            case POST:
            case DELETE:
        }
        return null;
        */
    	
    	HttpHeaders requestHeaders = new HttpHeaders();
    	java.util.List<MediaType> mediaTypes = java.util.Arrays.asList(new MediaType[] {requestContext.getResponseDesiredContentType()});
    	requestHeaders.setAccept(mediaTypes);
//    	requestHeaders.set("Connection", "close");
    	HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
    	ResponseEntity<?> response = restTemplate.exchange(new URI(requestContext.getRequestUrl()), requestContext.getRequestMethod(), requestEntity, requestContext.getReturnObjectType());
    	return response.getBody();
        
        
    }

    public ClientHttpResponse doHttpResponse(ClientHttpRequestContext requestContext) throws URISyntaxException {
       return restTemplate.getForEntity(new URI(requestContext.getRequestUrl()),
               ClientHttpResponse.class).getBody();
    }

    protected ClientHttpRequestContext createRequestContext(Method method, Object[] args) throws MalformedURLException {
        return new RequestContext(method, args, getExtensionRegistry(),
                getHttpInvocationHandlerServiceConfigurator());
    }

    private void validateRequestConditions(Method method, MediaType contentType) {
        if (method.getReturnType() == String.class &&
                contentType == ProtobufHttpMessageConverter.PROTOBUF) {
            throw new IllegalArgumentException("Only text content types can be used when the return type is String, but not " + contentType);
        }
    }

    private ResponseCallback retrieveCallback(Object[] args) {
        if (args == null)
            return null;
        for (Object o : args) {
            if (o != null && ResponseCallback.class.isAssignableFrom(o.getClass()))
                return (ResponseCallback) o;
        }
        return null;
    }

//    protected static String encodeMessageToBase64(Message msg) throws UnsupportedEncodingException {
//        return new String(Base64.encodeBase64(msg.toByteArray()), "UTF-8");
//    }

    private String generateRequestUrl(Object[] args, RequestParam[] paramNames, String pattern) {
        return generateRequestUrl(args, paramNames, pattern, false);
    }
    private String generateRequestUrl(Object[] args, RequestParam[] paramNames, String pattern, boolean patternOnly) {
        if (pattern == null)
            pattern = "";
        if (args == null)
            return pattern;
        if (!patternOnly)
            pattern = pattern + "?";
        for (int i = 0; i < args.length; i++) {
            if (paramNames[i] == null) {
                if (args[i] == null || !Message.class.isAssignableFrom(args[i].getClass())) {
                    // TODO: Possibly throw an exception instead!
                    log.log(Level.WARNING, "Argument at index " + i + " does not have a ParamName annotation and will be ignored!");
                    continue;
                }
            }
            if (args[i] == null && paramNames[i].required()) {
                throw new IllegalArgumentException(paramNames[i].value() + " is a required parameter!!!");
            }
            if (args[i] != null) {
                if (ResponseCallback.class.isAssignableFrom(args[i].getClass())) {
                    continue;
                }
                String token = "${" + paramNames[i].value() + "}";
                if (pattern.contains(token)) {
                    if (args[i].getClass().isArray()) {
                        throw new IllegalArgumentException("An array can not be used as an argument for " + token + " as spart of the URL path");
                    }
                    pattern = pattern.replace(token, urlEncode(args[i].toString()));
                } else if (!patternOnly && args[i].getClass().isArray()) {
                    for (int j = 0; j < Array.getLength(args[i]); j++) {
                        if (Array.get(args[i], j) == null) {
                            continue;
                        }
                        pattern = pattern + "&" + paramNames[i].value() + "=" + urlEncode(Array.get(args[i], j).toString());
                    }
                } else if (!patternOnly) {
                    pattern = pattern + "&" + paramNames[i].value() + "=" + urlEncode(args[i].toString());
                }
            }
        }
        return pattern;
    }

    private String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to convert " + string + " to UTF-8", e);
        }
    }

    public class RequestContext implements ClientHttpRequestContext {
        private Class returnType;
        private String url;
        private MediaType responseDesiredContentType;
        private HttpMethod requestMethod;
        private Message msg;
        private String serviceIdentifier;
        private String serviceIdentifierHeaderName;
        private int timeout = 30;
        private ResponseCallback callback;
        private ExtensionRegistry extensionRegistry;

        private RequestContext(Method method, Object[] args, ExtensionRegistry extensionRegistry,
                               ClientHttpInvocationHandlerServiceConfigurator config) throws MalformedURLException {
            this.extensionRegistry = extensionRegistry;
            RequestParam[] paramNames = retrieveRequestParams(method);
            ServiceRequest service = method.getAnnotation(ServiceRequest.class);
            callback = retrieveCallback(args);

            if (service == null)
                throw new RuntimeException("ServiceName annotation must be present on the method!");

            String serviceName = service.name();

            String baseUrl = config.getServiceBaseUrl(serviceName);
            Assert.notNull(baseUrl, "Base URL for the Service " + serviceName + " is not found in config!");
            String pattern = service.pattern();
            if (config.getServiceTimeoutInSeconds(serviceName) > 0) {
                timeout = config.getServiceTimeoutInSeconds(serviceName);
            }

            requestMethod = retrieveMethodType(method.getName());
            if (service.method() != null) {
                requestMethod = service.method();
            }
            msg = retrieveRequestBody(method, args);
            url = baseUrl;
            if (msg != null) {
                if (StringUtils.hasText(pattern)) {
                    // TODO: Convert MSG to patternized URL
                    // log.warn("A Message argument can't be converted into an Absolute URL Path Pattern: " + pattern);
                    // log.debug("Message Object: " + args[0]);
                    url = url + generateRequestUrl(args, paramNames, pattern, true);
                }
            } else {
                url = url + generateRequestUrl(args, paramNames, pattern);
            }

            responseDesiredContentType = MediaType.parseMediaType(service.contentType());
            serviceIdentifier = new URL(baseUrl).getPath();
            serviceIdentifierHeaderName = "REST" + "NAME"; //TODO: Come up with a header name
            validateRequestConditions(method, responseDesiredContentType);

            if (ClientHttpResponseObjectFuture.class.isAssignableFrom(method.getReturnType())) {
                returnType = retrieveHttpResponseObjectFutureGenericsType(method);
            } else {
                returnType = method.getReturnType();
            }
        }

        public Class getReturnObjectType() {
            return returnType;
        }

        public String getRequestUrl() {
            return url;
        }

        public MediaType getResponseDesiredContentType() {
            return responseDesiredContentType;
        }

        public HttpMethod getRequestMethod() {
            return requestMethod;
        }

        public Message getInputMessage() {
            return msg;
        }

        public String getServiceIdentifier() {
            return serviceIdentifier;
        }

        public String getServiceIdentifierHeaderName() {
            return serviceIdentifierHeaderName;
        }

        public int getTimeout() {
            return timeout;
        }

        public ResponseCallback getResponseCallback() {
            return callback;
        }

        public void addCallback(ResponseCallback callback) {
            if (this.callback == null)
                this.callback = callback;
            else
                this.callback = new CallbackChain(callback, this.callback);
        }

        public ExtensionRegistry getExtensionRegistry() {
            return extensionRegistry;
        }

        private class CallbackChain implements ResponseCallback {
            private ResponseCallback currentCallback;
            private ResponseCallback nextCallback;

            private CallbackChain(ResponseCallback currentCallback, ResponseCallback nextCallback) {
                this.currentCallback = currentCallback;
                this.nextCallback = nextCallback;
            }

            public ResponseCallback getCurrentCallback() {
                return this.currentCallback;
            }

            public ResponseCallback getNextCallback() {
                return nextCallback;
            }

            public void doWithResponse(ClientHttpResponse response) throws IOException {
                if (currentCallback != null)
                    currentCallback.doWithResponse(response);
                if (nextCallback != null)
                    nextCallback.doWithResponse(response);
            }
        }
    }
}