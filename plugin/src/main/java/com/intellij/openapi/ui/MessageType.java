package com.intellij.openapi.ui;
/** IntelliJ-compat stub for balloon message severity. */
public final class MessageType {
    public static final MessageType INFO = new MessageType("INFO");
    public static final MessageType WARNING = new MessageType("WARNING");
    public static final MessageType ERROR = new MessageType("ERROR");
    private final String name;
    private MessageType(String name) { this.name = name; }
    public String getName() { return name; }
    public String toString() { return name; }
}
