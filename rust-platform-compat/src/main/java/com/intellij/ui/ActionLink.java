package com.intellij.ui;

import java.awt.event.ActionListener;

/** Clickable text link; alias of {@link com.intellij.ui.components.ActionLink}. */
public class ActionLink extends com.intellij.ui.components.ActionLink {
    public ActionLink() { super(); }
    public ActionLink(String text) { super(text); }
    public ActionLink(String text, ActionListener listener) { super(text, listener); }
}
