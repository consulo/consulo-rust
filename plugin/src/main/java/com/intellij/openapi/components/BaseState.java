package com.intellij.openapi.components;
public class BaseState {
    public void resetModificationCount() {}
    public boolean isModified() { return false; }
    public void incrementModificationCount() {}
}
