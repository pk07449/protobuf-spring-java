package com.google.protobuf.spring.http;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.web.client.RestTemplate;

/**
 *
 */
public class ProtobufRestTemplateFactoryBean extends AbstractFactoryBean {
    public Class getObjectType() {
        return RestTemplate.class;
    }

    protected Object createInstance() throws Exception {
        RestTemplate template = new RestTemplate();
        template.getMessageConverters().add(new ProtobufHttpMessageConverter());
        return template;
    }
}
