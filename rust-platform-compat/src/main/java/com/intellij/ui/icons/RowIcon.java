package com.intellij.ui.icons;
import javax.swing.Icon;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
/** Compound icon that lays its child icons out in a horizontal row. */
public class RowIcon implements Icon {
    private final Icon[] icons;
    public RowIcon(Icon... icons) { this.icons = icons; }
    public int getIconCount() { return icons.length; }
    public Icon getIcon(int i) { return icons[i]; }
    public List<Icon> getAllIcons() { return Arrays.asList(icons); }
    @Override public void paintIcon(Component c, Graphics g, int x, int y) {}
    @Override public int getIconWidth() { return 16; }
    @Override public int getIconHeight() { return 16; }
}
