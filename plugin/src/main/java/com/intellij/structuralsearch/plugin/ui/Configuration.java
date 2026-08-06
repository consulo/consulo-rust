package com.intellij.structuralsearch.plugin.ui;
/** IntelliJ-compat stub — SSR configuration. */
public abstract class Configuration {
    private String name;
    private String category;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public abstract Configuration copy();
}
