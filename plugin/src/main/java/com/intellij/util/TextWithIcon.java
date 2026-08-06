package com.intellij.util;
import javax.swing.Icon;
import consulo.ui.image.Image;
/** IntelliJ-compat stub — text + optional icon pair. */
public final class TextWithIcon {
    private final String text;
    private final Image icon;
    public TextWithIcon(String text, Image icon) {
        this.text = text;
        this.icon = icon;
    }
    public String getText() { return text; }
    public Image getIcon() { return icon; }
}
