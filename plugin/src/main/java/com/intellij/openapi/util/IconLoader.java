package com.intellij.openapi.util;
import javax.swing.Icon;
import javax.swing.ImageIcon;
/** IntelliJ-compat stub — Consulo uses Image.empty / ImageEffects. */
public final class IconLoader {
    private IconLoader() {}
    private static final Icon EMPTY = new ImageIcon(new byte[]{});
    public static Icon getIcon(String path, Class<?> contextClass) { return EMPTY; }
    public static Icon getIcon(String path) { return EMPTY; }
}
