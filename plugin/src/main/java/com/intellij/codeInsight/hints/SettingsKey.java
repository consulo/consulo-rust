package com.intellij.codeInsight.hints;
/** Typed identifier for an inlay hint settings entry. */
public final class SettingsKey<T> {
    private final String id;
    public SettingsKey(String id) { this.id = id; }
    public String getId() { return id; }
}
