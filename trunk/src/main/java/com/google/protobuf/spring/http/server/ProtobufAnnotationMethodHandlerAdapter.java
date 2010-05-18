package com.google.protobuf.spring.http.server;

import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;

import com.google.protobuf.spring.http.ProtobufHttpMessageConverter;

import java.util.List;
import java.util.LinkedList;

/**
 * 
 */
public class ProtobufAnnotationMethodHandlerAdapter extends AnnotationMethodHandlerAdapter {
    private String formatParameterName = "format";

    public String getFormatParameterName() {
        return formatParameterName;
    }

    public void setFormatParameterName(String formatParameterName) {
        this.formatParameterName = formatParameterName;
    }

    @Override
    protected HttpInputMessage createHttpInputMessage(HttpServletRequest servletRequest) throws Exception {
        HttpInputMessage input = super.createHttpInputMessage(servletRequest);
        adjustAcceptHeader(servletRequest, input.getHeaders());
        return input;
    }

    private void adjustAcceptHeader(HttpServletRequest servletRequest, HttpHeaders headers) {
        List<MediaType> format = new LinkedList<MediaType>();
        List<MediaType> original = headers.getAccept();
        if (servletRequest.getParameter(formatParameterName) != null) {
            format.add(MediaType.parseMediaType(servletRequest.getParameter(formatParameterName)));
        } else if (!original.isEmpty()) {
            // This is to handle the modern browsers that send application/xml ahead of text/html header
            for (MediaType mediaType : original) {
                if (ProtobufHttpMessageConverter.HTML.isCompatibleWith(mediaType)) {
                    format.add(0, ProtobufHttpMessageConverter.HTML);
                } else {
                    format.add(mediaType);
                }
            }
            if (format.isEmpty()) {
                format = original;
            }
        } else {
            format.add(ProtobufHttpMessageConverter.PROTOBUF);
        }

        headers.setAccept(format);
    }
}
