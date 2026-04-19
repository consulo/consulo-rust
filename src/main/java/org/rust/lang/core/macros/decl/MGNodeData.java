/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.decl;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.utils.PresentableNodeData;

/**
 * Data associated with nodes in a macro graph.
 */
public abstract class MGNodeData implements PresentableNodeData {

    private MGNodeData() {}

    public static final class Literal extends MGNodeData {
        @NotNull
        private final ASTNode myValue;

        public Literal(@NotNull ASTNode value) {
            myValue = value;
        }

        @NotNull
        public ASTNode getValue() {
            return myValue;
        }

        @NotNull
        @Override
        public String getText() {
            return myValue.getText();
        }
    }

    public static final class Fragment extends MGNodeData {
        @NotNull
        private final FragmentKind myKind;

        public Fragment(@NotNull FragmentKind kind) {
            myKind = kind;
        }

        @NotNull
        public FragmentKind getKind() {
            return myKind;
        }

        @NotNull
        @Override
        public String getText() {
            return myKind.toString();
        }
    }

    public static final class Start extends MGNodeData {
        public static final Start INSTANCE = new Start();

        private Start() {}

        @NotNull
        @Override
        public String getText() {
            return "START";
        }
    }

    public static final class End extends MGNodeData {
        public static final End INSTANCE = new End();

        private End() {}

        @NotNull
        @Override
        public String getText() {
            return "END";
        }
    }

    public static final class BranchStart extends MGNodeData {
        public static final BranchStart INSTANCE = new BranchStart();

        private BranchStart() {}

        @NotNull
        @Override
        public String getText() {
            return "[S]";
        }
    }

    public static final class BranchEnd extends MGNodeData {
        public static final BranchEnd INSTANCE = new BranchEnd();

        private BranchEnd() {}

        @NotNull
        @Override
        public String getText() {
            return "[E]";
        }
    }
}
