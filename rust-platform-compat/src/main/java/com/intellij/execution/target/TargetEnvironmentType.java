package com.intellij.execution.target;

/** Type marker for a {@link TargetEnvironmentConfiguration}. */
public abstract class TargetEnvironmentType<T extends TargetEnvironmentConfiguration> {
    private final String id;
    protected TargetEnvironmentType(String id) { this.id = id; }
    public String getId() { return id; }
    public String getDisplayName() { return id; }
}
