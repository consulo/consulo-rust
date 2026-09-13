/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.intellij.openapi.ui;

import javax.swing.JPanel;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Form panel that owns the bindings between its components and the settings behind them.
 * Callers register a binding per editable value; {@link #reset()} pushes stored values into
 * the components, {@link #isModified()} reports whether any component differs from the stored
 * value, and {@link #apply()} writes the component values back.
 */
public class DialogPanel extends JPanel {
    private final List<Runnable> applyCallbacks = new ArrayList<>();
    private final List<Runnable> resetCallbacks = new ArrayList<>();
    private final List<BooleanSupplier> modifiedCallbacks = new ArrayList<>();

    public DialogPanel() {
        super();
    }

    public DialogPanel(LayoutManager layout) {
        super(layout);
    }

    /**
     * Registers one editable value: {@code read} loads it into the component, {@code write}
     * stores what the component currently holds, and {@code modified} compares the two.
     */
    public void bind(Runnable read, Runnable write, BooleanSupplier modified) {
        resetCallbacks.add(read);
        applyCallbacks.add(write);
        modifiedCallbacks.add(modified);
    }

    public void onApply(Runnable callback) {
        applyCallbacks.add(callback);
    }

    public void onReset(Runnable callback) {
        resetCallbacks.add(callback);
    }

    public void onIsModified(BooleanSupplier callback) {
        modifiedCallbacks.add(callback);
    }

    public void apply() {
        for (Runnable callback : applyCallbacks) {
            callback.run();
        }
    }

    public void reset() {
        for (Runnable callback : resetCallbacks) {
            callback.run();
        }
    }

    public boolean isModified() {
        for (BooleanSupplier callback : modifiedCallbacks) {
            if (callback.getAsBoolean()) return true;
        }
        return false;
    }
}
