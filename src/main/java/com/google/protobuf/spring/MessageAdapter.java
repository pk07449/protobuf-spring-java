package com.google.protobuf.spring;

import com.google.protobuf.Message;


public interface MessageAdapter<T> {

    public Message buildFrom(T object);
}
