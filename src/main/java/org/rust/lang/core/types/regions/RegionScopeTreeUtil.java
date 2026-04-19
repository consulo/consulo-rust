/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.regions;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsBlockUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsInferenceContextOwner;
import org.rust.lang.core.psi.ext.RsInferenceContextOwnerUtil;

/** Utility methods for building and querying {@link ScopeTree}s per inference context. */
public final class RegionScopeTreeUtil {
    private RegionScopeTreeUtil() {
    }

    /**
     * Build a region {@link ScopeTree} for the body of {@code owner}. The tree records:
     * <ul>
     *     <li>One {@link Scope.Node} per {@link RsBlock}, {@link RsStmt}, {@link RsExpr},
     *         {@link RsMatchArm}, {@link RsValueParameter}, or {@link RsLetExpr} encountered.</li>
     *     <li>Each block's statements as {@link Scope.Remainder} sub-scopes covering the
     *         suffix of the block after a let-decl — the standard Rust scoping rule.</li>
     *     <li>Parent/child relationships between scopes.</li>
     *     <li>Variable → enclosing scope bindings for every {@link RsPatBinding} and
     *         {@link RsSelfParameter} visited.</li>
     * </ul>
     */
    @NotNull
    public static ScopeTree getRegionScopeTree(@NotNull RsInferenceContextOwner owner) {
        ScopeTree tree = new ScopeTree();
        RsElement body = RsInferenceContextOwnerUtil.getBody(owner);
        tree.setRootBody(body);
        if (body == null) return tree;
        Visitor visitor = new Visitor(tree);
        // Prime ownership: feed the function/constant signature parts for variable scoping.
        if (owner instanceof RsFunction) {
            RsFunction fn = (RsFunction) owner;
            RsValueParameterList plist = fn.getValueParameterList();
            if (plist != null) {
                Scope fnScope = new Scope.Node(fn);
                visitor.recordScope(fnScope, null);
                RsSelfParameter self = fn.getSelfParameter();
                if (self != null) tree.recordVarScope(self, fnScope);
                for (RsValueParameter p : plist.getValueParameterList()) {
                    if (p.getPat() != null) visitor.bindPatterns(p.getPat(), fnScope);
                }
                visitor.pushCurrent(fnScope);
                RsBlock fnBody = org.rust.lang.core.psi.ext.RsFunctionUtil.getBlock(fn);
                if (fnBody != null) visitor.visitBlock(fnBody);
                visitor.popCurrent();
                return tree;
            }
        }
        visitor.visitAny(body);
        return tree;
    }

    private static final class Visitor {
        @NotNull private final ScopeTree tree;
        private Scope current;
        private int depth;

        Visitor(@NotNull ScopeTree tree) {
            this.tree = tree;
        }

        void pushCurrent(@NotNull Scope scope) {
            current = scope;
            depth++;
        }

        void popCurrent() {
            depth--;
        }

        void recordScope(@NotNull Scope scope, @Nullable Scope parent) {
            ScopeInfo parentInfo = parent == null ? null : new ScopeInfo(parent, depth);
            tree.recordScopeParent(scope, parentInfo);
        }

        void visitAny(@NotNull RsElement element) {
            if (element instanceof RsBlock) {
                visitBlock((RsBlock) element);
            } else if (element instanceof RsExpr) {
                visitExpr((RsExpr) element);
            } else {
                Scope node = new Scope.Node(element);
                recordScope(node, current);
                pushCurrent(node);
                for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (child instanceof RsElement) visitAny((RsElement) child);
                }
                popCurrent();
            }
        }

        void visitBlock(@NotNull RsBlock block) {
            Scope parent = current;
            Scope blockScope = new Scope.Node(block);
            recordScope(blockScope, parent);
            pushCurrent(blockScope);

            RsBlockUtil.ExpandedStmtsAndTailExpr pair = RsBlockUtil.getExpandedStmtsAndTailExpr(block);
            for (RsStmt stmt : pair.getStmts()) {
                if (stmt instanceof RsLetDecl) {
                    // Remainder(block, letDecl) is the sub-scope covering the suffix of `block`
                    // after this let declaration.
                    Scope remainder = new Scope.Remainder(block, (RsLetDecl) stmt);
                    recordScope(remainder, blockScope);
                    pushCurrent(remainder);
                    visitStmt(stmt);
                    popCurrent();
                } else {
                    visitStmt(stmt);
                }
            }
            RsExpr tail = pair.getTailExpr();
            if (tail != null) visitExpr(tail);

            popCurrent();
        }

        void visitStmt(@NotNull RsStmt stmt) {
            Scope stmtScope = new Scope.Node(stmt);
            recordScope(stmtScope, current);
            pushCurrent(stmtScope);
            if (stmt instanceof RsLetDecl) {
                RsLetDecl let = (RsLetDecl) stmt;
                if (let.getPat() != null) bindPatterns(let.getPat(), current);
                if (let.getExpr() != null) visitExpr(let.getExpr());
            } else if (stmt instanceof RsExprStmt) {
                RsExpr e = ((RsExprStmt) stmt).getExpr();
                if (e != null) visitExpr(e);
            }
            popCurrent();
        }

        void visitExpr(@NotNull RsExpr expr) {
            Scope exprScope = new Scope.Node(expr);
            recordScope(exprScope, current);
            pushCurrent(exprScope);
            if (expr instanceof RsBlockExpr) {
                RsBlock b = ((RsBlockExpr) expr).getBlock();
                if (b != null) visitBlock(b);
            } else if (expr instanceof RsMatchExpr) {
                RsMatchExpr match = (RsMatchExpr) expr;
                if (match.getExpr() != null) visitExpr(match.getExpr());
                RsMatchBody body = match.getMatchBody();
                if (body != null) {
                    for (RsMatchArm arm : body.getMatchArmList()) {
                        Scope armScope = new Scope.Node(arm);
                        recordScope(armScope, current);
                        pushCurrent(armScope);
                        if (arm.getPat() != null) bindPatterns(arm.getPat(), armScope);
                        if (arm.getExpr() != null) visitExpr(arm.getExpr());
                        popCurrent();
                    }
                }
            } else if (expr instanceof RsIfExpr) {
                RsIfExpr ifExpr = (RsIfExpr) expr;
                if (ifExpr.getCondition() != null) {
                    RsExpr cond = ifExpr.getCondition().getExpr();
                    if (cond != null) visitExpr(cond);
                }
                if (ifExpr.getBlock() != null) visitBlock(ifExpr.getBlock());
                RsElseBranch elseBranch = ifExpr.getElseBranch();
                if (elseBranch != null) {
                    if (elseBranch.getBlock() != null) visitBlock(elseBranch.getBlock());
                    if (elseBranch.getIfExpr() != null) visitExpr(elseBranch.getIfExpr());
                }
            } else if (expr instanceof RsForExpr) {
                RsForExpr forExpr = (RsForExpr) expr;
                if (forExpr.getPat() != null) bindPatterns(forExpr.getPat(), current);
                if (forExpr.getExpr() != null) visitExpr(forExpr.getExpr());
                if (forExpr.getBlock() != null) visitBlock(forExpr.getBlock());
            } else if (expr instanceof RsLambdaExpr) {
                RsLambdaExpr lambda = (RsLambdaExpr) expr;
                RsValueParameterList plist = lambda.getValueParameterList();
                if (plist != null) {
                    for (RsValueParameter p : plist.getValueParameterList()) {
                        if (p.getPat() != null) bindPatterns(p.getPat(), exprScope);
                    }
                }
                if (lambda.getExpr() != null) visitExpr(lambda.getExpr());
            } else {
                for (PsiElement child = expr.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (child instanceof RsExpr) visitExpr((RsExpr) child);
                    else if (child instanceof RsBlock) visitBlock((RsBlock) child);
                }
            }
            popCurrent();
        }

        void bindPatterns(@NotNull PsiElement pat, @NotNull Scope scope) {
            if (pat instanceof RsPatBinding) {
                tree.recordVarScope((RsPatBinding) pat, scope);
                return;
            }
            for (PsiElement child = pat.getFirstChild(); child != null; child = child.getNextSibling()) {
                bindPatterns(child, scope);
            }
        }
    }
}
