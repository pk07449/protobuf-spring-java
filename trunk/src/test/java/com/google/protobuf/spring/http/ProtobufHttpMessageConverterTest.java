package com.google.protobuf.spring.http;

import com.google.protobuf.spring.http.proto.model.Tester;
import com.google.protobuf.Message;
import junit.framework.TestCase;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;

/**
 * 
 */
public class ProtobufHttpMessageConverterTest extends TestCase {

    @SuppressWarnings("unchecked")
    public void testBasics() {
        Tester msg = Tester.newBuilder().setId(1).build();
        HttpMessageConverter converter = new ProtobufHttpMessageConverter();

        assertTrue(converter.canRead(msg.getClass(), ProtobufHttpMessageConverter.PROTOBUF));   
    }

    @SuppressWarnings("unchecked")
    public void testConvert() throws IOException {
        Tester msg = Tester.newBuilder().setId(1).build();
        HttpMessageConverter<Message> converter = new ProtobufHttpMessageConverter();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpOutputMessage out = new ServletServerHttpResponse(response);

        converter.write(msg, ProtobufHttpMessageConverter.PROTOBUF, out);

        assertEquals(msg.toByteArray(), response.getContentAsByteArray());

        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpInputMessage in = new ServletServerHttpRequest(request);
        request.setContent(msg.toByteArray());

        Message converted = converter.read(msg.getClass(), in);

        assertEquals(converted, msg);
    }
}
