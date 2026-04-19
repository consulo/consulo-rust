/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsDotExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsIndexExpr;
import org.rust.lang.core.psi.RsUnaryExpr;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsIndexExprExtUtil;
import org.rust.lang.core.psi.ext.RsUnaryExprUtil;
import org.rust.lang.core.psi.ext.UnaryOperator;
import org.rust.lang.core.types.regions.*;

import java.util.*;

/**
 * RvalueScopes is a mapping from sub-expressions to extended lifetime as determined by rules laid out in
 * `rustc_hir_analysis::check::rvalue_scopes`.
 */
public class RvalueScopes {
    private final Map<RsElement, Scope> myMap = new HashMap<>();

    private static final Set<UnaryOperator> REF_AND_DEREF_OPS = new HashSet<>(Arrays.asList(
        UnaryOperator.REF, UnaryOperator.REF_MUT, UnaryOperator.DEREF
    ));

    /** Returns the scope when the temp created by expr will be cleaned up. */
    @Nullable
    public Scope temporaryScope(@NotNull ScopeTree regionScopeTree, @NotNull RsExpr expr) {
        Scope scope = myMap.get(expr);
        if (scope != null) return scope;

        Scope id = new Scope.Node(expr);
        while (true) {
            Scope enclosing = regionScopeTree.getEnclosingScope(id);
            if (enclosing == null) return null;
            if (enclosing instanceof Scope.Destruction) return enclosing;
            id = enclosing;
        }
    }

    /** Make an association between a sub-expression and an extended lifetime. */
    public void recordRvalueScope(@NotNull RsElement element, @Nullable Scope lifetime) {
        if (lifetime != null) {
            if (element == lifetime.getElement()) {
                throw new IllegalStateException("Element cannot equal lifetime element");
            }
        }
        myMap.put(element, lifetime);
    }

    @NotNull
    public static RvalueScopes resolveRvalueScopes(@NotNull ScopeTree scopeTree) {
        RvalueScopes rvalueScopes = new RvalueScopes();
        for (Map.Entry<RsExpr, RvalueCandidateType> entry : scopeTree.getRvalueCandidates().entrySet()) {
            recordRvalueScopeRec(rvalueScopes, entry.getKey(), entry.getValue().getLifetime());
        }
        return rvalueScopes;
    }

    private static void recordRvalueScopeRec(@NotNull RvalueScopes rvalueScopes, @NotNull RsExpr expr, @Nullable Scope lifetime) {
        RsExpr exprVar = expr;
        while (true) {
            rvalueScopes.recordRvalueScope(exprVar, lifetime);
            if (exprVar instanceof RsIndexExpr) {
                exprVar = RsIndexExprExtUtil.getContainerExpr((RsIndexExpr) exprVar);
            } else if (exprVar instanceof RsUnaryExpr) {
                UnaryOperator op = RsUnaryExprUtil.getOperatorType((RsUnaryExpr) exprVar);
                if (REF_AND_DEREF_OPS.contains(op)) {
                    RsExpr inner = ((RsUnaryExpr) exprVar).getExpr();
                    if (inner == null) return;
                    exprVar = inner;
                } else {
                    return;
                }
            } else if (exprVar instanceof RsDotExpr && ((RsDotExpr) exprVar).getFieldLookup() != null) {
                exprVar = ((RsDotExpr) exprVar).getExpr();
            } else {
                return;
            }
        }
    }
}
