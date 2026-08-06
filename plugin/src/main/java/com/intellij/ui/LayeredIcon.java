package com.intellij.ui;
import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
/** IntelliJ-compat stub — compound layered icon. */
public class LayeredIcon implements Icon {
    private final Icon[] icons;
    public LayeredIcon(int layerCount) { this.icons = new Icon[layerCount]; }
    public LayeredIcon(Icon... icons) { this.icons = icons; }
    public void setIcon(Icon icon, int layer) { if (layer < icons.length) icons[layer] = icon; }
    public void setIcon(Icon icon, int layer, int hShift, int vShift) { setIcon(icon, layer); }
    @Override public void paintIcon(Component c, Graphics g, int x, int y) {
        for (Icon i : icons) if (i != null) i.paintIcon(c, g, x, y);
    }
    @Override public int getIconWidth() { int w = 0; for (Icon i : icons) if (i != null) w = Math.max(w, i.getIconWidth()); return w; }
    @Override public int getIconHeight() { int h = 0; for (Icon i : icons) if (i != null) h = Math.max(h, i.getIconHeight()); return h; }
}
