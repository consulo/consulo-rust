package com.intellij.openapi.module;
public abstract class ModuleTypeManager {
    public static ModuleTypeManager getInstance() { return null; }
    public abstract void registerModuleType(ModuleType<?> type);
    public abstract ModuleType<?> findByID(String id);
}
