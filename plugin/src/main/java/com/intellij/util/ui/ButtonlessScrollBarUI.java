package com.intellij.util.ui;
import javax.swing.plaf.basic.BasicScrollBarUI;
public class ButtonlessScrollBarUI extends BasicScrollBarUI {
    public static ButtonlessScrollBarUI createNormal() { return new ButtonlessScrollBarUI(); }
}
