/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa;

import consulo.language.controlFlow.ControlFlowBuilder;
import consulo.language.controlFlow.base.InstructionImpl;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsElement;

/**
 * A node of the {@link ControlFlowGraph}: an instruction of the platform control flow whose kind is its {@link CFGNodeData}.
 */
public final class CFGInstruction extends InstructionImpl {
    @Nonnull
    private final CFGNodeData data;

    public CFGInstruction(@Nonnull ControlFlowBuilder builder, @Nonnull CFGNodeData data) {
        super(builder, data.getElement());
        this.data = data;
    }

    @Nonnull
    public CFGNodeData getData() {
        return data;
    }

    @Nullable
    @Override
    public RsElement getElement() {
        return data.getElement();
    }

    @Override
    public String getElementPresentation() {
        return data.getText();
    }
}
