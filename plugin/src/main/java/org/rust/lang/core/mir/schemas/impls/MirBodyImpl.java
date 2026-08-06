/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas.impls;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.*;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.List;
import java.util.Objects;

public final class MirBodyImpl implements MirBody {
    @Nonnull
    private final RsElement sourceElement;
    @Nonnull
    private final List<MirBasicBlockImpl> basicBlocks;
    @Nonnull
    private final List<MirLocal> localDecls;
    @Nonnull
    private final MirSpan span;
    @Nonnull
    private final List<MirSourceScope> sourceScopes;
    private final int argCount;
    @Nonnull
    private final List<MirVarDebugInfo> varDebugInfo;

    public MirBodyImpl(
        @Nonnull RsElement sourceElement,
        @Nonnull List<MirBasicBlockImpl> basicBlocks,
        @Nonnull List<MirLocal> localDecls,
        @Nonnull MirSpan span,
        @Nonnull List<MirSourceScope> sourceScopes,
        int argCount,
        @Nonnull List<MirVarDebugInfo> varDebugInfo
    ) {
        this.sourceElement = sourceElement;
        this.basicBlocks = basicBlocks;
        this.localDecls = localDecls;
        this.span = span;
        this.sourceScopes = sourceScopes;
        this.argCount = argCount;
        this.varDebugInfo = varDebugInfo;
    }

    @Override
    @Nonnull
    public RsElement getSourceElement() {
        return sourceElement;
    }

    @Override
    @Nonnull
    public List<MirBasicBlockImpl> getBasicBlocks() {
        return basicBlocks;
    }

    @Override
    @Nonnull
    public List<MirLocal> getLocalDecls() {
        return localDecls;
    }

    @Override
    @Nonnull
    public MirSpan getSpan() {
        return span;
    }

    @Override
    @Nonnull
    public List<MirSourceScope> getSourceScopes() {
        return sourceScopes;
    }

    @Override
    public int getArgCount() {
        return argCount;
    }

    @Override
    @Nonnull
    public List<MirVarDebugInfo> getVarDebugInfo() {
        return varDebugInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MirBodyImpl mirBody = (MirBodyImpl) o;
        return argCount == mirBody.argCount
            && Objects.equals(sourceElement, mirBody.sourceElement)
            && Objects.equals(basicBlocks, mirBody.basicBlocks)
            && Objects.equals(localDecls, mirBody.localDecls)
            && Objects.equals(span, mirBody.span)
            && Objects.equals(sourceScopes, mirBody.sourceScopes)
            && Objects.equals(varDebugInfo, mirBody.varDebugInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceElement, basicBlocks, localDecls, span, sourceScopes, argCount, varDebugInfo);
    }

    @Override
    public String toString() {
        return "MirBodyImpl(sourceElement=" + sourceElement + ", argCount=" + argCount + ")";
    }
}
