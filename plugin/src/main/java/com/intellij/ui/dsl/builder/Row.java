/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.intellij.ui.dsl.builder;

import javax.swing.JComponent;
import java.util.function.Consumer;

/** IntelliJ Kotlin DSL {@code Row} stub. */
public class Row {
    public <T extends JComponent> Cell<T> cell(T component) { return new Cell<>(component); }
    public Cell<javax.swing.JLabel> label(String text) { return new Cell<>(new javax.swing.JLabel(text)); }
    public Cell<javax.swing.JCheckBox> checkBox(String text) { return new Cell<>(new javax.swing.JCheckBox(text)); }
    public Row layout(Object layout) { return this; }
    public Row rowComment(String text) { return this; }
    public Row visible(boolean visible) { return this; }
    public Row enabled(boolean enabled) { return this; }
    public Row visibleIf(Object predicate) { return this; }
    public Row enabledIf(Object predicate) { return this; }
    public Row topGap(Object gap) { return this; }
    public Row bottomGap(Object gap) { return this; }
    public Row resizableRow() { return this; }
}
