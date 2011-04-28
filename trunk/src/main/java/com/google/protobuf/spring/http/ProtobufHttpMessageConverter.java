package com.google.protobuf.spring.http;

import com.google.protobuf.*;
import com.google.protobuf.spring.ExtensionRegistryInitializer;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.FileCopyUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @since Spring 3.0
 */
public class ProtobufHttpMessageConverter extends AbstractHttpMessageConverter<Message> {
    public static String LS = System.getProperty("line.separator");
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    public static final MediaType PROTOBUF = new MediaType("application", "x-protobuf", DEFAULT_CHARSET);
    public static final MediaType XML = new MediaType("application", "xml", DEFAULT_CHARSET);
    public static final MediaType JSON = new MediaType("application", "json", DEFAULT_CHARSET);
    public static final MediaType TEXT = new MediaType("text", "plain");
    public static final MediaType HTML = new MediaType("text", "html");
    private ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();

    private static final ConcurrentHashMap<Class, Method> newBuilderMethodCache = new ConcurrentHashMap<Class, Method>();

    public ProtobufHttpMessageConverter() {
        this(null);
    }

    public ProtobufHttpMessageConverter(ExtensionRegistryInitializer registryInitializer) {
        super(PROTOBUF, JSON, HTML, TEXT, XML);
        initializeExtentionRegistry(registryInitializer);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Message.class.isAssignableFrom(clazz);
    }

    @Override
    protected Message readInternal(Class<? extends Message> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        MediaType contentType = inputMessage.getHeaders().getContentType();
        contentType = contentType != null ? contentType : PROTOBUF;
        try {
            Method m = getNewBuilderMessageMethod(clazz);
            Message.Builder builder = (Message.Builder) m.invoke(clazz);
            if (isJson(contentType)) {
                String data = convertInputStreamToString(inputMessage.getBody());
                String serverHeader = inputMessage.getHeaders().getFirst("Server");
                if (serverHeader != null && serverHeader.contains("CouchDB"))
                    CouchDBFormat.merge(data, extensionRegistry, builder);
                else
                    JsonFormat.merge(data, extensionRegistry, builder);
                
            } else if (isText(contentType)) {
                String data = convertInputStreamToString(inputMessage.getBody());
                TextFormat.merge(data, extensionRegistry, builder);
            } else if (isXml(contentType)) {
                String data = convertInputStreamToString(inputMessage.getBody());
                XmlFormat.merge(data, extensionRegistry, builder);
            } else {
                InputStream is = inputMessage.getBody();
                builder.mergeFrom(is, extensionRegistry);
            }
            return builder.build();
        } catch (Exception e) {
            throw new HttpMessageNotReadableException("Unable to convert inputMessage to Proto object", e);
        }
    }

    @Override
    protected void writeInternal(Message message, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        MediaType contentType = outputMessage.getHeaders().getContentType();
        Charset charset = contentType.getCharSet() != null ? contentType.getCharSet() : DEFAULT_CHARSET;

        if (isHtml(contentType)) {
            String data = HtmlFormat.printToString(message);
            FileCopyUtils.copy(data.getBytes(charset), outputMessage.getBody());
        } else if (isJson(contentType)) {
            String data = JsonFormat.printToString(message);
            FileCopyUtils.copy(data.getBytes(charset), outputMessage.getBody());
        } else if (isText(contentType)) {
            String data = TextFormat.printToString(message);
            FileCopyUtils.copy(data.getBytes(charset), outputMessage.getBody());
        } else if (isXml(contentType)) {
            String data = XmlFormat.printToString(message);
            FileCopyUtils.copy(data.getBytes(charset), outputMessage.getBody());
        } else {
            FileCopyUtils.copy(message.toByteArray(), outputMessage.getBody());
        }
    }

    @Override
    protected MediaType getDefaultContentType(Message message) {
        return PROTOBUF;
    }

    @Override
    protected Long getContentLength(Message message, MediaType contentType) {
        Charset charset = contentType.getCharSet() != null ? contentType.getCharSet() : DEFAULT_CHARSET;

        if (isHtml(contentType)) {
            String data = HtmlFormat.printToString(message);
            return (long) data.getBytes(charset).length;
        } else if (isJson(contentType)) {
            String data = JsonFormat.printToString(message);
            return (long) data.getBytes(charset).length;
        } else if (isText(contentType)) {
            String data = TextFormat.printToString(message);
            return (long) data.getBytes(charset).length;
        } else if (isXml(contentType)) {
            String data = XmlFormat.printToString(message);
            return (long) data.getBytes(charset).length;
        } else {
            return (long) message.toByteArray().length;
        }
    }

    protected boolean isJson(MediaType contentType) {
        return JSON.getType().equals(contentType.getType()) && JSON.getSubtype().equals(contentType.getSubtype());
    }

    protected boolean isText(MediaType contentType) {
        return TEXT.getType().equals(contentType.getType()) && TEXT.getSubtype().equals(contentType.getSubtype());
    }

    protected boolean isXml(MediaType contentType) {
        return XML.getType().equals(contentType.getType()) && XML.getSubtype().equals(contentType.getSubtype());
    }

    protected boolean isHtml(MediaType contentType) {
        return HTML.getType().equals(contentType.getType()) && HTML.getSubtype().equals(contentType.getSubtype());
    }

    private Method getNewBuilderMessageMethod(Class<? extends Message> clazz) throws NoSuchMethodException {
        Method m = newBuilderMethodCache.get(clazz);
        if (m == null) {
            m = clazz.getMethod("newBuilder");
            newBuilderMethodCache.put(clazz, m);
        }
        return m;
    }

    private void initializeExtentionRegistry(ExtensionRegistryInitializer registryInitializer) {
        if (registryInitializer != null) {
            registryInitializer.initializeExtensionRegistry(extensionRegistry);
        }
    }

    public static String convertInputStreamToString(InputStream io) {
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(io, "UTF-8"));
            String line = reader.readLine();
            while (line != null) {
                sb.append(line).append(LS);
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to obtain an InputStream", e);

        }
        return sb.toString();
    }

}
