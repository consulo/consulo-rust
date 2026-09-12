package com.intellij.execution.target;
public abstract class LanguageRuntimeConfiguration {
    private final String typeId;
    protected LanguageRuntimeConfiguration() { this.typeId = ""; }
    protected LanguageRuntimeConfiguration(String typeId) { this.typeId = typeId; }
    public String getTypeId() { return typeId; }
    public final LanguageRuntimeType<?> getType() { return null; }
}
