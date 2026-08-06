package com.intellij.ui;
import consulo.disposer.Disposable;
import javax.swing.JComponent;
import java.awt.Point;
/** IntelliJ-compat stub — popover tooltip. */
public class GotItTooltip {
    public GotItTooltip(String id, String text, Disposable parentDisposable) {}
    public GotItTooltip withHeader(String header) { return this; }
    public GotItTooltip withIcon(Object icon) { return this; }
    public GotItTooltip withLink(String linkText, Runnable action) { return this; }
    public GotItTooltip withButtonLabel(String label) { return this; }
    public GotItTooltip withMaxCount(int count) { return this; }
    public void show(JComponent component, java.util.function.BiFunction<JComponent, GotItTooltip, Point> pointProvider) {}

    public static final java.util.function.BiFunction<JComponent, GotItTooltip, Point> TOP_MIDDLE =
        (c, t) -> new Point(c.getWidth() / 2, 0);
    public static final java.util.function.BiFunction<JComponent, GotItTooltip, Point> BOTTOM_MIDDLE =
        (c, t) -> new Point(c.getWidth() / 2, c.getHeight());
}
