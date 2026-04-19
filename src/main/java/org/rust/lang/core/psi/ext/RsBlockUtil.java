/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public final class RsBlockUtil {

    private RsBlockUtil() {
    }

    /**
     * Result holder for expanded statements and tail expression.
     */
    public static final class ExpandedStmtsAndTailExpr {
        private final List<RsStmt> myStatements;
        @Nullable
        private final RsExpr myTailExpr;

        public ExpandedStmtsAndTailExpr(@NotNull List<RsStmt> statements, @Nullable RsExpr tailExpr) {
            this.myStatements = statements;
            this.myTailExpr = tailExpr;
        }

        @NotNull
        public List<RsStmt> getStatements() {
            return myStatements;
        }

        @NotNull
        public List<RsStmt> getStmts() {
            return myStatements;
        }

        @Nullable
        public RsExpr getTailExpr() {
            return myTailExpr;
        }

        @NotNull
        public List<RsStmt> getFirst() {
            return myStatements;
        }

        @Nullable
        public RsExpr getSecond() {
            return myTailExpr;
        }
    }

    /**
     * Returns a statement list and a tail expression (if exists) of the block after expansion
     * of macros and cfg attributes.
     */
    @NotNull
    public static ExpandedStmtsAndTailExpr getExpandedStmtsAndTailExpr(@NotNull RsBlock block) {
        return CachedValuesManager.getCachedValue(block, () ->
            CachedValueProvider.Result.create(
                doGetExpandedStmtsAndTailExpr(block),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        );
    }

    @NotNull
    private static ExpandedStmtsAndTailExpr doGetExpandedStmtsAndTailExpr(@NotNull RsBlock block) {
        List<RsStmt> stmts = new ArrayList<>();
        processExpandedStmtsInternal(block, stmt -> {
            if (stmt instanceof RsStmt) {
                if (!(stmt instanceof RsDocAndAttributeOwner)
                    || RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf((RsDocAndAttributeOwner) stmt)) {
                    stmts.add((RsStmt) stmt);
                }
            }
            return false;
        });

        RsExprStmt tailStmt = null;
        if (!stmts.isEmpty()) {
            RsStmt last = stmts.get(stmts.size() - 1);
            if (last instanceof RsExprStmt) {
                RsExprStmt exprStmt = (RsExprStmt) last;
                if (!RsStmtUtil.getHasSemicolon(exprStmt)) {
                    tailStmt = exprStmt;
                }
            }
        }

        if (tailStmt != null) {
            List<RsStmt> stmtsCopy = stmts.subList(0, stmts.size() - 1);
            return new ExpandedStmtsAndTailExpr(new ArrayList<>(stmtsCopy), tailStmt.getExpr());
        }
        return new ExpandedStmtsAndTailExpr(stmts, null);
    }

    /**
     * Returns the expanded tail expression of the block, or null.
     */
    @Nullable
    public static RsExpr getExpandedTailExpr(@NotNull RsBlock block) {
        return getExpandedStmtsAndTailExpr(block).getTailExpr();
    }

    /**
     * Returns the last RsExprStmt without a semicolon in a block before expansion
     * of macros and cfg attributes.
     */
    @Nullable
    public static RsExprStmt getSyntaxTailStmt(@NotNull RsBlock block) {
        return syntaxTailStmt(block);
    }

    @Nullable
    public static RsExprStmt syntaxTailStmt(@NotNull RsBlock block) {
        PsiElement rbrace = block.getRbrace();
        if (rbrace == null) return null;
        PsiElement lastStmtElem = PsiElementExt.getPrevNonCommentSibling(rbrace);
        if (!(lastStmtElem instanceof RsExprStmt)) return null;
        RsExprStmt lastStmt = (RsExprStmt) lastStmtElem;
        if (RsStmtUtil.getHasSemicolon(lastStmt)) return null;
        if (RsDocAndAttributeOwnerUtil.getQueryAttributes(lastStmt).hasCfgAttr()) return null;
        return lastStmt;
    }

    /**
     * For a block like {@code { 0 }} returns the single tail statement.
     */
    @Nullable
    public static RsExprStmt singleTailStmt(@NotNull RsBlock block) {
        RsExprStmt tailStmt = syntaxTailStmt(block);
        if (tailStmt == null) return null;
        PsiElement prev = PsiElementExt.getPrevNonCommentSibling(tailStmt);
        if (prev != null && prev.equals(block.getLbrace())) {
            return tailStmt;
        }
        return null;
    }

    /**
     * For a block like {@code { 0; }} returns {@code 0;}, for a block like {@code { 0 }} returns {@code 0}.
     */
    @Nullable
    public static RsStmt singleStmt(@NotNull RsBlock block) {
        PsiElement firstStmt = PsiElementExt.getNextNonCommentSibling(block.getLbrace());
        if (!(firstStmt instanceof RsStmt)) return null;
        PsiElement rbrace = block.getRbrace();
        if (rbrace != null && rbrace.equals(PsiElementExt.getNextNonCommentSibling(firstStmt))) {
            return (RsStmt) firstStmt;
        }
        return null;
    }

    /**
     * Returns a sequence of statements and macro calls from the block.
     */
    @NotNull
    public static List<RsElement> getStmtsAndMacros(@NotNull RsBlock block) {
        // Use children with leaves, filtering for RsElement instances
        List<RsElement> result = new ArrayList<>();
        PsiElement child = block.getFirstChild();
        while (child != null) {
            if (child instanceof RsElement) {
                result.add((RsElement) child);
            }
            child = child.getNextSibling();
        }
        return result;
    }

    private static boolean processExpandedStmtsInternal(@NotNull RsBlock block,
                                                         @NotNull Function<RsExpandedElement, Boolean> processor) {
        List<RsElement> stmtsAndMacros = getStmtsAndMacros(block);
        for (RsElement element : stmtsAndMacros) {
            if (processStmt(element, processor)) {
                return true;
            }
        }
        return false;
    }

    private static boolean processStmt(@NotNull RsElement element,
                                        @NotNull Function<RsExpandedElement, Boolean> processor) {
        if (element instanceof RsMacroCall) {
            return RsMacroCallUtil.processExpansionRecursively((RsMacroCall) element, processor);
        }
        if (element instanceof RsExpandedElement) {
            return processor.apply((RsExpandedElement) element);
        }
        return false;
    }
}
