/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.decl;

import consulo.language.ast.ASTNode;
import jakarta.annotation.Nonnull;
import org.rust.common.graph.PresentableNodeData;

/**
 * Data associated with nodes in a macro graph.
 */
public abstract class MGNodeData implements PresentableNodeData {

    private MGNodeData() {}

    public static final class Literal extends MGNodeData {
        @Nonnull
        private final ASTNode myValue;

        public Literal(@Nonnull ASTNode value) {
            myValue = value;
        }

        @Nonnull
        public ASTNode getValue() {
            return myValue;
        }

        @Nonnull
        @Override
        public String getText() {
            return myValue.getText();
            }
    }

    public static final class Fragment extends MGNodeData {
        @Nonnull
        private final FragmentKind myKind;

        public Fragment(@Nonnull FragmentKind kind) {
            myKind = kind;
        }

        @Nonnull
        public FragmentKind getKind() {
            return myKind;
        }

        @Nonnull
        @Override
        public String getText() {
            return myKind.toString();
            }
    }

    public static final class Start extends MGNodeData {
        public static final Start INSTANCE = new Start();

        private Start() {}

        @Nonnull
        @Override
        public String getText() {
            return "START";
            }
    }

    public static final class End extends MGNodeData {
        public static final End INSTANCE = new End();

        private End() {}

        @Nonnull
        @Override
        public String getText() {
            return "END";
            }
    }

    public static final class BranchStart extends MGNodeData {
        public static final BranchStart INSTANCE = new BranchStart();

        private BranchStart() {}

        @Nonnull
        @Override
        public String getText() {
            return "[S]";
            }
    }

    public static final class BranchEnd extends MGNodeData {
        public static final BranchEnd INSTANCE = new BranchEnd();

        private BranchEnd() {}

        @Nonnull
        @Override
        public String getText() {
            return "[E]";
            }
    }
}
