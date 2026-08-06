package com.intellij.openapi.module;
import consulo.ui.image.Image;
/** IntelliJ-compat stub — Consulo's module types are via consulo.module.extension. */
public abstract class ModuleType<T> {
    private final String id;
    protected ModuleType(String id) { this.id = id; }
    public String getId() { return id; }
    public String getName() { return id; }
    public String getDescription() { return ""; }
    public Image getIcon() { return Image.empty(16); }
}
