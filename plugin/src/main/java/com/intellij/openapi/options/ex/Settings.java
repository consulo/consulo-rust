package com.intellij.openapi.options.ex;
import consulo.configurable.Configurable;
import consulo.util.dataholder.Key;
/** IntelliJ-compat stub — IJ's Settings data-key for configurable editor. */
public interface Settings {
    Key<Settings> KEY = Key.create("settings.key");
    Configurable find(String id);
    Configurable find(Class<? extends Configurable> clazz);
    void select(Configurable configurable);
}
