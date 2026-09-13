package com.intellij.openapi.components;
import consulo.component.persist.PersistentStateComponent;
public abstract class SimplePersistentStateComponent<T extends BaseState> implements PersistentStateComponent<T> {
    private T state;
    protected SimplePersistentStateComponent(T defaultState) { this.state = defaultState; }
    @Override public T getState() { return state; }
    @Override public void loadState(T state) { this.state = state; }
}
