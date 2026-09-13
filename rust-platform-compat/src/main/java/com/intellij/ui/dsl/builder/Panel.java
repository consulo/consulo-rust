/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.intellij.ui.dsl.builder;

import com.intellij.openapi.ui.DialogPanel;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Builder for a {@link DialogPanel}. Every layout call is a no-op and {@link #build()}
 * returns an empty panel.
 */
public class Panel {
    public DialogPanel build() { return new DialogPanel(); }
    public Panel row(String label, Consumer<Row> block) { return this; }
    public Panel row(String label, Function<Row, ?> block) { return this; }
    public Panel row(Consumer<Row> block) { return this; }
    public Panel row(Function<Row, ?> block) { return this; }
    public Panel group(String title, Consumer<Panel> block) { return this; }
    public Panel group(String title, Function<Panel, ?> block) { return this; }
    public Panel groupRowsRange(String title, Consumer<Panel> block) { return this; }
    public Panel groupRowsRange(String title, Function<Panel, ?> block) { return this; }
    public Panel groupRowsRange(String title, boolean indent, Object topGap, Object bottomGap, Consumer<Panel> block) { return this; }
    public Panel groupRowsRange(String title, boolean indent, Object topGap, Object bottomGap, Function<Panel, ?> block) { return this; }
    public Panel panel(Consumer<Panel> block) { return this; }
    public Panel panel(Function<Panel, ?> block) { return this; }
    public Panel separator() { return this; }
    public Panel indent(Consumer<Panel> block) { return this; }
    public Panel indent(Function<Panel, ?> block) { return this; }
    public Panel collapsibleGroup(String title, Consumer<Panel> block) { return this; }
    public Panel collapsibleGroup(String title, Function<Panel, ?> block) { return this; }
    public Panel visibleIf(Object predicate) { return this; }
    public Panel enabledIf(Object predicate) { return this; }
}
