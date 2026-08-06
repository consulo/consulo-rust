package com.intellij.ui;
import javax.swing.JLabel;
import java.awt.event.ActionListener;
public class ActionLink extends JLabel {
    public ActionLink() {}
    public ActionLink(String text) { super(text); }
    public ActionLink(String text, ActionListener listener) { super(text); }
    public void addActionListener(ActionListener listener) {}
}
