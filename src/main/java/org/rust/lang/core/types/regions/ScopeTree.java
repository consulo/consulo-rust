/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.regions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.*;

/** The region scope tree encodes information about region relationships. */
public class ScopeTree {
    /** If not empty, this body is the root of this region hierarchy. */
    @Nullable
    private RsElement myRootBody;

    private final Map<Scope, ScopeInfo> myParentMap = new HashMap<>();
    private final Map<RsElement, Scope> myVarMap = new HashMap<>();
    private final Map<RsElement, Scope> myDestructionScopes = new HashMap<>();
    private final Map<RsExpr, RvalueCandidateType> myRvalueCandidates = new LinkedHashMap<>();

    @Nullable
    public RsElement getRootBody() {
        return myRootBody;
    }

    public void setRootBody(@Nullable RsElement rootBody) {
        myRootBody = rootBody;
    }

    @NotNull
    public Map<RsExpr, RvalueCandidateType> getRvalueCandidates() {
        return myRvalueCandidates;
    }

    public void recordScopeParent(@NotNull Scope childScope, @Nullable ScopeInfo parentInfo) {
        if (parentInfo != null) {
            ScopeInfo prev = myParentMap.put(childScope, parentInfo);
            if (prev != null) {
                throw new IllegalStateException("Scope parent already recorded");
            }
        }

        if (childScope instanceof Scope.Destruction) {
            myDestructionScopes.put(childScope.getElement(), childScope);
        }
    }

    @Nullable
    public Scope getDestructionScope(@NotNull RsElement element) {
        return myDestructionScopes.get(element);
    }

    public void recordVarScope(@NotNull RsPatBinding variable, @NotNull Scope lifetime) {
        if (variable == lifetime.getElement()) {
            throw new IllegalStateException("Variable cannot be its own scope element");
        }
        myVarMap.put(variable, lifetime);
    }

    public void recordVarScope(@NotNull RsSelfParameter variable, @NotNull Scope lifetime) {
        if (variable == lifetime.getElement()) {
            throw new IllegalStateException("Variable cannot be its own scope element");
        }
        myVarMap.put(variable, lifetime);
    }

    public void recordRvalueCandidate(@NotNull RsExpr expr, @NotNull RvalueCandidateType candidateType) {
        Scope lifetime = candidateType.getLifetime();
        if (lifetime != null) {
            if (expr == lifetime.getElement()) {
                throw new IllegalStateException("Expr cannot be its own rvalue scope element");
            }
        }
        myRvalueCandidates.put(expr, candidateType);
    }

    /** Returns the narrowest scope that encloses [scope], if any. */
    @Nullable
    public Scope getEnclosingScope(@NotNull Scope scope) {
        ScopeInfo info = myParentMap.get(scope);
        return info != null ? info.getScope() : null;
    }

    /** Returns the lifetime of the local variable, if any. */
    @Nullable
    public Scope getVariableScope(@NotNull RsPatBinding variable) {
        return myVarMap.get(variable);
    }

    /** Returns the lifetime of the local variable, if any. */
    @Nullable
    public Scope getVariableScope(@NotNull RsSelfParameter variable) {
        return myVarMap.get(variable);
    }

    /**
     * Finds the lowest common ancestor of two scopes.
     * That is, finds the smallest scope which is greater than or equal to both scope1 and scope2.
     */
    @NotNull
    public Scope getLowestCommonAncestor(@NotNull Scope scope1, @NotNull Scope scope2) {
        if (scope1.equals(scope2)) return scope1;

        Scope currentScope1 = scope1;
        Scope currentScope2 = scope2;

        ScopeInfo info1 = myParentMap.get(currentScope1);
        if (info1 == null) return currentScope1;
        ScopeInfo info2 = myParentMap.get(currentScope2);
        if (info2 == null) return currentScope2;

        Scope parentScope1 = info1.getScope();
        int parentDepth1 = info1.getDepth();
        Scope parentScope2 = info2.getScope();
        int parentDepth2 = info2.getDepth();

        if (parentDepth1 > parentDepth2) {
            currentScope1 = parentScope1;
            for (int i = 0; i < parentDepth1 - parentDepth2 - 1; i++) {
                ScopeInfo si = myParentMap.get(currentScope1);
                if (si == null) throw new IllegalStateException("Parent map missing entry");
                currentScope1 = si.getScope();
            }
        } else if (parentDepth2 > parentDepth1) {
            currentScope2 = parentScope2;
            for (int i = 0; i < parentDepth2 - parentDepth1 - 1; i++) {
                ScopeInfo si = myParentMap.get(currentScope2);
                if (si == null) throw new IllegalStateException("Parent map missing entry");
                currentScope2 = si.getScope();
            }
        } else {
            if (parentDepth1 == 0) throw new IllegalStateException("Depth must be non-zero");
            currentScope1 = parentScope1;
            currentScope2 = parentScope2;
        }

        while (!currentScope1.equals(currentScope2)) {
            ScopeInfo si1 = myParentMap.get(currentScope1);
            ScopeInfo si2 = myParentMap.get(currentScope2);
            if (si1 == null || si2 == null) throw new IllegalStateException("Parent map missing entry");
            currentScope1 = si1.getScope();
            currentScope2 = si2.getScope();
        }

        return currentScope1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScopeTree scopeTree = (ScopeTree) o;
        return Objects.equals(myRootBody, scopeTree.myRootBody)
            && Objects.equals(myParentMap, scopeTree.myParentMap)
            && Objects.equals(myVarMap, scopeTree.myVarMap)
            && Objects.equals(myDestructionScopes, scopeTree.myDestructionScopes)
            && Objects.equals(myRvalueCandidates, scopeTree.myRvalueCandidates);
    }

    @Override
    public int hashCode() {
        int result = myRootBody != null ? myRootBody.hashCode() : 0;
        result = 31 * result + myParentMap.hashCode();
        result = 31 * result + myVarMap.hashCode();
        result = 31 * result + myDestructionScopes.hashCode();
        result = 31 * result + myRvalueCandidates.hashCode();
        return result;
    }
}
