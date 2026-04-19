/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas.impls;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.*;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.List;
import java.util.Objects;

public final class MirBodyImpl implements MirBody {
    @NotNull
    private final RsElement sourceElement;
    @NotNull
    private final List<MirBasicBlockImpl> basicBlocks;
    @NotNull
    private final List<MirLocal> localDecls;
    @NotNull
    private final MirSpan span;
    @NotNull
    private final List<MirSourceScope> sourceScopes;
    private final int argCount;
    @NotNull
    private final List<MirVarDebugInfo> varDebugInfo;

    public MirBodyImpl(
        @NotNull RsElement sourceElement,
        @NotNull List<MirBasicBlockImpl> basicBlocks,
        @NotNull List<MirLocal> localDecls,
        @NotNull MirSpan span,
        @NotNull List<MirSourceScope> sourceScopes,
        int argCount,
        @NotNull List<MirVarDebugInfo> varDebugInfo
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
    @NotNull
    public RsElement getSourceElement() {
        return sourceElement;
    }

    @Override
    @NotNull
    public List<MirBasicBlockImpl> getBasicBlocks() {
        return basicBlocks;
    }

    @Override
    @NotNull
    public List<MirLocal> getLocalDecls() {
        return localDecls;
    }

    @Override
    @NotNull
    public MirSpan getSpan() {
        return span;
    }

    @Override
    @NotNull
    public List<MirSourceScope> getSourceScopes() {
        return sourceScopes;
    }

    @Override
    public int getArgCount() {
        return argCount;
    }

    @Override
    @NotNull
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
