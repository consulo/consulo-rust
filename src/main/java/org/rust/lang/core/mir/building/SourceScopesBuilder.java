/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.MirSourceScope;
import org.rust.lang.core.mir.schemas.MirSpan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SourceScopesBuilder {
    @NotNull
    private MirSourceScope sourceScope;
    @NotNull
    private final List<MirSourceScope> stack;

    public SourceScopesBuilder(@NotNull MirSpan span) {
        this.sourceScope = new MirSourceScope(0, span, null);
        this.stack = new ArrayList<>();
        this.stack.add(sourceScope);
    }

    @NotNull
    public MirSourceScope getSourceScope() {
        return sourceScope;
    }

    public void setSourceScope(@NotNull MirSourceScope sourceScope) {
        this.sourceScope = sourceScope;
    }

    @NotNull
    public MirSourceScope getOutermost() {
        return stack.get(0);
    }

    @NotNull
    public MirSourceScope newSourceScope(@NotNull MirSpan span) {
        MirSourceScope scope = new MirSourceScope(stack.size(), span, sourceScope);
        stack.add(scope);
        return scope;
    }

    @NotNull
    public List<MirSourceScope> build() {
        return Collections.unmodifiableList(stack);
    }
}
