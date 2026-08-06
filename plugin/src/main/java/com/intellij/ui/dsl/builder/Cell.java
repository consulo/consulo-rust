/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.intellij.ui.dsl.builder;

import jakarta.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** IntelliJ Kotlin DSL {@code Cell<T>} stub. */
public class Cell<T> {
    @Nonnull private final T component;

    public Cell(@Nonnull T component) { this.component = component; }

    @Nonnull public T getComponent() { return component; }

    public Cell<T> align(@Nonnull Object align) { return this; }
    public Cell<T> horizontalAlign(@Nonnull Object align) { return this; }
    public Cell<T> verticalAlign(@Nonnull Object align) { return this; }
    public Cell<T> gap(@Nonnull Object gap) { return this; }
    public Cell<T> comment(@Nonnull String text) { return this; }
    public Cell<T> label(@Nonnull String text) { return this; }
    public Cell<T> applyToComponent(@Nonnull Consumer<T> block) { block.accept(component); return this; }
    public Cell<T> bindText(@Nonnull Supplier<String> getter, @Nonnull Consumer<String> setter) { return this; }
    public Cell<T> bindSelected(@Nonnull Supplier<Boolean> getter, @Nonnull Consumer<Boolean> setter) { return this; }
    public Cell<T> bindIntText(@Nonnull Supplier<Integer> getter, @Nonnull Consumer<Integer> setter) { return this; }
    public Cell<T> enabled(boolean enabled) { return this; }
    public Cell<T> visible(boolean visible) { return this; }
    public Cell<T> visibleIf(Object predicate) { return this; }
    public Cell<T> enabledIf(Object predicate) { return this; }
    public Cell<T> resizableColumn() { return this; }
    public Cell<T> focused() { return this; }
    public Cell<T> columns(int columns) { return this; }
    public Cell<T> onChanged(@Nonnull Consumer<T> listener) { return this; }
}
