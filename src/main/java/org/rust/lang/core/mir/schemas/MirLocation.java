/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public final class MirLocation {
    @NotNull
    private final MirBasicBlock block;
    private final int statementIndex;

    public MirLocation(@NotNull MirBasicBlock block, int statementIndex) {
        this.block = block;
        this.statementIndex = statementIndex;
    }

    @NotNull
    public MirBasicBlock getBlock() {
        return block;
    }

    public int getStatementIndex() {
        return statementIndex;
    }

    @NotNull
    public MirSourceInfo getSource() {
        if (statementIndex == block.getStatements().size()) {
            return block.getTerminator().getSource();
        } else {
            return block.getStatements().get(statementIndex).getSource();
        }
    }

    /** null if corresponds to terminator */
    @Nullable
    public MirStatement getStatement() {
        List<MirStatement> statements = block.getStatements();
        if (statementIndex >= 0 && statementIndex < statements.size()) {
            return statements.get(statementIndex);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MirLocation that = (MirLocation) o;
        return statementIndex == that.statementIndex && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(block, statementIndex);
    }

    @Override
    public String toString() {
        return "MirLocation(block=" + block + ", statementIndex=" + statementIndex + ")";
    }
}
