package com.intellij.execution.target;
public interface TargetEnvironmentAwareRunProfile {
    default boolean canRunOn(TargetEnvironmentConfiguration target) { return true; }
    default String getDefaultTargetName() { return null; }
    default void setDefaultTargetName(String targetName) {}
    default LanguageRuntimeType<?> getDefaultLanguageRuntimeType() { return null; }
}
