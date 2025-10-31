package com.chatflow.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageType {
    TEXT, JOIN, LEAVE;

    // From JSON to java
    @JsonCreator
    public static MessageType fromString(String value) {
        return MessageType.valueOf(value.toUpperCase());
    }

    // From java to JSON
    @JsonValue
    public String toValue() {
        return this.name();
    }
}