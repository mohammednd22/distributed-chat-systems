package com.chatflow.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageType {
    TEXT, JOIN, LEAVE;

    @JsonCreator
    public static MessageType fromString(String value) {
        return MessageType.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}