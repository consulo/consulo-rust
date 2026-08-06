/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.MirSourceScope;
import org.rust.lang.core.mir.schemas.MirSpan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SourceScopesBuilder {
    @Nonnull
    private MirSourceScope sourceScope;
    @Nonnull
    private final List<MirSourceScope> stack;

    public SourceScopesBuilder(@Nonnull MirSpan span) {
        this.sourceScope = new MirSourceScope(0, span, null);
        this.stack = new ArrayList<>();
        this.stack.add(sourceScope);
    }

    @Nonnull
    public MirSourceScope getSourceScope() {
        return sourceScope;
    }

    public void setSourceScope(@Nonnull MirSourceScope sourceScope) {
        this.sourceScope = sourceScope;
    }

    @Nonnull
    public MirSourceScope getOutermost() {
        return stack.get(0);
    }

    @Nonnull
    public MirSourceScope newSourceScope(@Nonnull MirSpan span) {
        MirSourceScope scope = new MirSourceScope(stack.size(), span, sourceScope);
        stack.add(scope);
        return scope;
    }

    @Nonnull
    public List<MirSourceScope> build() {
        return Collections.unmodifiableList(stack);
    }
}
