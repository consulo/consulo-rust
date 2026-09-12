package com.intellij.ui.components;

import consulo.ui.ex.awt.LinkLabel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/** Clickable text link that notifies its action listeners when activated. */
public class ActionLink extends LinkLabel<Object> {
    private final List<ActionListener> listeners = new ArrayList<>();

    public ActionLink() {
        this("");
    }

    public ActionLink(String text) {
        super(text, null);
        setListener((source, data) -> fireActionPerformed(), null);
    }

    public ActionLink(String text, ActionListener listener) {
        this(text);
        addActionListener(listener);
    }

    public void addActionListener(ActionListener listener) {
        listeners.add(listener);
    }

    public void removeActionListener(ActionListener listener) {
        listeners.remove(listener);
    }

    private void fireActionPerformed() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getText());
        for (ActionListener listener : new ArrayList<>(listeners)) {
            listener.actionPerformed(event);
        }
    }
}
