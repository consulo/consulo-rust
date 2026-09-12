package com.intellij.openapi.options.ex;
import consulo.configurable.Configurable;
import consulo.util.dataholder.Key;
/** Settings editor handle: finds configurables by id or class and selects them. */
public interface Settings {
    Key<Settings> KEY = Key.create("settings.key");
    Configurable find(String id);
    Configurable find(Class<? extends Configurable> clazz);
    void select(Configurable configurable);
}
