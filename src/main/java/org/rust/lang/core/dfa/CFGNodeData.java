/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.utils.PresentableNodeData;

public abstract class CFGNodeData implements PresentableNodeData {
    @Nullable
    private final RsElement element;

    private CFGNodeData(@Nullable RsElement element) {
        this.element = element;
    }

    @Nullable
    public RsElement getElement() {
        return element;
    }

    /** Any execution unit (e.g. expression, statement) */
    public static class AST extends CFGNodeData {
        public AST(@NotNull RsElement element) {
            super(element);
        }
    }

    /** Execution start */
    public static class Entry extends CFGNodeData {
        public static final Entry INSTANCE = new Entry();

        private Entry() {
            super(null);
        }
    }

    /** Normal execution end (e.g. after main function block) */
    public static class Exit extends CFGNodeData {
        public static final Exit INSTANCE = new Exit();

        private Exit() {
            super(null);
        }
    }

    /** Any real (e.g. panic!() call) or imaginary (e.g. after infinite loop) execution end */
    public static class Termination extends CFGNodeData {
        public static final Termination INSTANCE = new Termination();

        private Termination() {
            super(null);
        }
    }

    /** Supplementary node to build complex control flow (e.g. loops and pattern matching) */
    public static class Dummy extends CFGNodeData {
        public static final Dummy INSTANCE = new Dummy();

        private Dummy() {
            super(null);
        }
    }

    /** Start of any unreachable code (e.g. after return) */
    public static class Unreachable extends CFGNodeData {
        public static final Unreachable INSTANCE = new Unreachable();

        private Unreachable() {
            super(null);
        }
    }

    @NotNull
    @Override
    public String getText() {
        if (this instanceof AST) {
            RsElement el = getElement();
            assert el != null;
            return cfgText(el).trim();
        }
        if (this instanceof Entry) return "Entry";
        if (this instanceof Exit) return "Exit";
        if (this instanceof Termination) return "Termination";
        if (this instanceof Dummy) return "Dummy";
        if (this instanceof Unreachable) return "Unreachable";
        return "";
    }

    @NotNull
    private static String cfgText(@NotNull RsElement element) {
        if (element instanceof RsBlock || element instanceof RsBlockExpr) return "BLOCK";
        if (element instanceof RsIfExpr) return "IF";
        if (element instanceof RsWhileExpr) return "WHILE";
        if (element instanceof RsLoopExpr) return "LOOP";
        if (element instanceof RsForExpr) return "FOR";
        if (element instanceof RsMatchExpr) return "MATCH";
        if (element instanceof RsLambdaExpr) return "CLOSURE";
        if (element instanceof RsExprStmt) {
            RsExprStmt exprStmt = (RsExprStmt) element;
            StringBuilder sb = new StringBuilder();
            for (RsOuterAttr attr : exprStmt.getOuterAttrList()) {
                sb.append(attr.getText());
            }
            if (sb.length() > 0) sb.append(" ");
            sb.append(cfgText(exprStmt.getExpr()));
            sb.append(";");
            return sb.toString();
        }
        return element.getText();
    }
}
