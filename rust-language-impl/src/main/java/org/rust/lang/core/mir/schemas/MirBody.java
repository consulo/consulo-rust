/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.*;

public interface MirBody {
    @Nonnull
    RsElement getSourceElement();

    @Nonnull
    List<? extends MirBasicBlock> getBasicBlocks();

    @Nonnull
    List<MirLocal> getLocalDecls();

    @Nonnull
    MirSpan getSpan();

    @Nonnull
    List<MirSourceScope> getSourceScopes();

    int getArgCount();

    @Nonnull
    List<MirVarDebugInfo> getVarDebugInfo();

    @Nonnull
    default MirBasicBlock getStartBlock() {
        return getBasicBlocks().get(0);
    }

    @Nonnull
    default MirSourceScope getOutermostScope() {
        return getSourceScopes().get(0);
    }

    @Nonnull
    default MirLocal getReturnLocal() {
        return getLocalDecls().get(0);
    }

    @Nonnull
    default Map<MirSourceScope, List<MirSourceScope>> getSourceScopesTree() {
        Map<MirSourceScope, List<MirSourceScope>> result = new HashMap<>();
        for (MirSourceScope scope : getSourceScopes()) {
            MirSourceScope parent = scope.getParentScope();
            if (parent != null) {
                result.computeIfAbsent(parent, k -> new ArrayList<>()).add(scope);
            }
        }
        return result;
    }

    @Nonnull
    default List<MirLocal> getArgs() {
        return getLocalDecls().subList(1, getArgCount() + 1);
    }

    @Nonnull
    default MirLocal returnPlace() {
        return getLocalDecls().get(0);
    }

    @Nonnull
    default Set<MirLocal> alwaysStorageLiveLocals() {
        Set<MirLocal> result = new LinkedHashSet<>(getLocalDecls());
        for (MirBasicBlock block : getBasicBlocks()) {
            for (MirStatement statement : block.getStatements()) {
                if (statement instanceof MirStatement.StorageDead) {
                    result.remove(((MirStatement.StorageDead) statement).getLocal());
                } else if (statement instanceof MirStatement.StorageLive) {
                    result.remove(((MirStatement.StorageLive) statement).getLocal());
                }
            }
        }
        return result;
    }

    @Nonnull
    default BasicBlocksPredecessors getBasicBlocksPredecessors() {
        List<? extends MirBasicBlock> blocks = getBasicBlocks();
        List<List<MirBasicBlock>> predecessors = new ArrayList<>(blocks.size());
        for (int i = 0; i < blocks.size(); i++) {
            predecessors.add(new ArrayList<>());
        }
        for (MirBasicBlock block : blocks) {
            for (MirBasicBlock successor : block.getTerminator().getSuccessors()) {
                predecessors.get(successor.getIndex()).add(block);
            }
        }
        return new BasicBlocksPredecessors(predecessors);
    }

    class BasicBlocksPredecessors {
        private final List<List<MirBasicBlock>> predecessors;

        public BasicBlocksPredecessors(@Nonnull List<List<MirBasicBlock>> predecessors) {
            this.predecessors = predecessors;
        }

        @Nonnull
        public List<MirBasicBlock> get(@Nonnull MirBasicBlock block) {
            return predecessors.get(block.getIndex());
        }
    }
}
